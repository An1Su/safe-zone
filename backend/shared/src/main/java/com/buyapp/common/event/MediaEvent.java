package com.buyapp.common.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MediaEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum EventType {
        IMAGE_UPLOADED,
        IMAGE_DELETED
    }
    
    private EventType eventType;
    private String mediaId;
    private String productId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String uploadedBy;
    private LocalDateTime timestamp;

    public MediaEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public MediaEvent(EventType eventType, String mediaId, String productId, String uploadedBy) {
        this.eventType = eventType;
        this.mediaId = mediaId;
        this.productId = productId;
        this.uploadedBy = uploadedBy;
        this.timestamp = LocalDateTime.now();
    }

    public MediaEvent(EventType eventType, String mediaId, String productId, String fileName,
            String contentType, Long fileSize, String uploadedBy) {
        this.eventType = eventType;
        this.mediaId = mediaId;
        this.productId = productId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
        this.timestamp = LocalDateTime.now();
    }
}
