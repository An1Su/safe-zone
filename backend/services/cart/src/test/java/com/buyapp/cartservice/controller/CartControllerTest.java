package com.buyapp.cartservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.buyapp.cartservice.service.CartService;
import com.buyapp.common.dto.CartDto;
import com.buyapp.common.dto.CartDto.CartItemDto;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private String testUserId;
    private CartDto testCartDto;
    private CartItemDto testCartItemDto;

    @BeforeEach
    void setUp() {
        testUserId = "user1@example.com";
        
        testCartDto = new CartDto();
        testCartDto.setUserId(testUserId);
        
        testCartItemDto = new CartItemDto();
        testCartItemDto.setProductId("product1");
        testCartItemDto.setQuantity(2);
    }

    @Test
    void getCart_ShouldReturnCart() {
        when(cartService.getCart(testUserId)).thenReturn(testCartDto);

        ResponseEntity<CartDto> response = cartController.getCart(testUserId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testCartDto, response.getBody());
        verify(cartService).getCart(testUserId);
    }

    @Test
    void addItem_ShouldReturnCreatedCart() {
        when(cartService.addItem(anyString(), any(CartItemDto.class))).thenReturn(testCartDto);

        ResponseEntity<CartDto> response = cartController.addItem(testCartItemDto, testUserId);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(testCartDto, response.getBody());
        verify(cartService).addItem(testUserId, testCartItemDto);
    }

    @Test
    void updateItemQuantity_ShouldReturnUpdatedCart() {
        Integer quantity = 5;
        when(cartService.updateItemQuantity(anyString(), anyString(), any(Integer.class))).thenReturn(testCartDto);

        ResponseEntity<CartDto> response = cartController.updateItemQuantity("product1", quantity, testUserId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testCartDto, response.getBody());
        verify(cartService).updateItemQuantity(testUserId, "product1", quantity);
    }

    @Test
    void removeItem_ShouldReturnUpdatedCart() {
        when(cartService.removeItem(anyString(), anyString())).thenReturn(testCartDto);

        ResponseEntity<CartDto> response = cartController.removeItem("product1", testUserId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testCartDto, response.getBody());
        verify(cartService).removeItem(testUserId, "product1");
    }

    @Test
    void clearCart_ShouldReturnNoContent() {
        ResponseEntity<Void> response = cartController.clearCart(testUserId);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(cartService).clearCart(testUserId);
    }
}
