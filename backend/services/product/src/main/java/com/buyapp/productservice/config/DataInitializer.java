package com.buyapp.productservice.config;

import com.buyapp.productservice.model.Product;
import com.buyapp.productservice.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private static final int MAX_RETRIES = 10;
    private static final long RETRY_DELAY_MS = 2000;

    private final ProductRepository productRepository;
    private final WebClient.Builder webClientBuilder;

    public DataInitializer(ProductRepository productRepository, WebClient.Builder webClientBuilder) {
        this.productRepository = productRepository;
        this.webClientBuilder = webClientBuilder;
    }

    @Bean
    CommandLineRunner initProductDatabase() {
        return args -> {
            if (shouldSkipSeeding()) {
                return;
            }

            logger.info("Initializing product database with seed data...");
            SellerIds sellerIds = getSellerIdsWithRetry();
            
            if (!sellerIds.isComplete()) {
                logger.warn("Could not find seller users after {} attempts.", MAX_RETRIES);
                logger.warn("Products will not be seeded. Please ensure user service is running and users are created.");
                return;
            }

            String seller1Id = Objects.requireNonNull(sellerIds.seller1Id(), "Seller 1 ID cannot be null");
            String seller2Id = Objects.requireNonNull(sellerIds.seller2Id(), "Seller 2 ID cannot be null");
            createAndSaveProducts(seller1Id, seller2Id);
        };
    }

    private boolean shouldSkipSeeding() {
        if (productRepository.count() > 0) {
            logger.info("Product database already contains products. Skipping seed data.");
            return true;
        }
        return false;
    }

    private SellerIds getSellerIdsWithRetry() {
        String seller1Id = null;
        String seller2Id = null;
        
        for (int retries = 0; retries < MAX_RETRIES; retries++) {
            seller1Id = getUserIdByEmail("seller1@gmail.com");
            seller2Id = getUserIdByEmail("seller2@gmail.com");
            
            if (seller1Id != null && seller2Id != null) {
                return new SellerIds(seller1Id, seller2Id);
            }
            
            if (retries < MAX_RETRIES - 1) {
                waitBeforeRetry(retries);
            }
        }
        
        return new SellerIds(seller1Id, seller2Id);
    }

    private void waitBeforeRetry(int retries) {
        try {
            logger.debug("Waiting for users to be created... (attempt {}/{})", retries + 1, MAX_RETRIES);
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for users");
        }
    }

    private void createAndSaveProducts(String seller1Id, String seller2Id) {
        List<Product> seller1Products = createSeller1Products(seller1Id);
        List<Product> seller2Products = createSeller2Products(seller2Id);
        
        productRepository.saveAll(Objects.requireNonNull(seller1Products, "Seller 1 products list cannot be null"));
        productRepository.saveAll(Objects.requireNonNull(seller2Products, "Seller 2 products list cannot be null"));
        logger.info("Seed products created successfully!");
    }

    private List<Product> createSeller1Products(String seller1Id) {
        return Arrays.asList(
            new Product(null, "Velvet Matte Lipstick", 
                "Long-lasting matte lipstick with velvety finish. Rich color payoff that stays put all day.", 
                24.99, 50, seller1Id, "Lips"),
            new Product(null, "Rose Gold Eyeshadow Palette", 
                "12-shade eyeshadow palette featuring rose gold tones. Highly pigmented and blendable.", 
                48.99, 30, seller1Id, "Eyes"),
            new Product(null, "Volumizing Drama Mascara", 
                "Builds volume and length for dramatic lashes. Waterproof formula.", 
                19.99, 75, seller1Id, "Eyes"),
            new Product(null, "Silk Glow Blush", 
                "Silky smooth blush with natural glow finish. Buildable coverage.", 
                28.99, 40, seller1Id, "Face"),
            new Product(null, "Crystal Shine Lip Gloss", 
                "High-shine lip gloss with crystal shimmer. Non-sticky formula.", 
                16.99, 60, seller1Id, "Lips"),
            new Product(null, "Diamond Dust Highlighter", 
                "Luminous highlighter with diamond-like shimmer. Creates a dewy glow.", 
                34.99, 35, seller1Id, "Face"),
            new Product(null, "Berry Bliss Lipstick", 
                "Rich berry-toned lipstick with satin finish. Comfortable wear.", 
                22.99, 45, seller1Id, "Lips"),
            new Product(null, "Smoky Night Palette", 
                "10-shade palette perfect for creating smoky eye looks. Includes matte and shimmer.", 
                52.99, 25, seller1Id, "Eyes")
        );
    }

    private List<Product> createSeller2Products(String seller2Id) {
        return Arrays.asList(
            new Product(null, "Lengthening Lash Mascara", 
                "Lengthens and separates lashes for a natural look. Smudge-proof.", 
                18.99, 55, seller2Id, "Eyes"),
            new Product(null, "Coral Dream Blush", 
                "Vibrant coral blush with soft matte finish. Perfect for warm skin tones.", 
                26.99, 42, seller2Id, "Face"),
            new Product(null, "Nude Shimmer Gloss", 
                "Nude-toned lip gloss with subtle shimmer. Everyday essential.", 
                14.99, 70, seller2Id, "Lips"),
            new Product(null, "Golden Hour Highlighter", 
                "Warm golden highlighter for a sun-kissed glow. Buildable intensity.", 
                32.99, 38, seller2Id, "Face"),
            new Product(null, "Ruby Red Lipstick", 
                "Classic red lipstick with creamy formula. Bold and beautiful.", 
                26.99, 48, seller2Id, "Lips"),
            new Product(null, "Peach Perfect Blush", 
                "Soft peach blush with natural finish. Flatters all skin tones.", 
                24.99, 50, seller2Id, "Face"),
            new Product(null, "Sunset Glow Palette", 
                "15-shade palette inspired by sunset colors. Mix of matte and metallic.", 
                44.99, 28, seller2Id, "Eyes"),
            new Product(null, "Waterproof Wonder Mascara", 
                "Waterproof mascara that won't budge. Perfect for active days.", 
                21.99, 65, seller2Id, "Eyes")
        );
    }

    private String getUserIdByEmail(String email) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("http://user-service/users/email/{email}", email)
                    .retrieve()
                    .bodyToMono(com.buyapp.common.dto.UserDto.class)
                    .map(com.buyapp.common.dto.UserDto::getId)
                    .block();
        } catch (Exception e) {
            logger.warn("Error fetching user ID for {}: {}", email, e.getMessage());
            return null;
        }
    }

    private record SellerIds(String seller1Id, String seller2Id) {
        boolean isComplete() {
            return seller1Id != null && seller2Id != null;
        }
    }
}
