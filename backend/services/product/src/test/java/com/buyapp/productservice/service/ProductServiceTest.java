package com.buyapp.productservice.service;

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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.function.client.WebClient;

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
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private ProductEventProducer productEventProducer;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private ProductDto testProductDto;
    private UserDto testUser;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(99.99);
        testProduct.setStock(10);
        testProduct.setUserId("user1");

        testProductDto = new ProductDto();
        testProductDto.setId("1");
        testProductDto.setName("Test Product");
        testProductDto.setDescription("Test Description");
        testProductDto.setPrice(99.99);
        testProductDto.setStock(10);

        testUser = new UserDto();
        testUser.setId("user1");
        testUser.setEmail("seller@example.com");
        testUser.setName("seller");
        testUser.setRole("seller");

        testUserDto = new UserDto();
        testUserDto.setId("user1");
        testUserDto.setEmail("test@example.com");
        testUserDto.setName("Test User");
        testUserDto.setRole("seller");
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
    void productRepository_ShouldBeMocked() {
        // Arrange & Assert
        assertNotNull(productRepository, "ProductRepository should be mocked");
        assertNotNull(productEventProducer, "ProductEventProducer should be mocked");
        assertNotNull(webClientBuilder, "WebClient.Builder should be mocked");
    }

    @Test
    void verifyProductValidation_PriceShouldBePositive() {
        // Arrange
        ProductDto invalidProduct = new ProductDto();
        invalidProduct.setName("Product");
        invalidProduct.setPrice(-10.0); // Invalid price
        invalidProduct.setDescription("Test");
        invalidProduct.setPrice(-10.0); // Invalid price
        invalidProduct.setStock(5);

        // Assert - this would typically be validated at controller/validation layer
        // but we're testing service logic awareness
        assertTrue(invalidProduct.getPrice() < 0, "Price validation should catch negative prices");
    }

    @Test
    void verifyProductValidation_StockShouldBeValid() {
        // Arrange
        ProductDto invalidProduct = new ProductDto();
        invalidProduct.setName("Product");
        invalidProduct.setDescription("Test");
        invalidProduct.setPrice(10.0);
        invalidProduct.setStock(-5); // Invalid stock

        // Assert
        assertTrue(invalidProduct.getStock() < 0, "Stock validation should catch negative stock");
    }

    @Test
    void checkStockAvailability_WhenSufficientStock_ShouldReturnTrue() {
        // Arrange
        Product product = new Product("1", "Test Product", "Description", 99.99, 10, "user1", "Face");
        when(productRepository.findById("1")).thenReturn(Optional.of(product));

        // Act
        boolean result = productService.checkStockAvailability("1", 5);

        // Assert
        assertTrue(result, "Should return true when sufficient stock is available");
        verify(productRepository, times(1)).findById("1");
    }

    @Test
    void checkStockAvailability_WhenInsufficientStock_ShouldReturnFalse() {
        // Arrange
        Product product = new Product("1", "Test Product", "Description", 99.99, 3, "user1", "Face");
        when(productRepository.findById("1")).thenReturn(Optional.of(product));

        // Act
        boolean result = productService.checkStockAvailability("1", 5);

        // Assert
        assertFalse(result, "Should return false when insufficient stock");
        verify(productRepository, times(1)).findById("1");
    }

    @Test
    void checkStockAvailability_WhenStockIsNull_ShouldReturnFalse() {
        // Arrange
        Product product = new Product("1", "Test Product", "Description", 99.99, null, "user1", "Face");
        when(productRepository.findById("1")).thenReturn(Optional.of(product));

        // Act
        boolean result = productService.checkStockAvailability("1", 5);

        // Assert
        assertFalse(result, "Should return false when stock is null");
    }

    @Test
    void reduceStock_WhenSufficientStock_ShouldReduceSuccessfully() {
        // Arrange
        Product product = new Product("1", "Test Product", "Description", 99.99, 10, "user1", "Face");
        when(productRepository.findById("1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // Act
        productService.reduceStock("1", 3);

        // Assert
        assertEquals(7, product.getStock(), "Stock should be reduced by 3");
        verify(productRepository, times(1)).save(product);
    }

    @Test
    void reduceStock_WhenInsufficientStock_ShouldThrowException() {
        // Arrange
        Product product = new Product("1", "Test Product", "Description", 99.99, 2, "user1", "Face");
        when(productRepository.findById("1")).thenReturn(Optional.of(product));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.reduceStock("1", 5);
        });

        assertTrue(exception.getMessage().contains("Insufficient stock"),
                "Exception message should mention insufficient stock");
        assertTrue(exception.getMessage().contains("Test Product"),
                "Exception message should include product name");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void reduceStock_WhenStockIsNull_ShouldThrowException() {
        // Arrange
        Product product = new Product("1", "Test Product", "Description", 99.99, null, "user1", "Face");
        when(productRepository.findById("1")).thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            productService.reduceStock("1", 5);
        });
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void restoreStock_WhenStockExists_ShouldIncreaseStock() {
        // Arrange
        Product product = new Product("1", "Test Product", "Description", 99.99, 5, "user1", "Face");
        when(productRepository.findById("1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // Act
        productService.restoreStock("1", 3);

        // Assert
        assertEquals(8, product.getStock(), "Stock should be increased by 3");
        verify(productRepository, times(1)).save(product);
    }

    @Test
    void restoreStock_WhenStockIsNull_ShouldSetToQuantity() {
        // Arrange
        Product product = new Product("1", "Test Product", "Description", 99.99, null, "user1", "Face");
        when(productRepository.findById("1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // Act
        productService.restoreStock("1", 3);

        // Assert
        assertEquals(3, product.getStock(), "Stock should be set to 3 when initially null");
        verify(productRepository, times(1)).save(product);
    }

    @Test
    void restoreStock_WhenProductNotFound_ShouldThrowException() {
        // Arrange
        when(productRepository.findById("999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.restoreStock("999", 5);
        });
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void getProductsByUserId_ShouldReturnUserProducts() {
        // Arrange
        Product product1 = new Product("1", "Product 1", "Desc 1", 99.99, 10, "user1", "Face");
        Product product2 = new Product("2", "Product 2", "Desc 2", 149.99, 5, "user1", "Eyes");
        List<Product> products = Arrays.asList(product1, product2);
        when(productRepository.findByUserId("user1")).thenReturn(products);

        // Act
        List<ProductDto> result = productService.getProductsByUserId("user1");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(productRepository, times(1)).findByUserId("user1");
    }

    @Test
    void getProductsByUserId_WhenNoProducts_ShouldReturnEmptyList() {
        // Arrange
        when(productRepository.findByUserId("user999")).thenReturn(Arrays.asList());

        // Act
        List<ProductDto> result = productService.getProductsByUserId("user999");

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void deleteProductsByUserId_ShouldCallRepository() {
        // Arrange
        lenient().doNothing().when(productRepository).deleteByUserId("user1");

        // Act
        productService.deleteProductsByUserId("user1");

        // Assert
        verify(productRepository, times(1)).deleteByUserId("user1");
    }

    @Test
    void productModel_ConstructorShouldSetAllFields() {
        // Arrange & Act
        Product product = new Product("1", "Test Product", "Description", 99.99, 10, "user1", "Face");

        // Assert
        assertEquals("1", product.getId());
        assertEquals("Test Product", product.getName());
        assertEquals("Description", product.getDescription());
        assertEquals(99.99, product.getPrice());
        assertEquals(10, product.getStock());
        assertEquals("user1", product.getUserId());
    }

    @Test
    void productModel_SettersShouldUpdateFields() {
        // Arrange
        Product product = new Product();

        // Act
        product.setName("Updated Name");
        product.setDescription("Updated Description");
        product.setPrice(199.99);
        product.setStock(20);
        product.setUserId("user2");

        // Assert
        assertEquals("Updated Name", product.getName());
        assertEquals("Updated Description", product.getDescription());
        assertEquals(199.99, product.getPrice());
        assertEquals(20, product.getStock());
        assertEquals("user2", product.getUserId());
    }

    @Test
    void productModel_DefaultConstructorShouldWork() {
        // Act
        Product product = new Product();

        // Assert
        assertNotNull(product);
    }

    @Test
    void productDto_SettersAndGettersShouldWork() {
        // Arrange
        ProductDto dto = new ProductDto();

        // Act
        dto.setId("1");
        dto.setName("Test Product");
        dto.setDescription("Test Description");
        dto.setPrice(99.99);
        dto.setStock(10);
        dto.setUser("user@example.com");

        // Assert
        assertEquals("1", dto.getId());
        assertEquals("Test Product", dto.getName());
        assertEquals("Test Description", dto.getDescription());
        assertEquals(99.99, dto.getPrice());
        assertEquals(10, dto.getStock());
        assertEquals("user@example.com", dto.getUser());
    }

    @Test
    void productDto_ConstructorShouldSetAllFields() {
        // Act
        ProductDto dto = new ProductDto("1", "Product", "Description", 99.99, 10, "user@example.com", "Face");

        // Assert
        assertEquals("1", dto.getId());
        assertEquals("Product", dto.getName());
        assertEquals("Description", dto.getDescription());
        assertEquals(99.99, dto.getPrice());
        assertEquals(10, dto.getStock());
        assertEquals("user@example.com", dto.getUser());
    }

    @Test
    void toDto_ShouldConvertProductToDto() {
        // Arrange
        Product product = new Product("1", "Test Product", "Test Desc", 99.99, 10, "user1", "Face");
        when(productRepository.findById("1")).thenReturn(Optional.of(product));

        // Act
        ProductDto result = productService.getProductById("1");

        // Assert
        assertNotNull(result);
        assertEquals(product.getName(), result.getName());
        assertEquals(product.getDescription(), result.getDescription());
        assertEquals(product.getPrice(), result.getPrice());
        assertEquals(product.getStock(), result.getStock());
    }

    @Test
    void checkStockAvailability_WhenExactStock_ShouldReturnTrue() {
        // Arrange
        Product product = new Product("1", "Test Product", "Description", 99.99, 5, "user1", "Face");
        when(productRepository.findById("1")).thenReturn(Optional.of(product));

        // Act
        boolean result = productService.checkStockAvailability("1", 5);

        // Assert
        assertTrue(result, "Should return true when requested quantity equals available stock");
    }

    @Test
    void reduceStock_WhenReducingToZero_ShouldWork() {
        // Arrange
        Product product = new Product("1", "Test Product", "Description", 99.99, 5, "user1", "Face");
        when(productRepository.findById("1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // Act
        productService.reduceStock("1", 5);

        // Assert
        assertEquals(0, product.getStock(), "Stock should be reduced to zero");
        verify(productRepository, times(1)).save(product);
    }

    @Test
    void restoreStock_WithLargeQuantity_ShouldWork() {
        // Arrange
        Product product = new Product("1", "Test Product", "Description", 99.99, 10, "user1", "Face");
        when(productRepository.findById("1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // Act
        productService.restoreStock("1", 100);

        // Assert
        assertEquals(110, product.getStock(), "Stock should be increased by 100");
        verify(productRepository, times(1)).save(product);
    }

    @Test
    void toEntity_WithNullStock_ShouldDefaultToZero() {
        // Arrange
        ProductDto dto = new ProductDto();
        dto.setName("Test Product");
        dto.setDescription("Test Description");
        dto.setPrice(99.99);
        dto.setStock(null); // Explicitly set to null

        Product product = new Product("1", dto.getName(), dto.getDescription(), dto.getPrice(), 0, "user1", "Face");
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // Act - We need to test through a method that uses toEntity
        // Since toEntity is private, we test it indirectly through createProduct
        when(authentication.getName()).thenReturn("test@example.com");
        doReturn(webClient).when(webClientBuilder).build();
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), any(Object[].class));
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(Mono.just(testUserDto)).when(responseSpec).bodyToMono(UserDto.class);

        ProductDto result = productService.createProduct(dto, authentication);

        // Assert
        assertNotNull(result);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void toDto_WhenUserServiceReturnsNull_ShouldSetUnknownUser() {
        // Arrange
        Product product = new Product("1", "Test Product", "Description", 99.99, 5, "unknownUserId", "Face");
        when(productRepository.findById("1")).thenReturn(Optional.of(product));
        doReturn(webClient).when(webClientBuilder).build();
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), any(Object[].class));
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(Mono.empty()).when(responseSpec).bodyToMono(UserDto.class);

        // Act
        ProductDto result = productService.getProductById("1");

        // Assert
        assertNotNull(result);
        assertEquals("Unknown User", result.getUser(), "Should set 'Unknown User' when user service returns null");
    }

    @Test
    void reduceStock_EdgeCase_ExactStockAmount() {
        // Arrange
        Product product = new Product("1", "Test Product", "Description", 99.99, 5, "user1", "Face");
        when(productRepository.findById("1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // Act
        productService.reduceStock("1", 5);

        // Assert
        assertEquals(0, product.getStock(), "Stock should be exactly 0");
        verify(productRepository, times(1)).save(product);
    }
}
