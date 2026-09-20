package com.bajrix.marketplace.dto;

import java.math.BigDecimal;

/** What a buyer sees in the catalogue/search results grid. */
public class ProductSummaryDto {
    private Long id;
    private String name;
    private String brand;
    private String category;
    private String unit;
    private BigDecimal fromPrice; // null if no seller currently offers it
    private long sellerCount;

    public ProductSummaryDto(Long id, String name, String brand, String category, String unit,
                              BigDecimal fromPrice, long sellerCount) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.unit = unit;
        this.fromPrice = fromPrice;
        this.sellerCount = sellerCount;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public String getCategory() { return category; }
    public String getUnit() { return unit; }
    public BigDecimal getFromPrice() { return fromPrice; }
    public long getSellerCount() { return sellerCount; }
}
