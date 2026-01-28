package com.buyapp.orderservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "carts")
@Getter
@Setter
@NoArgsConstructor
public class Cart {

    @Id
    private String id;

    @NotBlank(message = "User ID cannot be empty")
    @Field("userId")
    private String userId;

    @Valid
    private List<CartItem> items;

    @CreatedDate
    @Field("createdAt")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    private LocalDateTime updatedAt;

    public Cart(String userId) {
        this.userId = userId;
        this.items = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void setItems(List<CartItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    // Helper methods
    public void addItem(CartItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
        updatedAt = LocalDateTime.now();
    }

    public void removeItem(String productId) {
        if (items != null) {
            items.removeIf(item -> item.getProductId().equals(productId));
            updatedAt = LocalDateTime.now();
        }
    }

    public void updateItem(String productId, Integer quantity, Double price) {
        CartItem item = findItemByProductId(productId);
        if (item != null) {
            item.setQuantity(quantity);
            item.setPrice(price);
            updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Add quantity to existing item or add new item if not found
     */
    public void addOrUpdateItem(CartItem newItem) {
        CartItem existingItem = findItemByProductId(newItem.getProductId());
        if (existingItem != null) {
            // Add quantities together
            existingItem.setQuantity(existingItem.getQuantity() + newItem.getQuantity());
            // Update price in case it changed
            existingItem.setPrice(newItem.getPrice());
        } else {
            // Add new item
            addItem(newItem);
        }
        updatedAt = LocalDateTime.now();
    }

    public void clear() {
        if (items != null) {
            items.clear();
        } else {
            items = new ArrayList<>();
        }
        updatedAt = LocalDateTime.now();
    }

    public CartItem findItemByProductId(String productId) {
        if (items == null) {
            return null;
        }
        return items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    public Double getTotal() {
        if (items == null || items.isEmpty()) {
            return 0.0;
        }
        return items.stream()
                .mapToDouble(CartItem::getTotal)
                .sum();
    }

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    /**
     * Convert cart items to order items with seller IDs
     * @param sellerIdCache Map of productId -> sellerId
     * @return List of OrderItems
     */
    public List<com.buyapp.orderservice.model.OrderItem> toOrderItems(java.util.Map<String, String> sellerIdCache) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }
        return items.stream()
                .map(cartItem -> new com.buyapp.orderservice.model.OrderItem(
                        cartItem.getProductId(),
                        cartItem.getProductName(),
                        sellerIdCache.get(cartItem.getProductId()),
                        cartItem.getQuantity(),
                        cartItem.getPrice()))
                .toList();
    }
}
