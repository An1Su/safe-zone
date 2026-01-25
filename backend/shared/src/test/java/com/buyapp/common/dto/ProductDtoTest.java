package com.buyapp.common.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ProductDtoTest {

    @Test
    void defaultConstructor_ShouldCreateEmptyProductDto() {
        // Act
        ProductDto dto = new ProductDto();

        // Assert
        assertNotNull(dto);
        assertNull(dto.getId());
        assertNull(dto.getName());
        assertNull(dto.getDescription());
        assertNull(dto.getPrice());
        assertNull(dto.getStock());
        assertNull(dto.getUser());
    }

    @Test
    void parameterizedConstructor_ShouldSetAllFields() {
        // Arrange & Act
        ProductDto dto = new ProductDto("1", "Test Product", "Test Description", 99.99, 50, "user@test.com");

        // Assert
        assertEquals("1", dto.getId());
        assertEquals("Test Product", dto.getName());
        assertEquals("Test Description", dto.getDescription());
        assertEquals(99.99, dto.getPrice());
        assertEquals(50, dto.getStock());
        assertEquals("user@test.com", dto.getUser());
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        // Arrange
        ProductDto dto = new ProductDto();

        // Act
        dto.setId("123");
        dto.setName("New Product");
        dto.setDescription("New Description");
        dto.setPrice(149.99);
        dto.setStock(100);
        dto.setUser("seller@test.com");

        // Assert
        assertEquals("123", dto.getId());
        assertEquals("New Product", dto.getName());
        assertEquals("New Description", dto.getDescription());
        assertEquals(149.99, dto.getPrice());
        assertEquals(100, dto.getStock());
        assertEquals("seller@test.com", dto.getUser());
    }

    @Test
    void setStock_WithZero_ShouldSetZero() {
        // Arrange
        ProductDto dto = new ProductDto();

        // Act
        dto.setStock(0);

        // Assert
        assertEquals(0, dto.getStock());
    }

    @Test
    void setStock_WithNegative_ShouldStillSet() {
        // Arrange
        ProductDto dto = new ProductDto();

        // Act
        dto.setStock(-5);

        // Assert
        assertEquals(-5, dto.getStock());
    }
}
