package com.buyapp.orderservice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class JwtUserDetailsServiceTest {

    @InjectMocks
    private JwtUserDetailsService jwtUserDetailsService;

    // Base64 encoded secret key (256 bits minimum for HS256)
    private static final String TEST_SECRET_KEY = "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLWp3dC10b2tlbnM=";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUserDetailsService, "secretKey", TEST_SECRET_KEY);
    }

    @Test
    void loadUserByUsername_WhenNoRequestContext_ShouldThrowException() {
        // Clear any existing request context
        RequestContextHolder.resetRequestAttributes();

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            jwtUserDetailsService.loadUserByUsername("testuser");
        });
    }

    @Test
    void loadUserByUsername_WhenNoAuthorizationHeader_ShouldThrowException() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            jwtUserDetailsService.loadUserByUsername("testuser");
        });

        // Cleanup
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void loadUserByUsername_WhenInvalidToken_ShouldThrowException() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            jwtUserDetailsService.loadUserByUsername("testuser");
        });

        // Cleanup
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void serviceShouldBeInstantiated() {
        assertNotNull(jwtUserDetailsService, "JwtUserDetailsService should be instantiated");
    }
}

