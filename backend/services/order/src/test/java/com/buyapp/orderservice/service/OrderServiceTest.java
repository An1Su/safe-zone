package com.buyapp.orderservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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

import com.buyapp.common.dto.CartDto;
import com.buyapp.common.dto.CartDto.CartItemDto;
import com.buyapp.common.dto.OrderDto;
import com.buyapp.common.dto.ProductDto;
import com.buyapp.common.dto.ShippingAddressDto;
import com.buyapp.common.dto.UserDto;
import com.buyapp.common.exception.ResourceNotFoundException;
import com.buyapp.orderservice.model.Cart;
import com.buyapp.orderservice.model.CartItem;
import com.buyapp.orderservice.model.Order;
import com.buyapp.orderservice.model.OrderItem;
import com.buyapp.orderservice.model.OrderStatus;
import com.buyapp.orderservice.repository.CartRepository;
import com.buyapp.orderservice.repository.OrderRepository;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private OrderService orderService;

    private Cart testCart;
    private Order testOrder;
    private ProductDto testProductDto;
    private UserDto testUserDto;
    private ShippingAddressDto testShippingAddress;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        // Setup test cart
        testCart = new Cart("user1");
        CartItem testCartItem = new CartItem("product1", "Product 1", 2, 99.99);
        testCart.addItem(testCartItem);

        // Setup test order
        OrderItem testOrderItem = new OrderItem("product1", "Product 1", "seller1", 2, 99.99);
        testOrder = new Order("user1", Arrays.asList(testOrderItem), createShippingAddress());
        testOrder.setId("order1");
        testOrder.setStatus(OrderStatus.PENDING);

        // Setup test product DTO
        testProductDto = new ProductDto();
        testProductDto.setId("product1");
        testProductDto.setName("Product 1");
        testProductDto.setPrice(99.99);
        testProductDto.setStock(10);

        // Setup test user DTO
        testUserDto = new UserDto();
        testUserDto.setId("seller1");
        testUserDto.setEmail("seller@example.com");

        // Setup shipping address
        testShippingAddress = createShippingAddress();

        // Setup WebClient mocks (use doReturn to avoid generic type capture issues)
        // Using @MockitoSettings(strictness = Strictness.LENIENT) so lenient() not needed
        doReturn(webClient).when(webClientBuilder).build();
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestBodyUriSpec).when(webClient).post();
        // GET requests: uri() takes Object... (varargs), so use Object[].class
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), any(Object[].class));
        // POST requests: uri() returns RequestBodySpec, then retrieve() returns ResponseSpec
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(anyString(), any(Object[].class));
        doReturn(responseSpec).when(requestBodySpec).retrieve();
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
    }

    private ShippingAddressDto createShippingAddress() {
        ShippingAddressDto address = new ShippingAddressDto();
        address.setFullName("John Doe");
        address.setAddress("123 Main St");
        address.setCity("New York");
        address.setPhone("1234567890");
        return address;
    }

    // ========== Cart Tests ==========

    @Test
    void getCart_WhenCartExists_ShouldReturnCartDto() {
        // Arrange
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(testCart));
        mockProductServiceCall();

        // Act
        CartDto result = orderService.getCart("user1");

        // Assert
        assertNotNull(result);
        assertEquals("user1", result.getUserId());
        verify(cartRepository).findByUserId("user1");
    }

    @Test
    void getCart_WhenCartDoesNotExist_ShouldCreateNewCart() {
        // Arrange
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockProductServiceCall();

        // Act
        CartDto result = orderService.getCart("user1");

        // Assert
        assertNotNull(result);
        assertEquals("user1", result.getUserId());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addItem_WhenItemDoesNotExist_ShouldAddNewItem() {
        // Arrange
        Cart emptyCart = new Cart("user1");
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(emptyCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockProductServiceCall();

        CartItemDto itemDto = new CartItemDto("product1", "Product 1", 2, 99.99, null);

        // Act
        CartDto result = orderService.addItem("user1", itemDto);

        // Assert
        assertNotNull(result);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addItem_WhenItemExists_ShouldUpdateQuantity() {
        // Arrange
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockProductServiceCall();

        CartItemDto itemDto = new CartItemDto("product1", "Product 1", 3, 99.99, null);

        // Act
        CartDto result = orderService.addItem("user1", itemDto);

        // Assert
        assertNotNull(result);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addItem_WhenInsufficientStock_ShouldThrowException() {
        // Arrange
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(testCart));
        testProductDto.setStock(1); // Less than requested
        mockProductServiceCall();

        CartItemDto itemDto = new CartItemDto("product1", "Product 1", 5, 99.99, null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.addItem("user1", itemDto);
        });
    }

    @Test
    void updateItemQuantity_WhenValid_ShouldUpdateQuantity() {
        // Arrange
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockProductServiceCall();

        // Act
        CartDto result = orderService.updateItemQuantity("user1", "product1", 5);

        // Assert
        assertNotNull(result);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void updateItemQuantity_WhenCartNotFound_ShouldThrowException() {
        // Arrange
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.updateItemQuantity("user1", "product1", 5);
        });
    }

    @Test
    void updateItemQuantity_WhenQuantityLessThanOne_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.updateItemQuantity("user1", "product1", 0);
        });
    }

    @Test
    void removeItem_WhenValid_ShouldRemoveItem() {
        // Arrange
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CartDto result = orderService.removeItem("user1", "product1");

        // Assert
        assertNotNull(result);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void clearCart_WhenValid_ShouldClearCart() {
        // Arrange
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        orderService.clearCart("user1");

        // Assert
        verify(cartRepository).save(any(Cart.class));
    }

    // ========== Order Tests ==========

    @Test
    void createOrder_WhenValid_ShouldCreateOrder() {
        // Arrange
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(testCart));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockProductServiceCall();
        mockSellerIdCall();
        mockStockReductionCall();

        // Act
        OrderDto result = orderService.createOrder("user1", testShippingAddress);

        // Assert
        assertNotNull(result);
        assertEquals("user1", result.getUserId());
        verify(orderRepository).save(any(Order.class));
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void createOrder_WhenCartIsEmpty_ShouldThrowException() {
        // Arrange
        Cart emptyCart = new Cart("user1");
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(emptyCart));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder("user1", testShippingAddress);
        });
    }

    @Test
    void createOrder_WhenCartNotFound_ShouldThrowException() {
        // Arrange
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.createOrder("user1", testShippingAddress);
        });
    }

    @Test
    void getOrders_WhenValid_ShouldReturnOrders() {
        // Arrange
        when(orderRepository.findByUserIdOrderByCreatedAtDesc("user1"))
                .thenReturn(Arrays.asList(testOrder));

        // Act
        List<OrderDto> result = orderService.getOrders("user1");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(orderRepository).findByUserIdOrderByCreatedAtDesc("user1");
    }

    @Test
    void getOrderById_WhenValid_ShouldReturnOrder() {
        // Arrange
        when(orderRepository.findById("order1")).thenReturn(Optional.of(testOrder));

        // Act
        OrderDto result = orderService.getOrderById("order1", "user1");

        // Assert
        assertNotNull(result);
        assertEquals("order1", result.getId());
        verify(orderRepository).findById("order1");
    }

    @Test
    void getOrderById_WhenOrderDoesNotBelongToUser_ShouldThrowException() {
        // Arrange
        when(orderRepository.findById("order1")).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.getOrderById("order1", "differentUser");
        });
    }

    @Test
    void cancelOrder_WhenValid_ShouldCancelOrder() {
        // Arrange
        testOrder.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById("order1")).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockStockRestorationCall();

        // Act
        OrderDto result = orderService.cancelOrder("order1", "user1");

        // Assert
        assertNotNull(result);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void cancelOrder_WhenOrderCannotBeCancelled_ShouldThrowException() {
        // Arrange
        testOrder.setStatus(OrderStatus.SHIPPED); // Cannot cancel shipped orders
        when(orderRepository.findById("order1")).thenReturn(Optional.of(testOrder));
        // Mock stock restoration (happens before cancel check, but will fail when cancel() throws)
        mockStockRestorationCall();

        assertThrows(IllegalStateException.class, () -> {
            orderService.cancelOrder("order1", "user1");
        });
    }

    @Test
    void deleteOrder_WhenValid_ShouldDeleteOrder() {
        // Arrange
        testOrder.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById("order1")).thenReturn(Optional.of(testOrder));

        // Act
        orderService.deleteOrder("order1", "user1");

        // Assert
        verify(orderRepository).delete(testOrder);
    }

    @Test
    void deleteOrder_WhenOrderCannotBeDeleted_ShouldThrowException() {
        // Arrange
        testOrder.setStatus(OrderStatus.PENDING); // Cannot delete pending orders
        when(orderRepository.findById("order1")).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            orderService.deleteOrder("order1", "user1");
        });
    }

    @Test
    void redoOrder_WhenValid_ShouldCreateNewOrder() {
        // Arrange
        testOrder.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById("order1")).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockProductServiceCall();
        mockStockReductionCall();

        // Act
        OrderDto result = orderService.redoOrder("order1", "user1");

        // Assert
        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class)); // Only the new order is saved
    }

    @Test
    void searchOrders_WhenNoFilters_ShouldReturnAllOrders() {
        // Arrange
        when(orderRepository.findByUserIdOrderByCreatedAtDesc("user1"))
                .thenReturn(Arrays.asList(testOrder));

        // Act
        List<OrderDto> result = orderService.searchOrders("user1", null, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void searchOrders_WhenStatusFilter_ShouldReturnFilteredOrders() {
        // Arrange
        when(orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc("user1", OrderStatus.PENDING))
                .thenReturn(Arrays.asList(testOrder));

        // Act
        List<OrderDto> result = orderService.searchOrders("user1", null, OrderStatus.PENDING, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void searchOrders_WhenQueryFilter_ShouldReturnMatchingOrders() {
        // Arrange
        when(orderRepository.findByUserIdOrderByCreatedAtDesc("user1"))
                .thenReturn(Arrays.asList(testOrder));

        // Act
        List<OrderDto> result = orderService.searchOrders("user1", "order1", null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void searchOrders_WhenDateRangeFilter_ShouldReturnFilteredOrders() {
        // Arrange
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        when(orderRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc("user1", from, to))
                .thenReturn(Arrays.asList(testOrder));

        // Act
        List<OrderDto> result = orderService.searchOrders("user1", null, null, from, to);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========== Seller Order Tests ==========

    @Test
    void getSellerOrders_WhenValid_ShouldReturnSellerOrders() {
        // Arrange
        when(orderRepository.findByItemsSellerIdOrderByCreatedAtDesc("seller1"))
                .thenReturn(Arrays.asList(testOrder));
        mockUserServiceCall();

        // Act
        List<OrderDto> result = orderService.getSellerOrders("seller@example.com");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getSellerOrderById_WhenValid_ShouldReturnSellerOrder() {
        // Arrange
        when(orderRepository.findById("order1")).thenReturn(Optional.of(testOrder));
        mockUserServiceCall();

        // Act
        OrderDto result = orderService.getSellerOrderById("order1", "seller@example.com");

        // Assert
        assertNotNull(result);
        assertEquals("order1", result.getId());
    }

    @Test
    void getSellerOrderById_WhenOrderDoesNotContainSellerItems_ShouldThrowException() {
        // Arrange
        when(orderRepository.findById("order1")).thenReturn(Optional.of(testOrder));
        testUserDto.setId("differentSeller");
        mockUserServiceCall();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.getSellerOrderById("order1", "seller@example.com");
        });
    }

    @Test
    void updateOrderStatus_WhenValid_ShouldUpdateStatus() {
        // Arrange
        when(orderRepository.findById("order1")).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockUserServiceCall();

        // Act
        OrderDto result = orderService.updateOrderStatus("order1", OrderStatus.READY_FOR_DELIVERY, "seller@example.com");

        // Assert
        assertNotNull(result);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_WhenInvalidTransition_ShouldThrowException() {
        // Arrange
        testOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById("order1")).thenReturn(Optional.of(testOrder));
        mockUserServiceCall();

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            orderService.updateOrderStatus("order1", OrderStatus.PENDING, "seller@example.com");
        });
    }

    @Test
    void searchSellerOrders_WhenValid_ShouldReturnFilteredOrders() {
        // Arrange
        when(orderRepository.findByItemsSellerIdOrderByCreatedAtDesc("seller1"))
                .thenReturn(Arrays.asList(testOrder));
        mockUserServiceCall();

        // Act
        List<OrderDto> result = orderService.searchSellerOrders("seller@example.com", null, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========== Helper Methods ==========

    private void mockProductServiceCall() {
        lenient().when(responseSpec.bodyToMono(ProductDto.class))
                .thenReturn(Mono.just(testProductDto));
    }

    private void mockSellerIdCall() {
        lenient().when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just("seller1"));
    }

    private void mockStockReductionCall() {
        lenient().when(responseSpec.bodyToMono(Void.class))
                .thenReturn(Mono.empty());
    }

    private void mockStockRestorationCall() {
        lenient().when(responseSpec.bodyToMono(Void.class))
                .thenReturn(Mono.empty());
    }

    private void mockUserServiceCall() {
        lenient().when(responseSpec.bodyToMono(UserDto.class))
                .thenReturn(Mono.just(testUserDto));
    }
}
