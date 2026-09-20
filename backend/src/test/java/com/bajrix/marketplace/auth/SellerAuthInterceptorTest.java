package com.bajrix.marketplace.auth;

import com.bajrix.marketplace.entity.Seller;
import com.bajrix.marketplace.entity.SellerStatus;
import com.bajrix.marketplace.exception.UnauthenticatedException;
import com.bajrix.marketplace.repository.SellerRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerAuthInterceptorTest {

    @Mock private SellerRepository sellerRepository;
    @Mock private HttpServletRequest request;

    private SellerContext sellerContext;
    private SellerAuthInterceptor interceptor;

    // SellerContext is a request-scoped Spring bean in production; here it's
    // just a plain object, wired in by hand since there's no Spring context.
    @BeforeEach
    void wireUp() throws Exception {
        sellerContext = new SellerContext();
        interceptor = new SellerAuthInterceptor();
        setField(interceptor, "sellerRepository", sellerRepository);
        setField(interceptor, "sellerContext", sellerContext);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void missingHeader_isRejected() {
        when(request.getHeader(SellerAuthInterceptor.SELLER_ID_HEADER)).thenReturn(null);

        assertThatThrownBy(() -> interceptor.preHandle(request, null, null))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void nonNumericHeader_isRejected() {
        when(request.getHeader(SellerAuthInterceptor.SELLER_ID_HEADER)).thenReturn("not-a-number");

        assertThatThrownBy(() -> interceptor.preHandle(request, null, null))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void unknownSellerId_isRejected() {
        when(request.getHeader(SellerAuthInterceptor.SELLER_ID_HEADER)).thenReturn("999");
        when(sellerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interceptor.preHandle(request, null, null))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void validSellerId_populatesSellerContext() {
        Seller seller = new Seller();
        seller.setId(7L);
        seller.setStatus(SellerStatus.APPROVED);

        when(request.getHeader(SellerAuthInterceptor.SELLER_ID_HEADER)).thenReturn("7");
        when(sellerRepository.findById(7L)).thenReturn(Optional.of(seller));

        boolean result = interceptor.preHandle(request, null, null);

        assertThat(result).isTrue();
        assertThat(sellerContext.getSellerId()).isEqualTo(7L);
    }
}
