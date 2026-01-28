package com.buyapp.orderservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.buyapp.common.dto.ShippingAddressDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    private String id;

    @NotBlank(message = "User ID cannot be empty")
    @Field("userId")
    private String userId;

    @Valid
    private List<OrderItem> items;

    @Field("status")
    private OrderStatus status;

    @Field("totalAmount")
    private Double totalAmount;

    @Valid
    @Field("shippingAddress")
    private ShippingAddressDto shippingAddress;

    @CreatedDate
    @Field("createdAt")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    private LocalDateTime updatedAt;

    public Order(String userId, List<OrderItem> items, ShippingAddressDto shippingAddress) {
        this.userId = userId;
        this.items = items != null ? items : new ArrayList<>();
        this.shippingAddress = shippingAddress;
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.totalAmount = calculateTotal();
    }

    public void setItems(List<OrderItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        this.totalAmount = calculateTotal();
    }

    private Double calculateTotal() {
        if (items == null || items.isEmpty()) {
            return 0.0;
        }
        return items.stream()
                .mapToDouble(OrderItem::getTotal)
                .sum();
    }

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING || status == OrderStatus.READY_FOR_DELIVERY;
    }

    public boolean canBeDeleted() {
        return status == OrderStatus.CANCELLED || status == OrderStatus.DELIVERED;
    }

    public boolean belongsToUser(String userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    public boolean containsSellerItems(String sellerId) {
        if (items == null || items.isEmpty()) {
            return false;
        }
        return items.stream()
                .anyMatch(item -> item.getSellerId() != null && item.getSellerId().equals(sellerId));
    }

    /**
     * Cancel the order (updates status and timestamp)
     * @throws IllegalStateException if order cannot be cancelled
     */
    public void cancel() {
        if (!canBeCancelled()) {
            throw new IllegalStateException(
                    "Order cannot be cancelled. Current status: " + status);
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update order status with validation
     * @param newStatus the new status
     * @throws IllegalStateException if status transition is invalid
     */
    public void updateStatus(OrderStatus newStatus) {
        if (!OrderStatus.isValidTransition(this.status, newStatus)) {
            throw new IllegalStateException(
                    "Invalid status transition from " + status + " to " + newStatus);
        }
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if order matches search query (by ID or product name)
     */
    public boolean matchesQuery(String lowerQuery) {
        // Match by order ID
        if (matchesId(lowerQuery)) {
            return true;
        }

        // Match by product name (with null safety)
        if (items != null) {
            return items.stream()
                    .anyMatch(item -> item.getProductName() != null
                            && item.getProductName().toLowerCase().contains(lowerQuery));
        }

        return false;
    }

    /**
     * Check if order matches search query for seller (by ID or seller's product names)
     */
    public boolean matchesSellerQuery(String sellerId, String lowerQuery) {
        // Match by order ID
        if (matchesId(lowerQuery)) {
            return true;
        }

        // Match by seller's product names (with null safety)
        if (items != null) {
            return items.stream()
                    .filter(item -> item.getSellerId() != null && item.getSellerId().equals(sellerId))
                    .anyMatch(item -> item.getProductName() != null
                            && item.getProductName().toLowerCase().contains(lowerQuery));
        }

        return false;
    }

    /**
     * Check if order ID matches the query
     */
    private boolean matchesId(String lowerQuery) {
        return id != null && id.toLowerCase().contains(lowerQuery);
    }
}
