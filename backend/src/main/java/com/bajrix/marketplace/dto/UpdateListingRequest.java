package com.bajrix.marketplace.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Partial update payload for PUT /api/seller/listings/{id}.
 * `version` is required and is how the client proves it last read the
 * listing at a known state - see SellerListingService for the optimistic
 * concurrency check.
 */
public class UpdateListingRequest {

    @DecimalMin(value = "0.01", message = "price must be greater than 0")
    private java.math.BigDecimal price;

    @Min(value = 0, message = "stock cannot be negative")
    private Integer stock;

    @Min(value = 1, message = "minOrderQty must be at least 1")
    private Integer minOrderQty;

    private Boolean active;

    @NotNull(message = "version is required for concurrency-safe updates")
    private Long version;

    public java.math.BigDecimal getPrice() { return price; }
    public void setPrice(java.math.BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getMinOrderQty() { return minOrderQty; }
    public void setMinOrderQty(Integer minOrderQty) { this.minOrderQty = minOrderQty; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
