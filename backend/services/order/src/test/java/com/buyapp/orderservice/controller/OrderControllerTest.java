package com.buyapp.orderservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.buyapp.common.dto.CartDto;
import com.buyapp.common.dto.CartDto.CartItemDto;
import com.buyapp.common.dto.OrderDto;
import com.buyapp.common.dto.ShippingAddressDto;
import com.buyapp.orderservice.model.OrderStatus;
import com.buyapp.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private CartDto testCartDto;
    private OrderDto testOrderDto;
    private ShippingAddressDto testShippingAddress;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
        objectMapper = new ObjectMapper();

        // Setup test cart DTO
        testCartDto = new CartDto();
        testCartDto.setId("cart1");
        testCartDto.setUserId("user1");
        CartItemDto cartItemDto = new CartItemDto("product1", "Product 1", 2, 99.99, true);
        testCartDto.setItems(Arrays.asList(cartItemDto));
        testCartDto.setTotal(199.98);

        // Setup test order DTO
        testOrderDto = new OrderDto();
        testOrderDto.setId("order1");
        testOrderDto.setUserId("user1");
        testOrderDto.setStatus("PENDING");
        testOrderDto.setTotalAmount(199.98);

        // Setup shipping address
        testShippingAddress = new ShippingAddressDto();
        testShippingAddress.setFullName("John Doe");
        testShippingAddress.setAddress("123 Main St");
        testShippingAddress.setCity("New York");
        testShippingAddress.setPhone("1234567890");
    }

    // ========== Cart Endpoint Tests ==========

    @Test
    void getCart_ShouldReturnCart() throws Exception {
        // Arrange
        when(orderService.getCart("user1")).thenReturn(testCartDto);

        // Act & Assert
        mockMvc.perform(get("/cart")
                .header("X-User-Email", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user1"))
                .andExpect(jsonPath("$.total").value(199.98));

        verify(orderService).getCart("user1");
    }

    @Test
    void addItem_ShouldReturnCreatedCart() throws Exception {
        // Arrange
        CartItemDto itemDto = new CartItemDto("product2", "Product 2", 1, 49.99, null);
        when(orderService.addItem(anyString(), any(CartItemDto.class))).thenReturn(testCartDto);

        // Act & Assert
        mockMvc.perform(post("/cart/items")
                .header("X-User-Email", "user1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isCreated());

        verify(orderService).addItem(eq("user1"), any(CartItemDto.class));
    }

    @Test
    void updateItemQuantity_ShouldReturnUpdatedCart() throws Exception {
        // Arrange
        when(orderService.updateItemQuantity("user1", "product1", 5)).thenReturn(testCartDto);

        // Act & Assert
        mockMvc.perform(put("/cart/items/product1")
                .header("X-User-Email", "user1")
                .param("quantity", "5"))
                .andExpect(status().isOk());

        verify(orderService).updateItemQuantity("user1", "product1", 5);
    }

    @Test
    void removeItem_ShouldReturnUpdatedCart() throws Exception {
        // Arrange
        when(orderService.removeItem("user1", "product1")).thenReturn(testCartDto);

        // Act & Assert
        mockMvc.perform(delete("/cart/items/product1")
                .header("X-User-Email", "user1"))
                .andExpect(status().isOk());

        verify(orderService).removeItem("user1", "product1");
    }

    @Test
    void clearCart_ShouldReturnNoContent() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/cart")
                .header("X-User-Email", "user1"))
                .andExpect(status().isNoContent());

        verify(orderService).clearCart("user1");
    }

    // ========== Order Endpoint Tests ==========

    @Test
    void createOrder_ShouldReturnCreatedOrder() throws Exception {
        // Arrange
        when(orderService.createOrder(anyString(), any(ShippingAddressDto.class))).thenReturn(testOrderDto);

        // Act & Assert
        mockMvc.perform(post("/orders")
                .header("X-User-Email", "user1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testShippingAddress)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("order1"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(orderService).createOrder(eq("user1"), any(ShippingAddressDto.class));
    }

    @Test
    void getOrders_ShouldReturnOrdersList() throws Exception {
        // Arrange
        List<OrderDto> orders = Arrays.asList(testOrderDto);
        when(orderService.getOrders("user1")).thenReturn(orders);

        // Act & Assert
        mockMvc.perform(get("/orders")
                .header("X-User-Email", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("order1"));

        verify(orderService).getOrders("user1");
    }

    @Test
    void getOrderById_ShouldReturnOrder() throws Exception {
        // Arrange
        when(orderService.getOrderById("order1", "user1")).thenReturn(testOrderDto);

        // Act & Assert
        mockMvc.perform(get("/orders/order1")
                .header("X-User-Email", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("order1"));

        verify(orderService).getOrderById("order1", "user1");
    }

    @Test
    void cancelOrder_ShouldReturnCancelledOrder() throws Exception {
        // Arrange
        testOrderDto.setStatus("CANCELLED");
        when(orderService.cancelOrder("order1", "user1")).thenReturn(testOrderDto);

        // Act & Assert
        mockMvc.perform(put("/orders/order1/cancel")
                .header("X-User-Email", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(orderService).cancelOrder("order1", "user1");
    }

    @Test
    void deleteOrder_ShouldReturnNoContent() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/orders/order1")
                .header("X-User-Email", "user1"))
                .andExpect(status().isNoContent());

        verify(orderService).deleteOrder("order1", "user1");
    }

    @Test
    void redoOrder_ShouldReturnNewOrder() throws Exception {
        // Arrange
        when(orderService.redoOrder("order1", "user1")).thenReturn(testOrderDto);

        // Act & Assert
        mockMvc.perform(post("/orders/order1/redo")
                .header("X-User-Email", "user1"))
                .andExpect(status().isCreated());

        verify(orderService).redoOrder("order1", "user1");
    }

    @Test
    void searchOrders_ShouldReturnFilteredOrders() throws Exception {
        // Arrange
        List<OrderDto> orders = Arrays.asList(testOrderDto);
        when(orderService.searchOrders(anyString(), anyString(), any(), any(), any())).thenReturn(orders);

        // Act & Assert
        mockMvc.perform(get("/orders/search")
                .header("X-User-Email", "user1")
                .param("q", "order1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("order1"));

        verify(orderService).searchOrders(eq("user1"), eq("order1"), eq(null), eq(null), eq(null));
    }

    // ========== Seller Endpoint Tests ==========

    @Test
    void getSellerOrders_ShouldReturnSellerOrders() throws Exception {
        // Arrange
        List<OrderDto> orders = Arrays.asList(testOrderDto);
        when(orderService.getSellerOrders("seller@example.com")).thenReturn(orders);

        // Act & Assert
        mockMvc.perform(get("/orders/seller")
                .header("X-User-Email", "seller@example.com"))
                .andExpect(status().isOk());

        verify(orderService).getSellerOrders("seller@example.com");
    }

    @Test
    void getSellerOrderById_ShouldReturnSellerOrder() throws Exception {
        // Arrange
        when(orderService.getSellerOrderById("order1", "seller@example.com")).thenReturn(testOrderDto);

        // Act & Assert
        mockMvc.perform(get("/orders/seller/order1")
                .header("X-User-Email", "seller@example.com"))
                .andExpect(status().isOk());

        verify(orderService).getSellerOrderById("order1", "seller@example.com");
    }

    @Test
    void updateOrderStatus_ShouldReturnUpdatedOrder() throws Exception {
        // Arrange
        testOrderDto.setStatus("READY_FOR_DELIVERY");
        when(orderService.updateOrderStatus("order1", OrderStatus.READY_FOR_DELIVERY, "seller@example.com"))
                .thenReturn(testOrderDto);

        // Act & Assert
        mockMvc.perform(put("/orders/order1/status")
                .header("X-User-Email", "seller@example.com")
                .param("status", "READY_FOR_DELIVERY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_DELIVERY"));

        verify(orderService).updateOrderStatus("order1", OrderStatus.READY_FOR_DELIVERY, "seller@example.com");
    }

    @Test
    void searchSellerOrders_ShouldReturnFilteredOrders() throws Exception {
        // Arrange
        List<OrderDto> orders = Arrays.asList(testOrderDto);
        when(orderService.searchSellerOrders(anyString(), anyString(), any(), any(), any())).thenReturn(orders);

        // Act & Assert
        mockMvc.perform(get("/orders/seller/search")
                .header("X-User-Email", "seller@example.com")
                .param("q", "order1"))
                .andExpect(status().isOk());

        verify(orderService).searchSellerOrders(eq("seller@example.com"), eq("order1"), eq(null), eq(null), eq(null));
    }
}
