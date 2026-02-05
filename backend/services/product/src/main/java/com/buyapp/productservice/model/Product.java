package com.buyapp.productservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    private String id;

    @NotBlank(message = "Product name can't be empty")
    @Size(min = 1, max = 50)
    private String name;

    @NotBlank(message = "Product description can't be empty")
    @Size(min = 2, max = 150)
    private String description;

    @Positive(message = "Product price can't be negative")
    private Double price;

    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    @Field("userId")
    private String userId;

    private String category; // Face, Eyes, Lips
}
