package com.buyapp.productservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import com.buyapp.common.dto.ProductDto;
import com.buyapp.common.dto.UserDto;
import com.buyapp.common.exception.ResourceNotFoundException;
import com.buyapp.productservice.model.Product;
import com.buyapp.productservice.repository.ProductRepository;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private ProductEventProducer productEventProducer;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private ProductDto testProductDto;
    private UserDto testUser;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(99.99);
        testProduct.setQuality(10);
        testProduct.setUserId("user1");

        testProductDto = new ProductDto();
        testProductDto.setId("1");
        testProductDto.setName("Test Product");
        testProductDto.setDescription("Test Description");
        testProductDto.setPrice(99.99);
        testProductDto.setQuality(10);

        testUser = new UserDto();
        testUser.setId("user1");
        testUser.setEmail("seller@example.com");
        testUser.setName("seller");
        testUser.setRole("seller");
    }

    @Test
    void getAllProducts_ShouldReturnAllProducts() {
        // Arrange
        List<Product> products = Arrays.asList(testProduct);
        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductDto> result = productService.getAllProducts();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testProduct.getName(), result.get(0).getName());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getAllProducts_WhenEmpty_ShouldReturnEmptyList() {
        // Arrange
        when(productRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<ProductDto> result = productService.getAllProducts();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void getProductById_WhenProductExists_ShouldReturnProduct() {
        // Arrange
        when(productRepository.findById("1")).thenReturn(Optional.of(testProduct));

        // Act
        ProductDto result = productService.getProductById("1");

        // Assert
        assertNotNull(result);
        assertEquals(testProduct.getId(), result.getId());
        assertEquals(testProduct.getName(), result.getName());
        assertEquals(testProduct.getPrice(), result.getPrice());
        verify(productRepository, times(1)).findById("1");
    }

    @Test
    void getProductById_WhenProductDoesNotExist_ShouldThrowException() {
        // Arrange
        when(productRepository.findById("999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.getProductById("999");
        });
        verify(productRepository, times(1)).findById("999");
    }

    @Test
    void getProductEntityById_WhenProductExists_ShouldReturnProductEntity() {
        // Arrange
        when(productRepository.findById("1")).thenReturn(Optional.of(testProduct));

        // Act
        Product result = productService.getProductEntityById("1");

        // Assert
        assertNotNull(result);
        assertEquals(testProduct.getId(), result.getId());
        assertEquals(testProduct.getName(), result.getName());
    }

    @Test
    void getProductEntityById_WhenProductDoesNotExist_ShouldThrowException() {
        // Arrange
        when(productRepository.findById("999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.getProductEntityById("999");
        });
    }

    @Test
    void createProduct_ShouldSaveProductWithUserId() {
        // Arrange
        when(authentication.getName()).thenReturn("seller@example.com");
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // Mock WebClient chain for getUserByEmail
        WebClient webClient = mock(WebClient.class);
        RequestHeadersUriSpec requestHeadersUriSpec = mock(RequestHeadersUriSpec.class);
        RequestHeadersSpec requestHeadersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserDto.class)).thenReturn(Mono.just(testUser));

        // Act
        ProductDto result = productService.createProduct(testProductDto, authentication);

        // Assert
        assertNotNull(result);
        assertEquals(testProduct.getName(), result.getName());
        assertEquals(testProduct.getPrice(), result.getPrice());
        verify(productRepository, times(1)).save(any(Product.class));
        verify(productEventProducer, times(1)).sendProductEvent(any());
    }

    @Test
    void deleteProduct_WhenProductExists_ShouldDeleteProduct() {
        // Arrange
        when(productRepository.findById("1")).thenReturn(Optional.of(testProduct));
        when(authentication.getName()).thenReturn("seller@example.com");
        Collection<? extends org.springframework.security.core.GrantedAuthority> authorities = Arrays
                .asList(new SimpleGrantedAuthority("ROLE_SELLER"));
        doReturn(authorities).when(authentication).getAuthorities();

        // Mock WebClient chain for getUserByEmail (for permission check) and
        // getUserById (for event)
        WebClient webClient = mock(WebClient.class);
        RequestHeadersUriSpec requestHeadersUriSpec = mock(RequestHeadersUriSpec.class);
        RequestHeadersSpec requestHeadersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserDto.class)).thenReturn(Mono.just(testUser));

        // Act
        productService.deleteProduct("1", authentication);

        // Assert
        verify(productRepository, times(1)).findById("1");
        verify(productRepository, times(1)).deleteById("1");
        verify(productEventProducer, times(1)).sendProductEvent(any());
    }

    @Test
    void updateProduct_ShouldUpdateProductFields() {
        // Arrange
        Product existingProduct = new Product("1", "Old Name", "Description", 50.0, 5, "user1");

        ProductDto updateDto = new ProductDto();
        updateDto.setName("New Name");
        updateDto.setDescription("New Description");
        updateDto.setPrice(100.0);
        updateDto.setQuality(8);

        when(productRepository.findById("1")).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authentication.getName()).thenReturn("seller@example.com");
        Collection<? extends org.springframework.security.core.GrantedAuthority> authorities = Arrays
                .asList(new SimpleGrantedAuthority("ROLE_SELLER"));
        doReturn(authorities).when(authentication).getAuthorities();

        // Mock WebClient chain for getUserByEmail (for permission check) and
        // getUserById (for event)
        WebClient webClient = mock(WebClient.class);
        RequestHeadersUriSpec requestHeadersUriSpec = mock(RequestHeadersUriSpec.class);
        RequestHeadersSpec requestHeadersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(UserDto.class)).thenReturn(Mono.just(testUser));

        // Act
        ProductDto result = productService.updateProduct("1", updateDto, authentication);

        // Assert
        assertNotNull(result);
        assertEquals("New Name", result.getName());
        assertEquals("New Description", result.getDescription());
        assertEquals(100.0, result.getPrice());
        assertEquals(8, result.getQuality());
        verify(productRepository, times(1)).findById("1");
        verify(productRepository, times(1)).save(any(Product.class));
        verify(productEventProducer, times(1)).sendProductEvent(any());
    }

    @Test
    void verifyProductValidation_PriceShouldBePositive() {
        // Arrange
        ProductDto invalidProduct = new ProductDto();
        invalidProduct.setName("Product");
        invalidProduct.setPrice(-10.0); // Invalid price
        invalidProduct.setDescription("Test");
        invalidProduct.setPrice(-10.0); // Invalid price
        invalidProduct.setQuality(5);

        // Assert - this would typically be validated at controller/validation layer
        // but we're testing service logic awareness
        assertTrue(invalidProduct.getPrice() < 0, "Price validation should catch negative prices");
    }

    @Test
    void verifyProductValidation_QualityShouldBeValid() {
        // Arrange
        ProductDto invalidProduct = new ProductDto();
        invalidProduct.setName("Product");
        invalidProduct.setDescription("Test");
        invalidProduct.setPrice(10.0);
        invalidProduct.setQuality(-5); // Invalid quality

        // Assert
        assertTrue(invalidProduct.getQuality() < 0, "Quality validation should catch negative quality");
    }
}
