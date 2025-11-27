package com.buyapp.common.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class UserEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum EventType {
        USER_CREATED,
        USER_UPDATED,
        USER_DELETED
    }

    private EventType eventType;
    private String userId;
    private String email;
    private String role;
    private LocalDateTime timestamp;

    // Constructors
    public UserEvent() {
    }

    public UserEvent(EventType eventType, String userId, String email, String role) {
        this.eventType = eventType;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "UserEvent{" +
                "eventType=" + eventType +
                ", userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
