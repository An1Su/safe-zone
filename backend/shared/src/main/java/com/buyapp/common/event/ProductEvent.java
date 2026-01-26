package com.buyapp.common.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProductEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum EventType {
        PRODUCT_CREATED,
        PRODUCT_UPDATED,
        PRODUCT_DELETED
    }

    private EventType eventType;
    private String productId;
    private String productName;
    private String sellerId;
    private String sellerEmail;
    private LocalDateTime timestamp;

    public ProductEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public ProductEvent(EventType eventType, String productId, String sellerId, String sellerEmail) {
        this.eventType = eventType;
        this.productId = productId;
        this.sellerId = sellerId;
        this.sellerEmail = sellerEmail;
        this.timestamp = LocalDateTime.now();
    }

    public ProductEvent(EventType eventType, String productId, String productName, String sellerId, String sellerEmail) {
        this.eventType = eventType;
        this.productId = productId;
        this.productName = productName;
        this.sellerId = sellerId;
        this.sellerEmail = sellerEmail;
        this.timestamp = LocalDateTime.now();
    }
}
