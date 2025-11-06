package com.buyapp.ecommerce.config;

import com.buyapp.ecommerce.model.Product;
import com.buyapp.ecommerce.model.User;
import com.buyapp.ecommerce.model.Role;
import com.buyapp.ecommerce.repository.ProductRepository;
import com.buyapp.ecommerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class Initializer {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initUsers(UserRepository userRepository, ProductRepository productRepository) {
        return args -> {

            // Users
            if (userRepository.findByEmail("seller@example.com").isEmpty()) {
                userRepository.save(new User(
                        null,
                        "John Seller",
                        "seller@example.com",
                        passwordEncoder.encode("password123"),
                        Role.SELLER
                ));
            }
            if (userRepository.findByEmail("client1@example.com").isEmpty()) {
                userRepository.save(new User(
                        null,
                        "Alice Client",
                        "client1@example.com",
                        passwordEncoder.encode("password123"),
                        Role.CLIENT
                ));
            }
            if (userRepository.findByEmail("seller2@example.com").isEmpty()) {
                userRepository.save(new User(
                        null,
                        "Bob Seller",
                        "seller2@example.com",
                        passwordEncoder.encode("password123"),
                        Role.SELLER
                ));
            }

            // Products
            User seller1 = userRepository.findByEmail("seller@example.com").orElse(null);
            User seller2 = userRepository.findByEmail("seller2@example.com").orElse(null);

            if (seller1 != null && productRepository.findByUserId(seller1.getId()).isEmpty()) {
                productRepository.save(new Product(
                        null,
                        "Wireless Headphones",
                        "High-quality wireless headphones with noise cancellation",
                        99.99,
                        85,
                        seller1.getId()
                ));
                productRepository.save(new Product(
                        null,
                        "Gaming Mouse",
                        "Professional gaming mouse with RGB lighting",
                        49.99,
                        92,
                        seller1.getId()
                ));
            }
            if (seller2 != null && productRepository.findByUserId(seller2.getId()).isEmpty()) {
                productRepository.save(new Product(
                        null,
                        "Coffee Maker",
                        "Automatic coffee maker with timer function",
                        129.95,
                        88,
                        seller2.getId()
                ));
            }
        };
    }
}
