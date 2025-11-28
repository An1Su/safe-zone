package com.buyapp.productservice.listener;

import com.buyapp.common.event.UserEvent;
import com.buyapp.productservice.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener {

    private static final Logger logger = LoggerFactory.getLogger(UserEventListener.class);

    private final ProductService productService;

    public UserEventListener(ProductService productService) {
        this.productService = productService;
    }

    @KafkaListener(topics = "${kafka.topic.user-events}", groupId = "product-service-group", containerFactory = "userEventKafkaListenerContainerFactory")
    public void handleUserEvent(UserEvent event) {
        logger.info("Received user event: {}", event);

        if (event.getEventType() == UserEvent.EventType.USER_DELETED) {
            logger.info("Processing USER_DELETED event for seller: {}", event.getUserId());

            try {
                // Delete all products for this seller
                productService.deleteProductsByUserId(event.getUserId());
                logger.info("Successfully deleted all products for seller: {}", event.getUserId());
            } catch (Exception e) {
                logger.error("Error deleting products for seller: {}", event.getUserId(), e);
            }
        }
    }
}
