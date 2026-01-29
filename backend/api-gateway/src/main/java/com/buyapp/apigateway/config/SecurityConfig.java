package com.buyapp.apigateway.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import com.buyapp.apigateway.filter.JwtRequestFilter;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String PRODUCTS_PATH = "/products/**";
    private static final String MEDIA_PATH = "/media/**";
    private static final String ROLE_SELLER = "SELLER";
    private static final String ROLE_CLIENT = "CLIENT";

    private final JwtRequestFilter jwtRequestFilter;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods}")
    private String allowedMethods;

    @Value("${cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials}")
    private boolean allowCredentials;

    @Autowired
    public SecurityConfig(JwtRequestFilter jwtRequestFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchange -> exchange
                        // Public endpoints
                        .pathMatchers("/actuator/health").permitAll()
                        .pathMatchers("/auth/**").permitAll()
                        .pathMatchers(HttpMethod.GET, PRODUCTS_PATH).permitAll()
                        .pathMatchers(HttpMethod.GET, MEDIA_PATH).permitAll()

                        // Protected endpoints - user profile
                        .pathMatchers("/users/me").hasAnyRole(ROLE_CLIENT, ROLE_SELLER)

                        // Protected endpoints - seller only
                        .pathMatchers(HttpMethod.POST, PRODUCTS_PATH).hasRole(ROLE_SELLER)
                        .pathMatchers(HttpMethod.PUT, PRODUCTS_PATH).hasRole(ROLE_SELLER)
                        .pathMatchers(HttpMethod.DELETE, PRODUCTS_PATH).hasRole(ROLE_SELLER)
                        .pathMatchers(HttpMethod.POST, MEDIA_PATH).hasRole(ROLE_SELLER)
                        .pathMatchers(HttpMethod.DELETE, MEDIA_PATH).hasRole(ROLE_SELLER)

                        // All other endpoints require authentication
                        .anyExchange().authenticated())
                .addFilterAt(jwtRequestFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        configuration.setAllowCredentials(allowCredentials);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
