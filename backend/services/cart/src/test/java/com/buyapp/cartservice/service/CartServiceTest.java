package com.buyapp.cartservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.WebClient;

import com.buyapp.cartservice.model.Cart;
import com.buyapp.cartservice.model.CartItem;
import com.buyapp.cartservice.repository.CartRepository;
import com.buyapp.common.dto.CartDto;
import com.buyapp.common.dto.CartDto.CartItemDto;
import com.buyapp.common.dto.ProductDto;
import com.buyapp.common.exception.ResourceNotFoundException;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private CartService cartService;

    private Cart testCart;
    private CartItem testCartItem;
    private ProductDto testProductDto;
    private String testUserId;
    private String testProductId;

    @BeforeEach
    void setUp() {
        testUserId = "user1@example.com";
        testProductId = "product1";

        testProductDto = new ProductDto();
        testProductDto.setId(testProductId);
        testProductDto.setName("Test Product");
        testProductDto.setPrice(99.99);
        testProductDto.setStock(10);

        testCartItem = new CartItem(testProductId, "Test Product", 2, 99.99);
        testCart = new Cart(testUserId);
        testCart.setId("cart1");
        testCart.setCreatedAt(LocalDateTime.now());
        testCart.setUpdatedAt(LocalDateTime.now());
        testCart.addItem(testCartItem);

        // Setup WebClient mocking chain (lenient - not all tests use WebClient)
        lenient().doReturn(webClient).when(webClientBuilder).build();
        lenient().doReturn(requestHeadersUriSpec).when(webClient).get();
        lenient().doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), anyString());
        lenient().doReturn(responseSpec).when(requestHeadersSpec).retrieve();
    }

    @Test
    void getCart_ShouldReturnExistingCart() {
        when(cartRepository.findByUserId(testUserId)).thenReturn(Optional.of(testCart));
        when(responseSpec.bodyToMono(ProductDto.class)).thenReturn(Mono.just(testProductDto));

        CartDto result = cartService.getCart(testUserId);

        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(1, result.getItems().size());
        verify(cartRepository, times(1)).findByUserId(testUserId);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void getCart_ShouldCreateNewCartIfNotExists() {
        when(cartRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(responseSpec.bodyToMono(ProductDto.class)).thenReturn(Mono.just(testProductDto));

        CartDto result = cartService.getCart(testUserId);

        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertTrue(result.getItems().isEmpty());
        verify(cartRepository, times(1)).findByUserId(testUserId);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void addItem_ShouldAddNewItemToCart() {
        Cart emptyCart = new Cart(testUserId);
        emptyCart.setId("cart1");
        when(cartRepository.findByUserId(testUserId)).thenReturn(Optional.of(emptyCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(responseSpec.bodyToMono(ProductDto.class)).thenReturn(Mono.just(testProductDto));

        CartItemDto itemDto = new CartItemDto();
        itemDto.setProductId(testProductId);
        itemDto.setQuantity(3);

        CartDto result = cartService.addItem(testUserId, itemDto);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(3, result.getItems().get(0).getQuantity());
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void addItem_ShouldUpdateQuantityIfItemExists() {
        when(cartRepository.findByUserId(testUserId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(responseSpec.bodyToMono(ProductDto.class)).thenReturn(Mono.just(testProductDto));

        CartItemDto itemDto = new CartItemDto();
        itemDto.setProductId(testProductId);
        itemDto.setQuantity(3);

        CartDto result = cartService.addItem(testUserId, itemDto);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(5, result.getItems().get(0).getQuantity()); // 2 existing + 3 new
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void addItem_ShouldThrowExceptionWhenInsufficientStock() {
        testProductDto.setStock(1);
        lenient().when(cartRepository.findByUserId(testUserId)).thenReturn(Optional.of(testCart));
        when(responseSpec.bodyToMono(ProductDto.class)).thenReturn(Mono.just(testProductDto));

        CartItemDto itemDto = new CartItemDto();
        itemDto.setProductId(testProductId);
        itemDto.setQuantity(5);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cartService.addItem(testUserId, itemDto));

        assertTrue(exception.getMessage().contains("Insufficient stock"));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItem_ShouldThrowExceptionWhenProductNotFound() {
        when(responseSpec.bodyToMono(ProductDto.class)).thenReturn(Mono.empty());

        CartItemDto itemDto = new CartItemDto();
        itemDto.setProductId("nonexistent");
        itemDto.setQuantity(1);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> cartService.addItem(testUserId, itemDto));

        assertTrue(exception.getMessage().contains("Product not found"));
    }

    @Test
    void updateItemQuantity_ShouldUpdateQuantity() {
        when(cartRepository.findByUserId(testUserId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(responseSpec.bodyToMono(ProductDto.class)).thenReturn(Mono.just(testProductDto));

        CartDto result = cartService.updateItemQuantity(testUserId, testProductId, 5);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(5, result.getItems().get(0).getQuantity());
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void updateItemQuantity_ShouldThrowExceptionWhenInsufficientStock() {
        testProductDto.setStock(3);
        when(cartRepository.findByUserId(testUserId)).thenReturn(Optional.of(testCart));
        when(responseSpec.bodyToMono(ProductDto.class)).thenReturn(Mono.just(testProductDto));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cartService.updateItemQuantity(testUserId, testProductId, 5));

        assertTrue(exception.getMessage().contains("Insufficient stock"));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateItemQuantity_ShouldThrowExceptionWhenQuantityLessThanOne() {
        assertThrows(IllegalArgumentException.class,
                () -> cartService.updateItemQuantity(testUserId, testProductId, 0));
    }

    @Test
    void updateItemQuantity_ShouldThrowExceptionWhenCartNotFound() {
        when(cartRepository.findByUserId(testUserId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> cartService.updateItemQuantity(testUserId, testProductId, 5));

        assertTrue(exception.getMessage().contains("Cart not found"));
    }

    @Test
    void updateItemQuantity_ShouldThrowExceptionWhenItemNotFound() {
        Cart emptyCart = new Cart(testUserId);
        emptyCart.setItems(new ArrayList<>());
        when(cartRepository.findByUserId(testUserId)).thenReturn(Optional.of(emptyCart));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> cartService.updateItemQuantity(testUserId, testProductId, 5));

        assertTrue(exception.getMessage().contains("Item not found in cart"));
    }

    @Test
    void removeItem_ShouldRemoveItemFromCart() {
        when(cartRepository.findByUserId(testUserId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartDto result = cartService.removeItem(testUserId, testProductId);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void removeItem_ShouldThrowExceptionWhenCartNotFound() {
        when(cartRepository.findByUserId(testUserId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> cartService.removeItem(testUserId, testProductId));

        assertTrue(exception.getMessage().contains("Cart not found"));
    }

    @Test
    void clearCart_ShouldClearAllItems() {
        when(cartRepository.findByUserId(testUserId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cartService.clearCart(testUserId);

        verify(cartRepository, times(1)).save(any(Cart.class));
        Cart savedCart = testCart;
        assertTrue(savedCart.getItems().isEmpty());
    }

    @Test
    void clearCart_ShouldThrowExceptionWhenCartNotFound() {
        when(cartRepository.findByUserId(testUserId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> cartService.clearCart(testUserId));

        assertTrue(exception.getMessage().contains("Cart not found"));
    }

    @Test
    void getCart_ShouldSetAvailableFlagWhenProductInStock() {
        when(cartRepository.findByUserId(testUserId)).thenReturn(Optional.of(testCart));
        when(responseSpec.bodyToMono(ProductDto.class)).thenReturn(Mono.just(testProductDto));

        CartDto result = cartService.getCart(testUserId);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertTrue(Boolean.TRUE.equals(result.getItems().get(0).getAvailable())); // Stock (10) >= quantity (2)
    }

    @Test
    void getCart_ShouldSetUnavailableFlagWhenProductOutOfStock() {
        testProductDto.setStock(1); // Less than cart quantity (2)
        when(cartRepository.findByUserId(testUserId)).thenReturn(Optional.of(testCart));
        when(responseSpec.bodyToMono(ProductDto.class)).thenReturn(Mono.just(testProductDto));

        CartDto result = cartService.getCart(testUserId);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertFalse(Boolean.TRUE.equals(result.getItems().get(0).getAvailable()));
    }
}
