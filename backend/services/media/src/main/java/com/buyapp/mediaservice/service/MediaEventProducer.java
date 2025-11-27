package com.buyapp.mediaservice.service;

import com.buyapp.common.event.MediaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class MediaEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(MediaEventProducer.class);

    private final KafkaTemplate<String, MediaEvent> kafkaTemplate;

    @Value("${kafka.topic.media-events:media-events}")
    private String mediaEventsTopic;

    public MediaEventProducer(KafkaTemplate<String, MediaEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMediaEvent(MediaEvent event) {
        logger.info("Sending media event: {}", event);

        CompletableFuture<SendResult<String, MediaEvent>> future = kafkaTemplate.send(mediaEventsTopic,
                event.getMediaId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Successfully sent media event [{}] with offset=[{}]",
                        event.getEventType(), result.getRecordMetadata().offset());
            } else {
                logger.error("Failed to send media event [{}]: {}",
                        event.getEventType(), ex.getMessage());
            }
        });
    }
}
