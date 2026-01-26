package com.buyapp.common.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CartDto {
    private String id;
    private String userId;
    private List<CartItemDto> items = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double total;

    public void setItems(List<CartItemDto> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public Double getTotal() {
        if (total == null && items != null) {
            total = items.stream()
                    .mapToDouble(item -> item.getTotal() != null ? item.getTotal() : 0.0)
                    .sum();
        }
        return total != null ? total : 0.0;
    }

    // Inner class for cart items - simpler than separate file
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemDto {
        @NotBlank(message = "Product ID cannot be empty")
        private String productId;
        private String productName;
        private Integer quantity;
        private Double price;
        private Boolean available;

        public Double getTotal() {
            return (price != null && quantity != null) ? price * quantity : 0.0;
        }
    }
}
