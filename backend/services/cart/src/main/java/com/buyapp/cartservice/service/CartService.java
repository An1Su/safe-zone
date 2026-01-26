package com.buyapp.cartservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.buyapp.cartservice.model.Cart;
import com.buyapp.cartservice.model.CartItem;
import com.buyapp.cartservice.repository.CartRepository;
import com.buyapp.common.dto.CartDto;
import com.buyapp.common.dto.CartDto.CartItemDto;
import com.buyapp.common.dto.ProductDto;
import com.buyapp.common.exception.ResourceNotFoundException;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final WebClient.Builder webClientBuilder;

    private static final String PRODUCT_SERVICE_URL = "http://product-service";
    private static final String CART_NOT_FOUND_MESSAGE = "Cart not found for user: ";

    public CartService(CartRepository cartRepository, WebClient.Builder webClientBuilder) {
        this.cartRepository = cartRepository;
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * Get user's cart or create a new one if it doesn't exist
     */
    public CartDto getCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart(userId);
                    newCart.setCreatedAt(LocalDateTime.now());
                    newCart.setUpdatedAt(LocalDateTime.now());
                    return cartRepository.save(newCart);
                });

        return toDto(cart, true); // Validate availability when fetching cart
    }

    /**
     * Add item to cart or update quantity if item already exists
     * Validates stock availability before adding
     */
    public CartDto addItem(String userId, CartItemDto itemDto) {
        // Validate product exists and stock availability
        ProductDto product = getProductById(itemDto.getProductId());

        if (product == null) {
            throw new ResourceNotFoundException("Product not found with id: " + itemDto.getProductId());
        }

        // Check stock availability
        if (product.getStock() == null || product.getStock() < itemDto.getQuantity()) {
            throw new IllegalArgumentException(
                    "Insufficient stock for product: " + product.getName() +
                            ". Available: " + (product.getStock() != null ? product.getStock() : 0) +
                            ", Requested: " + itemDto.getQuantity());
        }

        // Get or create cart
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart(userId);
                    newCart.setCreatedAt(LocalDateTime.now());
                    return cartRepository.save(newCart);
                });

        // Check if item already exists in cart
        CartItem existingItem = cart.findItemByProductId(itemDto.getProductId());

        if (existingItem != null) {
            // Update quantity (validate total quantity doesn't exceed stock)
            int newQuantity = existingItem.getQuantity() + itemDto.getQuantity();
            if (product.getStock() < newQuantity) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: " + product.getName() +
                                ". Available: " + product.getStock() +
                                ", Total requested (existing + new): " + newQuantity);
            }
            existingItem.setQuantity(newQuantity);
        } else {
            // Add new item
            CartItem newItem = new CartItem(
                    itemDto.getProductId(),
                    product.getName(),
                    itemDto.getQuantity(),
                    product.getPrice());
            cart.addItem(newItem);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        Cart savedCart = cartRepository.save(cart);

        return toDto(savedCart, false); // No need to validate again, we just validated
    }

    /**
     * Update item quantity in cart
     * Validates stock availability before updating
     */
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

        // Validate stock availability
        ProductDto product = getProductById(productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }

        if (product.getStock() == null || product.getStock() < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock for product: " + product.getName() +
                            ". Available: " + (product.getStock() != null ? product.getStock() : 0) +
                            ", Requested: " + quantity);
        }

        item.setQuantity(quantity);
        item.setPrice(product.getPrice()); // Update price in case it changed
        cart.setUpdatedAt(LocalDateTime.now());

        Cart savedCart = cartRepository.save(cart);
        return toDto(savedCart, false);
    }

    /**
     * Remove item from cart
     */
    public CartDto removeItem(String userId, String productId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(CART_NOT_FOUND_MESSAGE + userId));

        cart.removeItem(productId);
        cart.setUpdatedAt(LocalDateTime.now());

        Cart savedCart = cartRepository.save(cart);
        return toDto(savedCart, false);
    }

    /**
     * Clear all items from cart
     */
    public void clearCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(CART_NOT_FOUND_MESSAGE + userId));

        cart.setItems(new java.util.ArrayList<>());
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }

    /**
     * Fetch product from Product Service
     */
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

    /**
     * Convert Cart entity to CartDto
     * 
     * @param validateAvailability if true, checks product availability for each
     *                             item
     */
    private CartDto toDto(Cart cart, boolean validateAvailability) {
        CartDto dto = new CartDto();
        dto.setId(cart.getId());
        dto.setUserId(cart.getUserId());
        dto.setCreatedAt(cart.getCreatedAt());
        dto.setUpdatedAt(cart.getUpdatedAt());

        List<CartItemDto> itemDtos = cart.getItems().stream()
                .map(item -> {
                    CartItemDto itemDto = new CartItemDto();
                    itemDto.setProductId(item.getProductId());
                    itemDto.setProductName(item.getProductName());
                    itemDto.setQuantity(item.getQuantity());
                    itemDto.setPrice(item.getPrice());

                    // Validate availability if requested
                    if (validateAvailability) {
                        ProductDto product = getProductById(item.getProductId());
                        if (product != null && product.getStock() != null) {
                            itemDto.setAvailable(product.getStock() >= item.getQuantity());
                        } else {
                            itemDto.setAvailable(false); // Product not found or no stock
                        }
                    }

                    return itemDto;
                })
                .toList();

        dto.setItems(itemDtos);
        return dto;
    }
}
