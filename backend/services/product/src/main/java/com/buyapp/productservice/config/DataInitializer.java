package com.buyapp.productservice.config;

import com.buyapp.productservice.model.Product;
import com.buyapp.productservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Bean
    CommandLineRunner initProductDatabase() {
        return args -> {
            // Only seed if database is empty
            if (productRepository.count() == 0) {
                System.out.println("Initializing product database with seed data...");

                // Wait for user service to be ready and users to be created
                // Retry up to 10 times with 2 second delays
                String seller1Id = null;
                String seller2Id = null;
                int retries = 0;
                int maxRetries = 10;

                while ((seller1Id == null || seller2Id == null) && retries < maxRetries) {
                    try {
                        seller1Id = getUserIdByEmail("seller1@gmail.com");
                        seller2Id = getUserIdByEmail("seller2@gmail.com");
                        
                        if (seller1Id == null || seller2Id == null) {
                            retries++;
                            System.out.println("Waiting for users to be created... (attempt " + retries + "/" + maxRetries + ")");
                            Thread.sleep(2000); // Wait 2 seconds before retry
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                if (seller1Id == null || seller2Id == null) {
                    System.out.println("Warning: Could not find seller users after " + maxRetries + " attempts.");
                    System.out.println("Products will not be seeded. Please ensure user service is running and users are created.");
                    return;
                }

                // Seller 1 products
                List<Product> seller1Products = Arrays.asList(
                    new Product(null, "Velvet Matte Lipstick", 
                        "Long-lasting matte lipstick with velvety finish. Rich color payoff that stays put all day.", 
                        24.99, 50, seller1Id),
                    new Product(null, "Rose Gold Eyeshadow Palette", 
                        "12-shade eyeshadow palette featuring rose gold tones. Highly pigmented and blendable.", 
                        48.99, 30, seller1Id),
                    new Product(null, "Volumizing Drama Mascara", 
                        "Builds volume and length for dramatic lashes. Waterproof formula.", 
                        19.99, 75, seller1Id),
                    new Product(null, "Silk Glow Blush", 
                        "Silky smooth blush with natural glow finish. Buildable coverage.", 
                        28.99, 40, seller1Id),
                    new Product(null, "Crystal Shine Lip Gloss", 
                        "High-shine lip gloss with crystal shimmer. Non-sticky formula.", 
                        16.99, 60, seller1Id),
                    new Product(null, "Diamond Dust Highlighter", 
                        "Luminous highlighter with diamond-like shimmer. Creates a dewy glow.", 
                        34.99, 35, seller1Id),
                    new Product(null, "Berry Bliss Lipstick", 
                        "Rich berry-toned lipstick with satin finish. Comfortable wear.", 
                        22.99, 45, seller1Id),
                    new Product(null, "Smoky Night Palette", 
                        "10-shade palette perfect for creating smoky eye looks. Includes matte and shimmer.", 
                        52.99, 25, seller1Id)
                );

                // Seller 2 products
                List<Product> seller2Products = Arrays.asList(
                    new Product(null, "Lengthening Lash Mascara", 
                        "Lengthens and separates lashes for a natural look. Smudge-proof.", 
                        18.99, 55, seller2Id),
                    new Product(null, "Coral Dream Blush", 
                        "Vibrant coral blush with soft matte finish. Perfect for warm skin tones.", 
                        26.99, 42, seller2Id),
                    new Product(null, "Nude Shimmer Gloss", 
                        "Nude-toned lip gloss with subtle shimmer. Everyday essential.", 
                        14.99, 70, seller2Id),
                    new Product(null, "Golden Hour Highlighter", 
                        "Warm golden highlighter for a sun-kissed glow. Buildable intensity.", 
                        32.99, 38, seller2Id),
                    new Product(null, "Ruby Red Lipstick", 
                        "Classic red lipstick with creamy formula. Bold and beautiful.", 
                        26.99, 48, seller2Id),
                    new Product(null, "Peach Perfect Blush", 
                        "Soft peach blush with natural finish. Flatters all skin tones.", 
                        24.99, 50, seller2Id),
                    new Product(null, "Sunset Glow Palette", 
                        "15-shade palette inspired by sunset colors. Mix of matte and metallic.", 
                        44.99, 28, seller2Id),
                    new Product(null, "Waterproof Wonder Mascara", 
                        "Waterproof mascara that won't budge. Perfect for active days.", 
                        21.99, 65, seller2Id)
                );

                productRepository.saveAll(seller1Products);
                productRepository.saveAll(seller2Products);
                System.out.println("Seed products created successfully!");
            } else {
                System.out.println("Product database already contains products. Skipping seed data.");
            }
        };
    }

    private String getUserIdByEmail(String email) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("http://user-service/users/email/{email}", email)
                    .retrieve()
                    .bodyToMono(com.buyapp.common.dto.UserDto.class)
                    .map(user -> user.getId())
                    .block();
        } catch (Exception e) {
            System.out.println("Error fetching user ID for " + email + ": " + e.getMessage());
            return null;
        }
    }
}
