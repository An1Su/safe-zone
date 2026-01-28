package com.buyapp.common.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderDto {
    private String id;
    private String userId;
    private List<OrderItemDto> items = new ArrayList<>();
    private String status;
    private Double totalAmount;
    private ShippingAddressDto shippingAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void setItems(List<OrderItemDto> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public Double getTotalAmount() {
        if (totalAmount == null && items != null) {
            totalAmount = items.stream()
                    .mapToDouble(item -> item.getTotal() != null ? item.getTotal() : 0.0)
                    .sum();
        }
        return totalAmount != null ? totalAmount : 0.0;
    }

    // Inner class for order items
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDto {
        private String productId;
        private String productName;
        private String sellerId;
        private Integer quantity;
        private Double price;

        public Double getTotal() {
            return (price != null && quantity != null) ? price * quantity : 0.0;
        }
    }
}
