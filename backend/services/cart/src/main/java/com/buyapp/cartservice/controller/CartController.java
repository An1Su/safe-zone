package com.buyapp.cartservice.controller;

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

import com.buyapp.cartservice.service.CartService;
import com.buyapp.common.dto.CartDto;
import com.buyapp.common.dto.CartDto.CartItemDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Get user's cart
     */
    @GetMapping
    public ResponseEntity<CartDto> getCart(@RequestHeader("X-User-Email") String userId) {
        CartDto cart = cartService.getCart(userId);
        return ResponseEntity.ok(cart);
    }

    /**
     * Add item to cart
     */
    @PostMapping("/items")
    public ResponseEntity<CartDto> addItem(@Valid @RequestBody CartItemDto itemDto,
            @RequestHeader("X-User-Email") String userId) {
        CartDto cart = cartService.addItem(userId, itemDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(cart);
    }

    /**
     * Update item quantity in cart
     */
    @PutMapping("/items/{productId}")
    public ResponseEntity<CartDto> updateItemQuantity(
            @PathVariable String productId,
            @RequestParam Integer quantity,
            @RequestHeader("X-User-Email") String userId) {
        CartDto cart = cartService.updateItemQuantity(userId, productId, quantity);
        return ResponseEntity.ok(cart);
    }

    /**
     * Remove item from cart
     */
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartDto> removeItem(@PathVariable String productId,
            @RequestHeader("X-User-Email") String userId) {
        CartDto cart = cartService.removeItem(userId, productId);
        return ResponseEntity.ok(cart);
    }

    /**
     * Clear entire cart
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart(@RequestHeader("X-User-Email") String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
