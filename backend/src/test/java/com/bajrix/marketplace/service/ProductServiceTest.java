package com.bajrix.marketplace.service;

import com.bajrix.marketplace.dto.ProductSummaryDto;
import com.bajrix.marketplace.entity.Product;
import com.bajrix.marketplace.exception.ResourceNotFoundException;
import com.bajrix.marketplace.repository.ProductRepository;
import com.bajrix.marketplace.repository.SellerListingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private SellerListingRepository sellerListingRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void search_attachesFromPriceAndSellerCountPerProduct() {
        Product cement = new Product();
        cement.setId(100L);
        cement.setName("PPC Cement 50kg");
        cement.setCategory("Cement");

        Page<Product> page = new PageImpl<>(List.of(cement), PageRequest.of(0, 20), 1);
        when(productRepository.search(eq("cement"), any(), any())).thenReturn(page);
        when(sellerListingRepository.findMinVisiblePrice(100L)).thenReturn(new BigDecimal("385.00"));
        when(sellerListingRepository.countVisibleSellersForProduct(100L)).thenReturn(3L);

        Page<ProductSummaryDto> result = productService.search("cement", null, PageRequest.of(0, 20));

        ProductSummaryDto dto = result.getContent().get(0);
        assertThat(dto.getFromPrice()).isEqualByComparingTo("385.00");
        assertThat(dto.getSellerCount()).isEqualTo(3L);
    }

    @Test
    void search_blankKeywordIsTreatedAsNoFilter() {
        Page<Product> page = new PageImpl<>(List.of());
        when(productRepository.search(eq(null), eq(null), any())).thenReturn(page);

        productService.search("   ", "", PageRequest.of(0, 20));
        // no exception, and the repository received nulls rather than blank strings - verified via the stub above
    }

    @Test
    void getDetail_throwsWhenProductMissing() {
        when(productRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getDetail(42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
