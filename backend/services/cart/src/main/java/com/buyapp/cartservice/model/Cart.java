package com.buyapp.cartservice.model;

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
}
