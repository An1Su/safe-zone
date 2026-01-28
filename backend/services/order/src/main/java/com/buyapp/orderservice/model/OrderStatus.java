package com.buyapp.orderservice.model;

public enum OrderStatus {
    PENDING,
    READY_FOR_DELIVERY,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    /**
     * Check if transition from current status to next status is valid
     * @param current the current status
     * @param next the next status
     * @return true if transition is valid, false otherwise
     */
    public static boolean isValidTransition(OrderStatus current, OrderStatus next) {
        return switch (current) {
            case PENDING -> next == READY_FOR_DELIVERY || next == CANCELLED;
            case READY_FOR_DELIVERY -> next == SHIPPED || next == CANCELLED;
            case SHIPPED -> next == DELIVERED;
            case DELIVERED, CANCELLED -> false; // Terminal states
        };
    }
}
