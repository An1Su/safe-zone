package com.buyapp.orderservice.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    @NotBlank(message = "Product ID cannot be empty")
    private String productId;
    
    @NotBlank(message = "Product name cannot be empty")
    private String productName;
    
    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    @NotNull(message = "Price cannot be null")
    @Positive(message = "Price must be positive")
    private Double price;
    
    // Helper method to calculate item total
    public Double getTotal() {
        return price * quantity;
    }
}
