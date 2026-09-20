package com.bajrix.marketplace.dto;

import com.bajrix.marketplace.entity.SellerListing;

import java.math.BigDecimal;
import java.time.Instant;

public class ListingDto {
	private Long productId;
	private String productName;
    private Long id;
    private Long sellerId;
    private String sellerName;
    private BigDecimal price;
    private Integer stock;
    private Integer minOrderQty;
    private boolean active;
    /** true when active + stock >= minOrderQty, i.e. a buyer could actually place a minimum order. */
    private boolean orderable;
    private Long version;
    private Instant updatedAt;

    public static ListingDto from(SellerListing l) {
        ListingDto dto = new ListingDto();
        dto.id = l.getId();
        dto.productId = l.getProduct().getId();
        dto.productName = l.getProduct().getName();
        dto.sellerId = l.getSeller().getId();
        dto.sellerName = l.getSeller().getName();
        dto.price = l.getPrice();
        dto.stock = l.getStock();
        dto.minOrderQty = l.getMinOrderQty();
        dto.active = l.isActive();
        dto.orderable = l.isActive() && l.getStock() >= l.getMinOrderQty();
        dto.version = l.getVersion();
        dto.updatedAt = l.getUpdatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public BigDecimal getPrice() { return price; }
    public Integer getStock() { return stock; }
    public Integer getMinOrderQty() { return minOrderQty; }
    public boolean isActive() { return active; }
    public boolean isOrderable() { return orderable; }
    public Long getVersion() { return version; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
}
