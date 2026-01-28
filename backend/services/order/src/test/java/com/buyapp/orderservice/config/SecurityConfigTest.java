package com.buyapp.orderservice.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buyapp.common.security.JwtAuthenticationFilter;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Test
    void securityConfigShouldBeInstantiated() {
        assertNotNull(securityConfig, "SecurityConfig should be instantiated");
    }

    @Test
    void jwtAuthenticationFilterShouldBeInjected() {
        assertNotNull(jwtAuthenticationFilter, "JwtAuthenticationFilter should be mocked");
    }
}

