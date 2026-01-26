package com.buyapp.common.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
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

    public UserEvent(EventType eventType, String userId, String email, String role) {
        this.eventType = eventType;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.timestamp = LocalDateTime.now();
    }
}
