package com.buyapp.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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

    private String category; // Face, Eyes, Lips
}
