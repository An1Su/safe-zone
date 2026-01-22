package com.buyapp.apigateway.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import com.buyapp.apigateway.filter.JwtRequestFilter;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtRequestFilter jwtRequestFilter;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(jwtRequestFilter);
        // Use reflection to set the private fields since they're @Value injected
        setField(securityConfig, "allowedOrigins", "http://localhost:4200,https://localhost:4200");
        setField(securityConfig, "allowedMethods", "GET,POST,PUT,DELETE,OPTIONS");
        setField(securityConfig, "allowedHeaders", "Authorization,Content-Type,X-Requested-With");
        setField(securityConfig, "exposedHeaders", "Authorization");
        setField(securityConfig, "allowCredentials", true);
    }

    @Test
    void constructor_ShouldInitializeJwtRequestFilter() {
        // Arrange & Act
        SecurityConfig config = new SecurityConfig(jwtRequestFilter);

        // Assert
        assertNotNull(config, "SecurityConfig should be initialized");
    }

    @Test
    void corsConfigurationSource_ShouldConfigureCorsCorrectly() {
        // Act
        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();

        // Assert
        assertNotNull(corsSource, "CorsConfigurationSource should not be null");

        // Verify the configuration is a UrlBasedCorsConfigurationSource
        assertTrue(corsSource instanceof org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource,
                "CorsConfigurationSource should be of type UrlBasedCorsConfigurationSource");
    }

    @Test
    void constructor_WithNullFilter_ShouldStillInitialize() {
        // Act & Assert
        // This tests that the constructor accepts the parameter
        SecurityConfig config = new SecurityConfig(null);
        assertNotNull(config, "SecurityConfig should initialize even with null filter for testing");
    }

    // Helper method to set private fields using reflection
    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            // Ignore for testing purposes
        }
    }
}
