package com.buyapp.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ProductDto {
    private String id;

    @NotBlank(message = "Product name cannot be empty")
    @Size(min = 1, max = 50, message = "Product name must be between 1 and 50 characters")
    private String name;

    @NotBlank(message = "Product description cannot be empty")
    @Size(min = 2, max = 150, message = "Product description must be between 2 and 150 characters")
    private String description;

    @Positive(message = "Product price must be positive")
    private Double price;

    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    private String user; // Email of the owner

    // Constructors
    public ProductDto() {
    }

    public ProductDto(String id, String name, String description, Double price, Integer stock, String user) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.user = user;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
