package com.bajrix.marketplace.repository;

import com.bajrix.marketplace.entity.SellerListing;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SellerListingRepository extends JpaRepository<SellerListing, Long> {

    /** All listings a buyer is allowed to see for a product: active listings from approved sellers, cheapest first. */
    @EntityGraph(attributePaths = {"seller"})
    @Query("""
            SELECT l FROM SellerListing l
            WHERE l.product.id = :productId
              AND l.active = true
              AND l.seller.status = com.bajrix.marketplace.entity.SellerStatus.APPROVED
            ORDER BY l.price ASC
            """)
    List<SellerListing> findBuyerVisibleListingsForProduct(@Param("productId") Long productId);

    /** Cheapest visible (active + approved-seller) price for a product, used for catalogue "from ₹x" display. */
    @Query("""
            SELECT MIN(l.price) FROM SellerListing l
            WHERE l.product.id = :productId
              AND l.active = true
              AND l.seller.status = com.bajrix.marketplace.entity.SellerStatus.APPROVED
            """)
    java.math.BigDecimal findMinVisiblePrice(@Param("productId") Long productId);

    @Query("""
            SELECT COUNT(l) FROM SellerListing l
            WHERE l.product.id = :productId
              AND l.active = true
              AND l.seller.status = com.bajrix.marketplace.entity.SellerStatus.APPROVED
            """)
    long countVisibleSellersForProduct(@Param("productId") Long productId);

    @EntityGraph(attributePaths = {"seller", "product"})
    List<SellerListing> findBySellerIdOrderByUpdatedAtDesc(Long sellerId);

    Optional<SellerListing> findByProductIdAndSellerId(Long productId, Long sellerId);

    boolean existsByProductIdAndSellerId(Long productId, Long sellerId);
}
