package com.buyapp.productservice.service;

import com.buyapp.common.dto.ProductDto;
import com.buyapp.common.dto.UserDto;
import com.buyapp.common.event.ProductEvent;
import com.buyapp.common.exception.ForbiddenException;
import com.buyapp.common.exception.ResourceNotFoundException;
import com.buyapp.productservice.model.Product;
import com.buyapp.productservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final String PRODUCT_NOT_FOUND = "Product not found with this id:";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private ProductEventProducer productEventProducer;

    public List<ProductDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ProductDto getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND + id));
        return toDto(product);
    }

    public Product getProductEntityById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND + id));
    }

    public ProductDto createProduct(ProductDto productDto, Authentication authentication) {
        String userEmail = authentication.getName();

        // Call User Service to get user information
        UserDto user = getUserByEmail(userEmail);
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user not found in user service");
        }

        Product product = toEntity(productDto);
        product.setUserId(user.getId()); // Store user ID internally
        Product saved = productRepository.save(product);

        // Publish PRODUCT_CREATED event
        ProductEvent event = new ProductEvent(
                ProductEvent.EventType.PRODUCT_CREATED,
                saved.getId(),
                saved.getName(),
                user.getId(),
                user.getEmail());
        productEventProducer.sendProductEvent(event);

        return toDto(saved);
    }

    public ProductDto updateProduct(String id, ProductDto productDto, Authentication authentication) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND + id));

        if (!canModifyProduct(existing, authentication)) {
            throw new ForbiddenException("You don't have permission to modify this product");
        }

        existing.setName(productDto.getName());
        existing.setDescription(productDto.getDescription());
        existing.setPrice(productDto.getPrice());
        if (productDto.getStock() != null) {
            existing.setStock(productDto.getStock());
        }
        existing.setCategory(productDto.getCategory());

        Product updated = productRepository.save(existing);

        // Publish PRODUCT_UPDATED event
        UserDto user = getUserById(updated.getUserId());
        if (user == null) {
            throw new IllegalArgumentException(
                    "User not found for updated product with userId: " + updated.getUserId());
        }
        ProductEvent event = new ProductEvent(
                ProductEvent.EventType.PRODUCT_UPDATED,
                updated.getId(),
                updated.getName(),
                user.getId(),
                user.getEmail());
        productEventProducer.sendProductEvent(event);

        return toDto(updated);
    }

    public void deleteProduct(String id, Authentication authentication) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND + id));

        if (!canModifyProduct(existing, authentication)) {
            throw new ForbiddenException("You don't have permission to modify this product");
        }

        // Publish PRODUCT_DELETED event before deletion
        UserDto user = getUserById(existing.getUserId());
        if (user == null) {
            throw new IllegalStateException(
                    "User not found with id: " + existing.getUserId() + " when deleting product: " + id);
        }
        ProductEvent event = new ProductEvent(
                ProductEvent.EventType.PRODUCT_DELETED,
                existing.getId(),
                user.getId(),
                user.getEmail());
        productEventProducer.sendProductEvent(event);

        productRepository.deleteById(id);
    }

    public List<ProductDto> getProductsByUser(String userEmail) {
        // Call User Service to get user information
        UserDto user = getUserByEmail(userEmail);
        if (user == null) {
            throw new IllegalArgumentException("User not found with email: " + userEmail);
        }

        return productRepository.findByUserId(user.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ProductDto> getProductsByUserId(String userId) {
        return productRepository.findByUserId(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void deleteProductsByUserId(String userId) {
        productRepository.deleteByUserId(userId);
    }

    public boolean checkStockAvailability(String productId, Integer requestedQuantity) {
        Product product = getProductEntityById(productId);
        return product.getStock() != null && product.getStock() >= requestedQuantity;
    }

    public void reduceStock(String productId, Integer quantity) {
        Product product = getProductEntityById(productId);
        if (product.getStock() == null || product.getStock() < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock for product: " + product.getName() + 
                    ". Available: " + product.getStock() + ", Requested: " + quantity);
        }
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
    }

    public void restoreStock(String productId, Integer quantity) {
        Product product = getProductEntityById(productId);
        product.setStock((product.getStock() != null ? product.getStock() : 0) + quantity);
        productRepository.save(product);
    }

    public String getProductSellerId(String productId) {
        Product product = getProductEntityById(productId);
        return product.getUserId();
    }

    private boolean canModifyProduct(Product product, Authentication authentication) {
        String currentUserEmail = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(ROLE_ADMIN));

        if (isAdmin) {
            return true;
        }

        // Get current user's ID via User Service to compare with product's userId
        UserDto currentUser = getUserByEmail(currentUserEmail);
        if (currentUser == null) {
            throw new IllegalArgumentException("Authenticated user not found in user service");
        }

        return product.getUserId().equals(currentUser.getId());
    }

    private UserDto getUserByEmail(String email) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("http://user-service/users/email/{email}", email)
                    .retrieve()
                    .bodyToMono(UserDto.class)
                    .block();
        } catch (Exception e) {
            return null;
        }
    }

    private UserDto getUserById(String userId) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("http://user-service/users/{id}", userId)
                    .retrieve()
                    .bodyToMono(UserDto.class)
                    .block();
        } catch (Exception e) {
            return null;
        }
    }

    private ProductDto toDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        dto.setCategory(product.getCategory());

        // Convert userId to email for display via User Service call
        UserDto user = getUserById(product.getUserId());
        dto.setUser(user != null ? user.getEmail() : "Unknown User");

        return dto;
    }

    private Product toEntity(ProductDto dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock() != null ? dto.getStock() : 0);
        product.setCategory(dto.getCategory());
        // Note: userId should be set separately in service methods, not from DTO
        return product;
    }
}