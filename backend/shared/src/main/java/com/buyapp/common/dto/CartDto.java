package com.buyapp.common.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;

public class CartDto {
    private String id;
    private String userId;
    private List<CartItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double total;

    // Inner class for cart items - simpler than separate file
    public static class CartItemDto {
        @NotBlank(message = "Product ID cannot be empty")
        private String productId;
        private String productName;
        private Integer quantity;
        private Double price;
        private Boolean available;

        /**
         * Default constructor required for JSON deserialization (Jackson, Spring)
         */
        public CartItemDto() {
            // Empty constructor required for JSON deserialization frameworks
        }

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public Boolean getAvailable() {
            return available;
        }

        public void setAvailable(Boolean available) {
            this.available = available;
        }

        public Double getTotal() {
            return (price != null && quantity != null) ? price * quantity : 0.0;
        }
    }

    public CartDto() {
        this.items = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<CartItemDto> getItems() {
        return items;
    }

    public void setItems(List<CartItemDto> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Double getTotal() {
        if (total == null && items != null) {
            total = items.stream()
                    .mapToDouble(item -> item.getTotal() != null ? item.getTotal() : 0.0)
                    .sum();
        }
        return total != null ? total : 0.0;
    }

    public void setTotal(Double total) {
        this.total = total;
    }
}
