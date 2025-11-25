package com.buyapp.common.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class MediaEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventType; // IMAGE_UPLOADED, IMAGE_DELETED
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

    public MediaEvent(String eventType, String mediaId, String productId, String uploadedBy) {
        this.eventType = eventType;
        this.mediaId = mediaId;
        this.productId = productId;
        this.uploadedBy = uploadedBy;
        this.timestamp = LocalDateTime.now();
    }

    public MediaEvent(String eventType, String mediaId, String productId, String fileName,
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

    // Getters and Setters
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "MediaEvent{" +
                "eventType='" + eventType + '\'' +
                ", mediaId='" + mediaId + '\'' +
                ", productId='" + productId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", contentType='" + contentType + '\'' +
                ", fileSize=" + fileSize +
                ", uploadedBy='" + uploadedBy + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
