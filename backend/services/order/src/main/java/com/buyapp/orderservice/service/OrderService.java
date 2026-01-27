package com.buyapp.orderservice.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.buyapp.common.dto.CartDto;
import com.buyapp.common.dto.OrderDto;
import com.buyapp.common.dto.OrderDto.OrderItemDto;
import com.buyapp.common.dto.ProductDto;
import com.buyapp.common.dto.ShippingAddressDto;
import com.buyapp.common.exception.ResourceNotFoundException;
import com.buyapp.orderservice.model.Order;
import com.buyapp.orderservice.model.OrderItem;
import com.buyapp.orderservice.model.OrderStatus;
import com.buyapp.orderservice.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    /**
     * Service URLs for Eureka service discovery.
     * These match the spring.application.name from each service's application.yml.
     * @LoadBalanced WebClient resolves these through Eureka to find the actual service instances.
     */
    private static final String CART_SERVICE_URL = "http://cart-service";
    private static final String PRODUCT_SERVICE_URL = "http://product-service";
    private static final String USER_SERVICE_URL = "http://user-service";
    private static final String ORDER_NOT_FOUND_MESSAGE = "Order not found with id: ";

    public OrderService(OrderRepository orderRepository, WebClient.Builder webClientBuilder) {
        this.orderRepository = orderRepository;
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * Create order from cart
     * Reduces stock for each product and clears the cart
     */
    public OrderDto createOrder(String userId, ShippingAddressDto shippingAddressDto) {
        // Get cart from Cart Service
        CartDto cart = getCart(userId);
        
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot create order from empty cart");
        }

        // Cache products and seller IDs to avoid redundant calls
        Map<String, ProductDto> productCache = new HashMap<>();
        Map<String, String> sellerIdCache = new HashMap<>();

        // Validate all items are available and cache products
        for (CartDto.CartItemDto item : cart.getItems()) {
            ProductDto product = getProductById(item.getProductId());
            if (product == null) {
                throw new ResourceNotFoundException("Product not found: " + item.getProductId());
            }
            productCache.put(item.getProductId(), product);
            
            if (product.getStock() == null || product.getStock() < item.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: " + product.getName() +
                                ". Available: " + (product.getStock() != null ? product.getStock() : 0) +
                                ", Requested: " + item.getQuantity());
            }
            
            // Cache seller ID
            String sellerId = getProductSellerId(item.getProductId());
            if (sellerId == null) {
                throw new ResourceNotFoundException("Seller not found for product: " + product.getName());
            }
            sellerIdCache.put(item.getProductId(), sellerId);
        }

        // Convert cart items to order items (using cached data)
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> new OrderItem(
                        cartItem.getProductId(),
                        cartItem.getProductName(),
                        sellerIdCache.get(cartItem.getProductId()),
                        cartItem.getQuantity(),
                        cartItem.getPrice()))
                .collect(Collectors.toList());

        // Create order
        Order order = new Order(userId, orderItems, shippingAddressDto);
        Order savedOrder = orderRepository.save(order);

        // Reduce stock for each product
        for (OrderItem item : orderItems) {
            reduceStock(item.getProductId(), item.getQuantity());
        }

        // Clear cart
        clearCart(userId);

        return toDto(savedOrder);
    }

    /**
     * Get all orders for a buyer
     */
    public List<OrderDto> getOrders(String userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get order by ID (buyer)
     */
    public OrderDto getOrderById(String orderId, String userId) {
        Order order = findOrderById(orderId);
        verifyOrderBelongsToUser(order, userId);
        return toDto(order);
    }

    /**
     * Cancel order (only if PENDING or READY_FOR_DELIVERY)
     * Restores stock for each product
     */
    public OrderDto cancelOrder(String orderId, String userId) {
        Order order = findOrderById(orderId);
        verifyOrderBelongsToUser(order, userId);

        // Check if order can be cancelled
        if (!order.canBeCancelled()) {
            throw new IllegalStateException(
                    "Order cannot be cancelled. Current status: " + order.getStatus());
        }

        // Restore stock for each product
        for (OrderItem item : order.getItems()) {
            restoreStock(item.getProductId(), item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        return toDto(savedOrder);
    }

    /**
     * Delete order (only if CANCELLED or DELIVERED)
     */
    public void deleteOrder(String orderId, String userId) {
        Order order = findOrderById(orderId);
        verifyOrderBelongsToUser(order, userId);

        // Check if order can be deleted
        if (!order.canBeDeleted()) {
            throw new IllegalStateException(
                    "Order cannot be deleted. Current status: " + order.getStatus());
        }

        orderRepository.delete(order);
    }

    /**
     * Redo order (create new order from cancelled order)
     */
    public OrderDto redoOrder(String orderId, String userId) {
        Order originalOrder = findOrderById(orderId);
        verifyOrderBelongsToUser(originalOrder, userId);

        // Create new order with same items and shipping address
        Order newOrder = new Order(
                userId,
                originalOrder.getItems(),
                originalOrder.getShippingAddress());

        // Validate stock availability
        for (OrderItem item : newOrder.getItems()) {
            ProductDto product = getProductById(item.getProductId());
            if (product == null) {
                throw new ResourceNotFoundException("Product not found: " + item.getProductId());
            }
            if (product.getStock() == null || product.getStock() < item.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: " + product.getName() +
                                ". Available: " + (product.getStock() != null ? product.getStock() : 0) +
                                ", Requested: " + item.getQuantity());
            }
        }

        Order savedOrder = orderRepository.save(newOrder);

        // Reduce stock
        for (OrderItem item : savedOrder.getItems()) {
            reduceStock(item.getProductId(), item.getQuantity());
        }

        return toDto(savedOrder);
    }

    /**
     * Search orders for buyer
     */
    public List<OrderDto> searchOrders(String userId, String query, OrderStatus status,
            LocalDateTime dateFrom, LocalDateTime dateTo) {
        List<Order> orders;

        if (dateFrom != null && dateTo != null) {
            orders = orderRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                    userId, dateFrom, dateTo);
        } else {
            orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }

        // Filter by status if provided
        if (status != null) {
            orders = orders.stream()
                    .filter(order -> order.getStatus() == status)
                    .collect(Collectors.toList());
        }

        // Filter by query (order ID or product name)
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase();
            orders = orders.stream()
                    .filter(order -> {
                        // Match order ID
                        if (order.getId() != null && order.getId().toLowerCase().contains(lowerQuery)) {
                            return true;
                        }
                        // Match product names
                        return order.getItems().stream()
                                .anyMatch(item -> item.getProductName().toLowerCase().contains(lowerQuery));
                    })
                    .collect(Collectors.toList());
        }

        return orders.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all orders for a seller (orders containing seller's products)
     */
    public List<OrderDto> getSellerOrders(String sellerEmail) {
        String sellerId = getSellerIdByEmail(sellerEmail);
        List<Order> orders = orderRepository.findByItemsSellerIdOrderByCreatedAtDesc(sellerId);
        return orders.stream()
                .map(order -> toSellerOrderDto(order, sellerId))
                .collect(Collectors.toList());
    }

    /**
     * Get seller order by ID (only seller's items)
     */
    public OrderDto getSellerOrderById(String orderId, String sellerEmail) {
        String sellerId = getSellerIdByEmail(sellerEmail);
        Order order = findOrderById(orderId);
        verifyOrderContainsSellerItems(order, sellerId);
        return toSellerOrderDto(order, sellerId);
    }

    /**
     * Update order status (seller only)
     */
    public OrderDto updateOrderStatus(String orderId, OrderStatus newStatus, String sellerEmail) {
        String sellerId = getSellerIdByEmail(sellerEmail);
        Order order = findOrderById(orderId);
        verifyOrderContainsSellerItems(order, sellerId);

        // Validate status transition
        OrderStatus currentStatus = order.getStatus();
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new IllegalStateException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        return toSellerOrderDto(savedOrder, sellerId);
    }

    /**
     * Search orders for seller
     */
    public List<OrderDto> searchSellerOrders(String sellerEmail, String query, OrderStatus status,
            LocalDateTime dateFrom, LocalDateTime dateTo) {
        String sellerId = getSellerIdByEmail(sellerEmail);
        List<Order> orders;

        if (dateFrom != null && dateTo != null) {
            // Note: MongoDB doesn't support filtering by date range AND sellerId directly
            // We'll filter in memory
            orders = orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(dateFrom, dateTo);
        } else {
            orders = orderRepository.findByItemsSellerIdOrderByCreatedAtDesc(sellerId);
        }

        // Filter by seller's items, status, and query in one pass
        String lowerQuery = (query != null && !query.trim().isEmpty()) ? query.toLowerCase() : null;
        final String finalSellerId = sellerId;
        final OrderStatus finalStatus = status;

        orders = orders.stream()
                .filter(order -> {
                    // Filter by seller's items
                    boolean hasSellerItems = order.getItems().stream()
                            .anyMatch(item -> item.getSellerId().equals(finalSellerId));
                    if (!hasSellerItems) {
                        return false;
                    }

                    // Filter by status if provided
                    if (finalStatus != null && order.getStatus() != finalStatus) {
                        return false;
                    }

                    // Filter by query if provided
                    if (lowerQuery != null) {
                        // Match order ID
                        if (order.getId() != null && order.getId().toLowerCase().contains(lowerQuery)) {
                            return true;
                        }
                        // Match product names (seller's products only)
                        return order.getItems().stream()
                                .filter(item -> item.getSellerId().equals(finalSellerId))
                                .anyMatch(item -> item.getProductName().toLowerCase().contains(lowerQuery));
                    }

                    return true;
                })
                .collect(Collectors.toList());

        return orders.stream()
                .map(order -> toSellerOrderDto(order, sellerId))
                .collect(Collectors.toList());
    }

    // Helper methods for inter-service communication

    private CartDto getCart(String userId) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("http://" + CART_SERVICE + "/api/cart")
                    .header("X-User-Email", userId)
                    .retrieve()
                    .bodyToMono(CartDto.class)
                    .block();
        } catch (Exception e) {
            throw new ResourceNotFoundException("Cart not found for user: " + userId);
        }
    }

    private void clearCart(String userId) {
        try {
            webClientBuilder.build()
                    .delete()
                    .uri("http://" + CART_SERVICE + "/api/cart")
                    .header("X-User-Email", userId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception e) {
            // Log but don't fail order creation if cart clearing fails
            System.err.println("Failed to clear cart for user: " + userId);
        }
    }

    private ProductDto getProductById(String productId) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(PRODUCT_SERVICE_URL + "/api/products/{id}", productId)
                    .retrieve()
                    .bodyToMono(ProductDto.class)
                    .block();
        } catch (Exception e) {
            return null;
        }
    }

    private void reduceStock(String productId, Integer quantity) {
        try {
            webClientBuilder.build()
                    .post()
                    .uri(PRODUCT_SERVICE_URL + "/api/products/{id}/reduce-stock?quantity={quantity}",
                            productId, quantity)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Failed to reduce stock for product: " + productId, e);
        }
    }

    private void restoreStock(String productId, Integer quantity) {
        try {
            webClientBuilder.build()
                    .post()
                    .uri(PRODUCT_SERVICE_URL + "/api/products/{id}/restore-stock?quantity={quantity}",
                            productId, quantity)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Failed to restore stock for product: " + productId, e);
        }
    }

    private String getProductSellerId(String productId) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(PRODUCT_SERVICE_URL + "/api/products/{id}/seller-id", productId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            return null;
        }
    }

    // Helper methods for validation and common operations

    private Order findOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_MESSAGE + orderId));
    }

    private void verifyOrderBelongsToUser(Order order, String userId) {
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }
    }

    private void verifyOrderContainsSellerItems(Order order, String sellerId) {
        boolean hasSellerItems = order.getItems().stream()
                .anyMatch(item -> item.getSellerId().equals(sellerId));
        if (!hasSellerItems) {
            throw new IllegalArgumentException("Order does not contain seller's products");
        }
    }

    private String getSellerIdByEmail(String email) {
        try {
            com.buyapp.common.dto.UserDto user = webClientBuilder.build()
                    .get()
                    .uri(USER_SERVICE_URL + "/api/users/email/{email}", email)
                    .retrieve()
                    .bodyToMono(com.buyapp.common.dto.UserDto.class)
                    .block();
            if (user == null) {
                throw new ResourceNotFoundException("Seller not found with email: " + email);
            }
            return user.getId();
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ResourceNotFoundException("Seller not found with email: " + email, e);
        }
    }

    // Conversion methods

    private OrderDto toDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setStatus(order.getStatus().name());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        // Shipping address is already a DTO (no conversion needed)
        dto.setShippingAddress(order.getShippingAddress());

        // Convert order items (using AllArgsConstructor)
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderItemDto(
                        item.getProductId(),
                        item.getProductName(),
                        item.getSellerId(),
                        item.getQuantity(),
                        item.getPrice()))
                .collect(Collectors.toList());

        dto.setItems(itemDtos);
        return dto;
    }

    private OrderDto toSellerOrderDto(Order order, String sellerId) {
        OrderDto dto = toDto(order);

        // Filter items to only include seller's products
        List<OrderItemDto> sellerItems = dto.getItems().stream()
                .filter(item -> item.getSellerId().equals(sellerId))
                .collect(Collectors.toList());

        // Recalculate total for seller's items only
        Double sellerTotal = sellerItems.stream()
                .mapToDouble(OrderItemDto::getTotal)
                .sum();

        dto.setItems(sellerItems);
        dto.setTotalAmount(sellerTotal);
        return dto;
    }

    private boolean isValidStatusTransition(OrderStatus current, OrderStatus next) {
        // Define valid status transitions
        return switch (current) {
            case PENDING -> next == OrderStatus.READY_FOR_DELIVERY || next == OrderStatus.CANCELLED;
            case READY_FOR_DELIVERY -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            case SHIPPED -> next == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false; // Terminal states
        };
    }
}
