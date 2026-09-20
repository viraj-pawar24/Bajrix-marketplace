package com.bajrix.marketplace.service;

import com.bajrix.marketplace.dto.CreateListingRequest;
import com.bajrix.marketplace.dto.ListingDto;
import com.bajrix.marketplace.dto.UpdateListingRequest;
import com.bajrix.marketplace.entity.Product;
import com.bajrix.marketplace.entity.Seller;
import com.bajrix.marketplace.entity.SellerListing;
import com.bajrix.marketplace.entity.SellerStatus;
import com.bajrix.marketplace.exception.BusinessValidationException;
import com.bajrix.marketplace.exception.DuplicateListingException;
import com.bajrix.marketplace.exception.ForbiddenException;
import com.bajrix.marketplace.repository.ProductRepository;
import com.bajrix.marketplace.repository.SellerListingRepository;
import com.bajrix.marketplace.repository.SellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the parts of the brief that are easy to silently get wrong:
 * duplicate listings, cross-seller writes, and stale (concurrent) updates.
 * These are pure Mockito tests (no Spring context, no DB) so they run fast
 * and pin down the business logic independently of persistence wiring.
 */
@ExtendWith(MockitoExtension.class)
class SellerListingServiceTest {

    @Mock private SellerListingRepository listingRepository;
    @Mock private ProductRepository productRepository;
    @Mock private SellerRepository sellerRepository;

    @InjectMocks
    private SellerListingService service;

    private Seller seller;
    private Seller otherSeller;
    private Product product;

    @BeforeEach
    void setUp() {
        seller = new Seller();
        seller.setId(1L);
        seller.setName("Shree Traders");
        seller.setStatus(SellerStatus.APPROVED);

        otherSeller = new Seller();
        otherSeller.setId(2L);
        otherSeller.setName("Om Building Supplies");
        otherSeller.setStatus(SellerStatus.APPROVED);

        product = new Product();
        product.setId(100L);
        product.setName("PPC Cement 50kg");
        product.setCategory("Cement");
    }

    @Test
    void create_rejectsDuplicateListingForSameSellerAndProduct() {
        when(sellerRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(listingRepository.existsByProductIdAndSellerId(100L, 1L)).thenReturn(true);

        CreateListingRequest req = validCreateRequest();

        assertThatThrownBy(() -> service.create(1L, req))
                .isInstanceOf(DuplicateListingException.class);

        verify(listingRepository, never()).save(any());
    }

    @Test
    void create_rejectsNonPositivePrice() {
        when(sellerRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(listingRepository.existsByProductIdAndSellerId(100L, 1L)).thenReturn(false);

        CreateListingRequest req = validCreateRequest();
        req.setPrice(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.create(1L, req))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void create_savesListingWhenValid() {
        when(sellerRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(listingRepository.existsByProductIdAndSellerId(100L, 1L)).thenReturn(false);
        when(listingRepository.save(any(SellerListing.class))).thenAnswer(inv -> {
            SellerListing l = inv.getArgument(0);
            l.setId(555L);
            l.setVersion(0L);
            return l;
        });

        ListingDto dto = service.create(1L, validCreateRequest());

        assertThat(dto.getId()).isEqualTo(555L);
        assertThat(dto.getSellerId()).isEqualTo(1L);
        assertThat(dto.isActive()).isTrue();
    }

    @Test
    void update_rejectsWhenSellerDoesNotOwnListing() {
        SellerListing existing = existingListing(seller, BigDecimal.valueOf(390), 100, 10, 0L);
        when(listingRepository.findById(999L)).thenReturn(Optional.of(existing));

        UpdateListingRequest req = new UpdateListingRequest();
        req.setPrice(BigDecimal.valueOf(400));
        req.setVersion(0L);

        // otherSeller (id=2) tries to update seller 1's listing
        assertThatThrownBy(() -> service.update(2L, 999L, req))
                .isInstanceOf(ForbiddenException.class);

        verify(listingRepository, never()).save(any());
    }

    @Test
    void update_rejectsStaleVersion_concurrentModification() {
        SellerListing existing = existingListing(seller, BigDecimal.valueOf(390), 100, 10, 3L); // current version is 3
        when(listingRepository.findById(999L)).thenReturn(Optional.of(existing));

        UpdateListingRequest req = new UpdateListingRequest();
        req.setPrice(BigDecimal.valueOf(400));
        req.setVersion(1L); // caller is working off a stale copy (version 1)

        assertThatThrownBy(() -> service.update(1L, 999L, req))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        verify(listingRepository, never()).save(any());
    }

    @Test
    void update_appliesPartialChangesAndKeepsUntouchedFields() {
        SellerListing existing = existingListing(seller, BigDecimal.valueOf(390), 100, 10, 0L);
        when(listingRepository.findById(999L)).thenReturn(Optional.of(existing));
        when(listingRepository.save(any(SellerListing.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateListingRequest req = new UpdateListingRequest();
        req.setStock(50); // only stock changes
        req.setVersion(0L);

        ListingDto dto = service.update(1L, 999L, req);

        assertThat(dto.getStock()).isEqualTo(50);
        assertThat(dto.getPrice()).isEqualByComparingTo("390");
        assertThat(dto.getMinOrderQty()).isEqualTo(10);
    }

    @Test
    void stopSelling_deactivatesRatherThanDeletes() {
        SellerListing existing = existingListing(seller, BigDecimal.valueOf(390), 100, 10, 0L);
        when(listingRepository.findById(999L)).thenReturn(Optional.of(existing));
        when(listingRepository.save(any(SellerListing.class))).thenAnswer(inv -> inv.getArgument(0));

        ListingDto dto = service.stopSelling(1L, 999L, 0L);

        assertThat(dto.isActive()).isFalse();
    }

    private CreateListingRequest validCreateRequest() {
        CreateListingRequest req = new CreateListingRequest();
        req.setProductId(100L);
        req.setPrice(BigDecimal.valueOf(390));
        req.setStock(500);
        req.setMinOrderQty(10);
        return req;
    }

    private SellerListing existingListing(Seller owner, BigDecimal price, int stock, int moq, Long version) {
        SellerListing l = new SellerListing();
        l.setId(999L);
        l.setProduct(product);
        l.setSeller(owner);
        l.setPrice(price);
        l.setStock(stock);
        l.setMinOrderQty(moq);
        l.setActive(true);
        l.setVersion(version);
        return l;
    }
}
