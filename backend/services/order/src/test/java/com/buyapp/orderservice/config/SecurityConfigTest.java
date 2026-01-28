package com.buyapp.orderservice.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

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

