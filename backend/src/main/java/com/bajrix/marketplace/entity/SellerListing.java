package com.bajrix.marketplace.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single seller's offer to sell a given Product: price, stock and minimum
 * order quantity. A Product can have many SellerListings (one per seller);
 * a Seller can have many SellerListings (one per product they carry). The
 * (product_id, seller_id) pair is unique - a seller cannot list the same
 * product twice, they must update their existing listing instead.
 */
@Entity
@Table(name = "seller_listings",
        uniqueConstraints = @UniqueConstraint(name = "uq_listing_product_seller", columnNames = {"product_id", "seller_id"}),
        indexes = {
                @Index(name = "idx_listing_product", columnList = "product_id"),
                @Index(name = "idx_listing_seller", columnList = "seller_id")
        })
public class SellerListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "min_order_qty", nullable = false)
    private Integer minOrderQty;

    /** A seller "stops selling" a product by flipping this to false rather than deleting the row. */
    @Column(nullable = false)
    private boolean active = true;

    /** Optimistic-locking token: guards against two concurrent updates silently clobbering each other. */
    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Seller getSeller() { return seller; }
    public void setSeller(Seller seller) { this.seller = seller; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Integer getMinOrderQty() { return minOrderQty; }
    public void setMinOrderQty(Integer minOrderQty) { this.minOrderQty = minOrderQty; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
