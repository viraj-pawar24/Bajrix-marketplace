package com.bajrix.marketplace.dto;

import java.util.List;

public class ProductDetailDto {
    private Long id;
    private String name;
    private String brand;
    private String category;
    private String unit;
    private String description;
    private List<ListingDto> listings;

    public ProductDetailDto(Long id, String name, String brand, String category, String unit,
                             String description, List<ListingDto> listings) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.unit = unit;
        this.description = description;
        this.listings = listings;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public String getCategory() { return category; }
    public String getUnit() { return unit; }
    public String getDescription() { return description; }
    public List<ListingDto> getListings() { return listings; }
}
