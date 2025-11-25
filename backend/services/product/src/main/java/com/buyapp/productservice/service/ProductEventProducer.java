package com.buyapp.productservice.service;

import com.buyapp.common.event.ProductEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ProductEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventProducer.class);

    @Autowired
    private KafkaTemplate<String, ProductEvent> kafkaTemplate;

    @Value("${kafka.topic.product-events:product-events}")
    private String productEventsTopic;

    public void sendProductEvent(ProductEvent event) {
        log.info("Sending product event: {}", event);

        CompletableFuture<SendResult<String, ProductEvent>> future = kafkaTemplate.send(productEventsTopic,
                event.getProductId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully sent product event [{}] with offset=[{}]",
                        event.getEventType(), result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send product event [{}]: {}",
                        event.getEventType(), ex.getMessage());
            }
        });
    }
}
