package com.buyapp.cartservice.controller;

import com.buyapp.cartservice.service.CartService;
import com.buyapp.common.dto.CartDto;
import com.buyapp.common.dto.CartDto.CartItemDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

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
    public ResponseEntity<CartDto> addItem(@Valid @RequestBody CartItemDto itemDto, @RequestHeader("X-User-Email") String userId) {
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
    public ResponseEntity<CartDto> removeItem(@PathVariable String productId, @RequestHeader("X-User-Email") String userId) {
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

