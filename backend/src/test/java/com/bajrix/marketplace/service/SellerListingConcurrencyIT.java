package com.bajrix.marketplace.service;

import com.bajrix.marketplace.entity.Product;
import com.bajrix.marketplace.entity.Seller;
import com.bajrix.marketplace.entity.SellerListing;
import com.bajrix.marketplace.entity.SellerStatus;
import com.bajrix.marketplace.repository.ProductRepository;
import com.bajrix.marketplace.repository.SellerListingRepository;
import com.bajrix.marketplace.repository.SellerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves two guarantees at the real persistence layer (H2 in MySQL mode),
 * not just in mocked service tests:
 *  1. the DB-level unique constraint actually stops a duplicate listing;
 *  2. @Version optimistic locking actually rejects a write based on stale data,
 *     which is what protects against two concurrent edits to the same listing
 *     silently overwriting each other (see brief's "Concurrency" section).
 */
@DataJpaTest
@ActiveProfiles("test")
class SellerListingConcurrencyIT {

    @Autowired private TestEntityManager tem;
    @Autowired private EntityManager em;
    @Autowired private SellerListingRepository listingRepository;
    @Autowired private SellerRepository sellerRepository;
    @Autowired private ProductRepository productRepository;

    @Test
    void concurrentUpdates_secondWriterFailsOptimisticLock() {
        Seller seller = tem.persistAndFlush(seller("Shree Traders"));
        Product product = tem.persistAndFlush(product("PPC Cement 50kg"));
        SellerListing listing = tem.persistAndFlush(listing(product, seller, "390.00", 500, 10));
        tem.clear();

        // Simulate two clients loading the same listing at the same time.
        SellerListing readByClientA = listingRepository.findById(listing.getId()).orElseThrow();
        SellerListing readByClientB = listingRepository.findById(listing.getId()).orElseThrow();

        // Client A saves first - succeeds, version increments.
        readByClientA.setPrice(new BigDecimal("400.00"));
        listingRepository.saveAndFlush(readByClientA);

        // Client B still holds the old version and tries to save - must fail.
        readByClientB.setStock(50);
        assertThatThrownBy(() -> listingRepository.saveAndFlush(readByClientB))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void duplicateListingForSameSellerAndProduct_violatesUniqueConstraint() {
        Seller seller = tem.persistAndFlush(seller("Om Building Supplies"));
        Product product = tem.persistAndFlush(product("TMT Bar 12mm"));
        tem.persistAndFlush(listing(product, seller, "68000.00", 40, 1));

        SellerListing duplicate = listing(product, seller, "67000.00", 10, 1);

        assertThatThrownBy(() -> tem.persistAndFlush(duplicate))
                .isInstanceOf(PersistenceException.class);
    }

    private Seller seller(String name) {
        Seller s = new Seller();
        s.setName(name);
        s.setContactEmail(name.toLowerCase().replace(" ", "") + "@example.com");
        s.setStatus(SellerStatus.APPROVED);
        return s;
    }

    private Product product(String name) {
        Product p = new Product();
        p.setName(name);
        p.setCategory("Test Category");
        return p;
    }

    private SellerListing listing(Product product, Seller seller, String price, int stock, int moq) {
        SellerListing l = new SellerListing();
        l.setProduct(product);
        l.setSeller(seller);
        l.setPrice(new BigDecimal(price));
        l.setStock(stock);
        l.setMinOrderQty(moq);
        l.setActive(true);
        return l;
    }
}
