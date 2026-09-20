package com.bajrix.marketplace.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateProductRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String brand;

    @NotBlank(message = "category is required")
    private String category;

    private String unit;

    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
