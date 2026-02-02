package com.buyapp.mediaservice.config;

import com.buyapp.common.dto.ProductDto;
import com.buyapp.mediaservice.model.Media;
import com.buyapp.mediaservice.repository.MediaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    private final MediaRepository mediaRepository;
    private final WebClient.Builder webClientBuilder;
    private static final String UPLOAD_DIR = "uploads/images/";
    private static final int MAX_RETRIES = 10;
    private static final long RETRY_DELAY_MS = 2000;
    private static final String DEFAULT_CONTENT_TYPE = "image/jpeg";

    public DataInitializer(MediaRepository mediaRepository, WebClient.Builder webClientBuilder) {
        this.mediaRepository = mediaRepository;
        this.webClientBuilder = webClientBuilder;
    }

    @Bean
    CommandLineRunner initMediaDatabase() {
        return args -> {
            if (shouldSkipSeeding()) {
                return;
            }

            logger.info("Initializing media database with seed data...");
            List<ProductDto> products = getProductsWithRetry();
            
            if (products.isEmpty()) {
                logger.warn("Could not find products. Make sure products are created first.");
                logger.warn("Media will not be seeded.");
                return;
            }

            ensureUploadDirectoryExists();
            List<Media> mediaList = createMediaRecordsForProducts(products);
            saveMediaRecords(mediaList);
        };
    }

    private boolean shouldSkipSeeding() {
        if (mediaRepository.count() > 0) {
            logger.info("Media database already contains media. Skipping seed data.");
            return true;
        }
        return false;
    }

    private void ensureUploadDirectoryExists() {
        try {
            Path uploadDir = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
        } catch (IOException e) {
            logger.error("Error creating upload directory: {}", e.getMessage(), e);
        }
    }

    private List<Media> createMediaRecordsForProducts(List<ProductDto> products) {
        List<Media> mediaList = new ArrayList<>();
        
        for (ProductDto product : products) {
            Media media = createMediaForProduct(product);
            if (media != null) {
                mediaList.add(media);
            }
        }
        
        return mediaList;
    }

    private Media createMediaForProduct(ProductDto product) {
        if (!isValidProduct(product)) {
            return null;
        }
        
        String imageFileName = findMatchingImage(product.getName(), product.getDescription());
        if (imageFileName == null) {
            logger.debug("No matching image found for product: {}", product.getName());
            return null;
        }
        
        Path imagePath = Paths.get(UPLOAD_DIR).resolve(imageFileName);
        if (!Files.exists(imagePath)) {
            logger.warn("Image file not found: {} for product: {}", imagePath, product.getName());
            return null;
        }
        
        return buildMediaEntity(product, imageFileName, imagePath);
    }

    private boolean isValidProduct(ProductDto product) {
        if (product.getId() == null || product.getName() == null) {
            logger.debug("Skipping product with null id or name");
            return false;
        }
        return true;
    }

    private Media buildMediaEntity(ProductDto product, String imageFileName, Path imagePath) {
        Media media = new Media();
        media.setImagePath(UPLOAD_DIR + imageFileName);
        media.setProductId(product.getId());
        media.setFileName(imageFileName);
        media.setContentType(detectContentType(imageFileName));
        media.setFileSize(getFileSize(imagePath, imageFileName));
        
        logger.debug("Matched image '{}' to product '{}'", imageFileName, product.getName());
        return media;
    }

    private Long getFileSize(Path imagePath, String imageFileName) {
        try {
            return Files.size(imagePath);
        } catch (IOException e) {
            logger.warn("Error reading file size for {}: {}", imageFileName, e.getMessage(), e);
            return 0L;
        }
    }

    private void saveMediaRecords(List<Media> mediaList) {
        if (mediaList.isEmpty()) {
            logger.warn("No media was created. Check if seed images exist in {}", UPLOAD_DIR);
            return;
        }
        
        mediaRepository.saveAll(mediaList);
        logger.info("Seed media created successfully! ({} images)", mediaList.size());
    }

    private List<ProductDto> getProductsWithRetry() {
        for (int retries = 0; retries < MAX_RETRIES; retries++) {
            List<ProductDto> products = attemptFetchProducts();
            if (products != null && !products.isEmpty()) {
                return products;
            }
            
            if (retries < MAX_RETRIES - 1) {
                waitBeforeRetry(retries);
            }
        }
        return Collections.emptyList();
    }

    private List<ProductDto> attemptFetchProducts() {
        try {
            return fetchProductsFromService();
        } catch (Exception e) {
            logger.warn("Error fetching products: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<ProductDto> fetchProductsFromService() {
        return webClientBuilder.build()
                .get()
                .uri("http://product-service/products")
                .retrieve()
                .bodyToFlux(ProductDto.class)
                .collectList()
                .block();
    }

    private void waitBeforeRetry(int retries) {
        try {
            logger.debug("Waiting for products to be created... (attempt {}/{})", retries + 1, MAX_RETRIES);
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for products");
        }
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
            return DEFAULT_CONTENT_TYPE;
        }
        
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return DEFAULT_CONTENT_TYPE;
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }
        
        return DEFAULT_CONTENT_TYPE;
    }
}
