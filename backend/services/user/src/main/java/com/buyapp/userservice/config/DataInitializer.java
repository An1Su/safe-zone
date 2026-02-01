package com.buyapp.userservice.config;

import com.buyapp.userservice.model.Role;
import com.buyapp.userservice.model.User;
import com.buyapp.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {
            // Only seed if database is empty
            if (userRepository.count() == 0) {
                System.out.println("Initializing database with seed data...");

                String password = passwordEncoder.encode("12345");

                List<User> users = Arrays.asList(
                    new User(null, "Seller One", "seller1@gmail.com", password, Role.SELLER, null),
                    new User(null, "Seller Two", "seller2@gmail.com", password, Role.SELLER, null),
                    new User(null, "Buyer One", "buyer1@gmail.com", password, Role.CLIENT, null),
                    new User(null, "Buyer Two", "buyer2@gmail.com", password, Role.CLIENT, null)
                );

                userRepository.saveAll(users);
                System.out.println("Seed users created successfully!");
            } else {
                System.out.println("Database already contains users. Skipping seed data.");
            }
        };
    }
}
