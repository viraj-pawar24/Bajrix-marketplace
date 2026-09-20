package com.bajrix.marketplace.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreateListingRequest {

    @NotNull(message = "productId is required")
    private Long productId;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.01", message = "price must be greater than 0")
    private java.math.BigDecimal price;

    @NotNull(message = "stock is required")
    @Min(value = 0, message = "stock cannot be negative")
    private Integer stock;

    @NotNull(message = "minOrderQty is required")
    @Min(value = 1, message = "minOrderQty must be at least 1")
    private Integer minOrderQty;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public java.math.BigDecimal getPrice() { return price; }
    public void setPrice(java.math.BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getMinOrderQty() { return minOrderQty; }
    public void setMinOrderQty(Integer minOrderQty) { this.minOrderQty = minOrderQty; }
}
