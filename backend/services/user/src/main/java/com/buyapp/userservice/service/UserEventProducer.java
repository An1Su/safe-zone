package com.buyapp.userservice.service;

import com.buyapp.common.event.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class UserEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(UserEventProducer.class);

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    @Value("${spring.kafka.topic.user-events}")
    private String userEventsTopic;

    public UserEventProducer(KafkaTemplate<String, UserEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendUserEvent(UserEvent event) {
        logger.info("Sending user event: {}", event);

        CompletableFuture<SendResult<String, UserEvent>> future = kafkaTemplate.send(userEventsTopic, event.getUserId(),
                event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Successfully sent user event: {} with offset: {}",
                        event.getEventType(), result.getRecordMetadata().offset());
            } else {
                logger.error("Failed to send user event: {}", event.getEventType(), ex);
            }
        });
    }
}
