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

    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING || status == OrderStatus.READY_FOR_DELIVERY;
    }

    public boolean canBeDeleted() {
        return status == OrderStatus.CANCELLED || status == OrderStatus.DELIVERED;
    }
}
