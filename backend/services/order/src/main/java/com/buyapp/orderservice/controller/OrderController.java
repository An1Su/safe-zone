package com.buyapp.orderservice.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buyapp.common.dto.OrderDto;
import com.buyapp.common.dto.ShippingAddressDto;
import com.buyapp.orderservice.model.OrderStatus;
import com.buyapp.orderservice.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ========== Buyer Endpoints ==========

    /**
     * Create order from cart
     * POST /orders
     */
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(
            @RequestHeader("X-User-Email") String userId,
            @Valid @RequestBody ShippingAddressDto shippingAddress) {
        OrderDto order = orderService.createOrder(userId, shippingAddress);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Get all orders for buyer
     * GET /orders
     */
    @GetMapping
    public ResponseEntity<List<OrderDto>> getOrders(
            @RequestHeader("X-User-Email") String userId) {
        List<OrderDto> orders = orderService.getOrders(userId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get order by ID (buyer)
     * GET /orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String userId) {
        OrderDto order = orderService.getOrderById(id, userId);
        return ResponseEntity.ok(order);
    }

    /**
     * Cancel order
     * PUT /orders/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderDto> cancelOrder(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String userId) {
        OrderDto order = orderService.cancelOrder(id, userId);
        return ResponseEntity.ok(order);
    }

    /**
     * Delete order
     * DELETE /orders/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String userId) {
        orderService.deleteOrder(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Redo order (create new order from cancelled order)
     * POST /orders/{id}/redo
     */
    @PostMapping("/{id}/redo")
    public ResponseEntity<OrderDto> redoOrder(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String userId) {
        OrderDto order = orderService.redoOrder(id, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Search orders (buyer)
     * GET /orders/search
     */
    @GetMapping("/search")
    public ResponseEntity<List<OrderDto>> searchOrders(
            @RequestHeader("X-User-Email") String userId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo) {
        List<OrderDto> orders = orderService.searchOrders(userId, q, status, dateFrom, dateTo);
        return ResponseEntity.ok(orders);
    }

    // ========== Seller Endpoints ==========

    /**
     * Get all orders for seller (orders containing seller's products)
     * GET /orders/seller
     */
    @GetMapping("/seller")
    public ResponseEntity<List<OrderDto>> getSellerOrders(
            @RequestHeader("X-User-Email") String sellerId) {
        List<OrderDto> orders = orderService.getSellerOrders(sellerId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get seller order by ID (only seller's items)
     * GET /orders/seller/{id}
     */
    @GetMapping("/seller/{id}")
    public ResponseEntity<OrderDto> getSellerOrderById(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String sellerId) {
        OrderDto order = orderService.getSellerOrderById(id, sellerId);
        return ResponseEntity.ok(order);
    }

    /**
     * Update order status (seller only)
     * PUT /orders/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @PathVariable String id,
            @RequestParam OrderStatus status,
            @RequestHeader("X-User-Email") String sellerId) {
        OrderDto order = orderService.updateOrderStatus(id, status, sellerId);
        return ResponseEntity.ok(order);
    }

    /**
     * Search orders (seller)
     * GET /orders/seller/search
     */
    @GetMapping("/seller/search")
    public ResponseEntity<List<OrderDto>> searchSellerOrders(
            @RequestHeader("X-User-Email") String sellerId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo) {
        List<OrderDto> orders = orderService.searchSellerOrders(sellerId, q, status, dateFrom, dateTo);
        return ResponseEntity.ok(orders);
    }
}
