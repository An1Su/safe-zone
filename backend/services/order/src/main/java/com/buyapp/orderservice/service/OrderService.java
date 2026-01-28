package com.buyapp.orderservice.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.buyapp.common.dto.CartDto;
import com.buyapp.common.dto.CartDto.CartItemDto;
import com.buyapp.common.dto.OrderDto;
import com.buyapp.common.dto.OrderDto.OrderItemDto;
import com.buyapp.common.dto.ProductDto;
import com.buyapp.common.dto.ShippingAddressDto;
import com.buyapp.common.exception.ResourceNotFoundException;
import com.buyapp.orderservice.model.Cart;
import com.buyapp.orderservice.model.CartItem;
import com.buyapp.orderservice.model.Order;
import com.buyapp.orderservice.model.OrderItem;
import com.buyapp.orderservice.model.OrderStatus;
import com.buyapp.orderservice.repository.CartRepository;
import com.buyapp.orderservice.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final WebClient.Builder webClientBuilder;

    /**
     * Service URLs for Eureka service discovery.
     * These match the spring.application.name from each service's application.yml.
     * @LoadBalanced WebClient resolves these through Eureka to find the actual service instances.
     */
    private static final String PRODUCT_SERVICE_URL = "http://product-service";
    private static final String USER_SERVICE_URL = "http://user-service";
    private static final String ORDER_NOT_FOUND_MESSAGE = "Order not found with id: ";
    private static final String CART_NOT_FOUND_MESSAGE = "Cart not found for user: ";

    public OrderService(OrderRepository orderRepository, CartRepository cartRepository, WebClient.Builder webClientBuilder) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.webClientBuilder = webClientBuilder;
    }

    // Cart

    //Get user's cart or create a new one if it doesn't exist
    public CartDto getCart(String userId) {
        Cart cart = getOrCreateCart(userId);
        return toCartDto(cart, true); // Validate availability when fetching cart
    }

    //Add item to cart or update quantity if item already exists

    public CartDto addItem(String userId, CartItemDto itemDto) {
        // Validate product exists and stock availability
        ProductDto product = getProductByIdOrThrow(itemDto.getProductId());

        // Get or create cart
        Cart cart = getOrCreateCart(userId);

        // Check if item already exists to validate total quantity
        CartItem existingItem = cart.findItemByProductId(itemDto.getProductId());
        if (existingItem != null) {
            // Validate total quantity doesn't exceed stock
            int newQuantity = existingItem.getQuantity() + itemDto.getQuantity();
            validateStockAvailability(product, newQuantity);
        } else {
            // Validate initial quantity
            validateStockAvailability(product, itemDto.getQuantity());
        }

        // Use Cart's method to add or update item
        CartItem newItem = new CartItem(
                itemDto.getProductId(),
                product.getName(),
                itemDto.getQuantity(),
                product.getPrice());
        cart.addOrUpdateItem(newItem); // Handles both add and update, updates updatedAt

        Cart savedCart = cartRepository.save(cart);

        return toCartDto(savedCart, false); // No need to validate again, we just validated
    }

    //Update item quantity in cart
    public CartDto updateItemQuantity(String userId, String productId, Integer quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(CART_NOT_FOUND_MESSAGE + userId));

        CartItem item = cart.findItemByProductId(productId);
        if (item == null) {
            throw new ResourceNotFoundException("Item not found in cart: " + productId);
        }

        ProductDto product = getProductByIdOrThrow(productId);
        validateStockAvailability(product, quantity);

        cart.updateItem(productId, quantity, product.getPrice());
        Cart savedCart = cartRepository.save(cart);
        return toCartDto(savedCart, false);
    }

    public CartDto removeItem(String userId, String productId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(CART_NOT_FOUND_MESSAGE + userId));

        cart.removeItem(productId); // removeItem already updates updatedAt
        Cart savedCart = cartRepository.save(cart);
        return toCartDto(savedCart, false);
    }

    public void clearCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(CART_NOT_FOUND_MESSAGE + userId));

        cart.clear(); // Uses Cart's clear() method which updates updatedAt
        cartRepository.save(cart);
    }

    //Order

    //Create order from cart
    public OrderDto createOrder(String userId, ShippingAddressDto shippingAddressDto) {
        // Get cart from local repository
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(CART_NOT_FOUND_MESSAGE + userId));

        if (cart.isEmpty()) {
            throw new IllegalArgumentException("Cannot create order from empty cart");
        }

        // Cache seller IDs to avoid redundant calls
        Map<String, String> sellerIdCache = new HashMap<>();

        // Validate all items are available and cache seller IDs
        for (CartItem item : cart.getItems()) {
            ProductDto product = getProductByIdOrThrow(item.getProductId());
            validateStockAvailability(product, item.getQuantity());

            // Cache seller ID
            String sellerId = getProductSellerId(item.getProductId());
            if (sellerId == null) {
                throw new ResourceNotFoundException("Seller not found for product: " + product.getName());
            }
            sellerIdCache.put(item.getProductId(), sellerId);
        }

        // Convert cart items to order items (using cached seller IDs)
        List<OrderItem> orderItems = cart.toOrderItems(sellerIdCache);

        // Create and save order
        Order order = new Order(userId, orderItems, shippingAddressDto);
        Order savedOrder = orderRepository.save(order);

        // Reduce stock for all products (after order is saved)
        // If this fails, order exists but stock wasn't reduced - consider transaction management
        reduceStockForItems(orderItems);

        // Clear cart after successful order creation and stock reduction
        clearCart(userId);

        return toDto(savedOrder);
    }

    public List<OrderDto> getOrders(String userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream()
                .map(this::toDto)
                .toList();
    }

    public OrderDto getOrderById(String orderId, String userId) {
        Order order = findOrderById(orderId);
        if (!order.belongsToUser(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }
        return toDto(order);
    }

    public OrderDto cancelOrder(String orderId, String userId) {
        Order order = findOrderById(orderId);
        if (!order.belongsToUser(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        // Restore stock for all products
        restoreStockForItems(order.getItems());

        // Use Order's cancel() method which handles status and timestamp
        order.cancel();
        Order savedOrder = orderRepository.save(order);

        return toDto(savedOrder);
    }

    public void deleteOrder(String orderId, String userId) {
        Order order = findOrderById(orderId);
        if (!order.belongsToUser(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        // Check if order can be deleted
        if (!order.canBeDeleted()) {
            throw new IllegalStateException(
                    "Order cannot be deleted. Current status: " + order.getStatus());
        }

        orderRepository.delete(order);
    }

    public OrderDto redoOrder(String orderId, String userId) {
        Order originalOrder = findOrderById(orderId);
        if (!originalOrder.belongsToUser(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        // Create new order with same items and shipping address
        Order newOrder = new Order(
                userId,
                originalOrder.getItems(),
                originalOrder.getShippingAddress());

        // Validate stock availability
        for (OrderItem item : newOrder.getItems()) {
            ProductDto product = getProductByIdOrThrow(item.getProductId());
            validateStockAvailability(product, item.getQuantity());
        }

        Order savedOrder = orderRepository.save(newOrder);

        // Reduce stock for all products
        reduceStockForItems(savedOrder.getItems());

        return toDto(savedOrder);
    }

    public List<OrderDto> searchOrders(String userId, String query, OrderStatus status,
            LocalDateTime dateFrom, LocalDateTime dateTo) {
        List<Order> orders;

        // Optimize database query: use status filter if no date range and status is provided
        if (dateFrom != null && dateTo != null) {
            orders = orderRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                    userId, dateFrom, dateTo);
        } else if (status != null) {
            // Use repository method to filter by status at database level
            orders = orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        } else {
            orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }

        // Filter by status if not already filtered at DB level (when date range was used)
        if (status != null && dateFrom != null && dateTo != null) {
            orders = orders.stream()
                    .filter(order -> order.getStatus() == status)
                    .toList();
        }

        // Early return if no query filter
        if (query == null || query.trim().isEmpty()) {
            return orders.stream()
                    .map(this::toDto)
                    .toList();
        }

        // Filter by query (order ID or product name)
        String lowerQuery = query.toLowerCase();
        return orders.stream()
                .filter(order -> order.matchesQuery(lowerQuery))
                .map(this::toDto)
                .toList();
    }

    public List<OrderDto> getSellerOrders(String sellerEmail) {
        String sellerId = getSellerIdByEmail(sellerEmail);
        List<Order> orders = orderRepository.findByItemsSellerIdOrderByCreatedAtDesc(sellerId);
        return orders.stream()
                .map(order -> toSellerOrderDto(order, sellerId))
                .toList();
    }

    public OrderDto getSellerOrderById(String orderId, String sellerEmail) {
        String sellerId = getSellerIdByEmail(sellerEmail);
        Order order = findOrderById(orderId);
        if (!order.containsSellerItems(sellerId)) {
            throw new IllegalArgumentException("Order does not contain seller's products");
        }
        return toSellerOrderDto(order, sellerId);
    }

    public OrderDto updateOrderStatus(String orderId, OrderStatus newStatus, String sellerEmail) {
        String sellerId = getSellerIdByEmail(sellerEmail);
        Order order = findOrderById(orderId);
        if (!order.containsSellerItems(sellerId)) {
            throw new IllegalArgumentException("Order does not contain seller's products");
        }

        // Use Order's updateStatus() method which validates transition and updates timestamp
        order.updateStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        return toSellerOrderDto(savedOrder, sellerId);
    }

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

        // Filter by seller's items first
        orders = orders.stream()
                .filter(order -> order.containsSellerItems(sellerId))
                .toList();

        // Filter by status if provided
        if (status != null) {
            orders = orders.stream()
                    .filter(order -> order.getStatus() == status)
                    .toList();
        }

        // Early return if no query filter
        if (query == null || query.trim().isEmpty()) {
            return orders.stream()
                    .map(order -> toSellerOrderDto(order, sellerId))
                    .toList();
        }

        // Filter by query (order ID or seller's product names)
        String lowerQuery = query.toLowerCase();
        return orders.stream()
                .filter(order -> order.matchesSellerQuery(sellerId, lowerQuery))
                .map(order -> toSellerOrderDto(order, sellerId))
                .toList();
    }

    // Helper methods for inter-service communication

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

    private ProductDto getProductByIdOrThrow(String productId) {
        ProductDto product = getProductById(productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        return product;
    }

    private void validateStockAvailability(ProductDto product, Integer requestedQuantity) {
        if (product.getStock() == null || product.getStock() < requestedQuantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock for product: " + product.getName() +
                            ". Available: " + (product.getStock() != null ? product.getStock() : 0) +
                            ", Requested: " + requestedQuantity);
        }
    }

    private Cart getOrCreateCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(new Cart(userId)));
    }

    private void reduceStock(String productId, Integer quantity) {
        callStockEndpoint(productId, quantity, "reduce-stock", "reduce");
    }

    private void restoreStock(String productId, Integer quantity) {
        callStockEndpoint(productId, quantity, "restore-stock", "restore");
    }

    private void reduceStockForItems(List<OrderItem> items) {
        items.forEach(item -> reduceStock(item.getProductId(), item.getQuantity()));
    }

    private void restoreStockForItems(List<OrderItem> items) {
        items.forEach(item -> restoreStock(item.getProductId(), item.getQuantity()));
    }

    private void callStockEndpoint(String productId, Integer quantity, String endpoint, String action) {
        try {
            webClientBuilder.build()
                    .post()
                    .uri(PRODUCT_SERVICE_URL + "/api/products/{id}/" + endpoint + "?quantity={quantity}",
                            productId, quantity)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Failed to " + action + " stock for product: " + productId, e);
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
    private CartDto toCartDto(Cart cart, boolean validateAvailability) {
        CartDto dto = new CartDto();
        dto.setId(cart.getId());
        dto.setUserId(cart.getUserId());
        dto.setCreatedAt(cart.getCreatedAt());
        dto.setUpdatedAt(cart.getUpdatedAt());

        List<CartItemDto> itemDtos = cart.getItems().stream()
                .map(item -> {
                    Boolean available = null;
                    // Validate availability if requested
                    if (validateAvailability) {
                        ProductDto product = getProductById(item.getProductId());
                        available = product != null && product.getStock() != null
                                && product.getStock() >= item.getQuantity();
                    }
                    return new CartItemDto(
                            item.getProductId(),
                            item.getProductName(),
                            item.getQuantity(),
                            item.getPrice(),
                            available);
                })
                .toList();

        dto.setItems(itemDtos);
        dto.setTotal(cart.getTotal()); // Use Cart's getTotal() method
        return dto;
    }

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
                .toList();

        dto.setItems(itemDtos);
        return dto;
    }

    private OrderDto toSellerOrderDto(Order order, String sellerId) {
        OrderDto dto = toDto(order);

        // Filter items to only include seller's products
        List<OrderItemDto> sellerItems = dto.getItems().stream()
                .filter(item -> item.getSellerId().equals(sellerId))
                .toList();

        // Recalculate total for seller's items only
        Double sellerTotal = sellerItems.stream()
                .mapToDouble(OrderItemDto::getTotal)
                .sum();

        dto.setItems(sellerItems);
        dto.setTotalAmount(sellerTotal);
        return dto;
    }

}
