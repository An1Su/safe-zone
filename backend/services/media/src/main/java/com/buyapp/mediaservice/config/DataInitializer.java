package com.buyapp.mediaservice.config;

import com.buyapp.common.dto.ProductDto;
import com.buyapp.mediaservice.model.Media;
import com.buyapp.mediaservice.repository.MediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataInitializer {

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    private static final String UPLOAD_DIR = "uploads/images/";

    @Bean
    CommandLineRunner initMediaDatabase() {
        return args -> {
            // Only seed if database is empty
            if (mediaRepository.count() == 0) {
                System.out.println("Initializing media database with seed data...");

                // Wait for product service to be ready and products to be created
                List<ProductDto> products = getProductsWithRetry();
                
                if (products == null || products.isEmpty()) {
                    System.out.println("Warning: Could not find products. Make sure products are created first.");
                    System.out.println("Media will not be seeded.");
                    return;
                }

                // Create upload directory if it doesn't exist
                Path uploadDir = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                // Match products to images and create media records
                List<Media> mediaList = new ArrayList<>();
                
                for (ProductDto product : products) {
                    if (product.getId() == null || product.getName() == null) {
                        System.out.println("Skipping product with null id or name");
                        continue;
                    }
                    
                    // Find matching image based on product name/description keywords
                    String imageFileName = findMatchingImage(product.getName(), product.getDescription());
                    
                    if (imageFileName != null) {
                        Path imagePath = Paths.get(UPLOAD_DIR).resolve(imageFileName);
                        
                        if (!Files.exists(imagePath)) {
                            System.out.println("Image file not found: " + imagePath + " for product: " + product.getName());
                            continue;
                        }
                        
                        Media media = new Media();
                        media.setImagePath(UPLOAD_DIR + imageFileName);
                        media.setProductId(product.getId());
                        media.setFileName(imageFileName);
                        
                        // Detect content type from file extension
                        media.setContentType(detectContentType(imageFileName));
                        
                        // Get actual file size
                        try {
                            media.setFileSize(Files.size(imagePath));
                        } catch (IOException e) {
                            System.out.println("Error reading file size for " + imageFileName + ": " + e.getMessage());
                            media.setFileSize(0L);
                        }
                        
                        mediaList.add(media);
                        System.out.println("Matched image '" + imageFileName + "' to product '" + product.getName() + "'");
                    } else {
                        System.out.println("No matching image found for product: " + product.getName());
                    }
                }

                if (!mediaList.isEmpty()) {
                    mediaRepository.saveAll(mediaList);
                    System.out.println("Seed media created successfully! (" + mediaList.size() + " images)");
                } else {
                    System.out.println("No media was created. Check if seed images exist in " + UPLOAD_DIR);
                }
            } else {
                System.out.println("Media database already contains media. Skipping seed data.");
            }
        };
    }

    private List<ProductDto> getProductsWithRetry() {
        int retries = 0;
        int maxRetries = 10;
        long retryDelayMs = 2000;

        while (retries < maxRetries) {
            try {
                List<ProductDto> products = webClientBuilder.build()
                        .get()
                        .uri("http://product-service/products")
                        .retrieve()
                        .bodyToFlux(ProductDto.class)
                        .collectList()
                        .block();

                if (products != null && !products.isEmpty()) {
                    return products;
                }

                retries++;
                if (retries < maxRetries) {
                    System.out.println("Waiting for products to be created... (attempt " + retries + "/" + maxRetries + ")");
                    Thread.sleep(retryDelayMs);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Interrupted while waiting for products");
                return null;
            } catch (Exception e) {
                retries++;
                if (retries >= maxRetries) {
                    System.out.println("Error fetching products after " + maxRetries + " attempts: " + e.getMessage());
                    return null;
                }
                try {
                    System.out.println("Error fetching products (attempt " + retries + "/" + maxRetries + "): " + e.getMessage());
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * Finds matching image file based on product name and description keywords.
     * Returns the image filename if found, null otherwise.
     */
    private String findMatchingImage(String productName, String description) {
        if (productName == null) {
            return null;
        }
        
        String searchText = (productName + " " + (description != null ? description : "")).toLowerCase();
        Path uploadPath = Paths.get(UPLOAD_DIR);
        
        // Match keywords to images (order matters - more specific first)
        if (searchText.contains("blush")) {
            return checkImageExists(uploadPath, "product-blush.jpg");
        } else if (searchText.contains("palette") || searchText.contains("eyeshadow")) {
            return checkImageExists(uploadPath, "product-eyeshadow.jpg");
        } else if (searchText.contains("gloss")) {
            return checkImageExists(uploadPath, "product-gloss.jpg");
        } else if (searchText.contains("highlighter")) {
            return checkImageExists(uploadPath, "product-highlighter.jpg");
        } else if (searchText.contains("lipstick")) {
            return checkImageExists(uploadPath, "product-lipstick.jpg");
        } else if (searchText.contains("mascara")) {
            return checkImageExists(uploadPath, "product-mascara.jpg");
        }
        
        return null;
    }
    
    /**
     * Checks if image file exists and returns its name, or null if not found.
     */
    private String checkImageExists(Path uploadPath, String fileName) {
        Path imagePath = uploadPath.resolve(fileName);
        if (Files.exists(imagePath) && Files.isRegularFile(imagePath)) {
            return fileName;
        }
        return null;
    }
    
    /**
     * Detects content type based on file extension.
     */
    private String detectContentType(String fileName) {
        if (fileName == null) {
            return "image/jpeg"; // Default
        }
        
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }
        
        return "image/jpeg"; // Default fallback
    }
}
