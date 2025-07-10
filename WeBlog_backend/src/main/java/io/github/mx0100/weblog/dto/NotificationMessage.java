package io.github.mx0100.weblog.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket notification message DTO
 * 
 * @author mx0100
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    
    /**
     * Notification type
     */
    private String type;
    
    /**
     * User ID who triggered the notification
     */
    private Long fromUserId;
    
    /**
     * User ID who should receive the notification
     */
    private Long toUserId;
    
    /**
     * Human-readable notification message
     */
    private String message;
    
    /**
     * Additional data related to the notification
     */
    private Object data;
    
    /**
     * Timestamp when the notification was created
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * Notification types
     */
    public static class Type {
        public static final String PAIR_REQUEST = "PAIR_REQUEST";
        public static final String PAIR_REQUEST_ACCEPTED = "PAIR_REQUEST_ACCEPTED";
        public static final String PAIR_REQUEST_REJECTED = "PAIR_REQUEST_REJECTED";
        public static final String NEW_POST = "NEW_POST";
        public static final String POST_UPDATED = "POST_UPDATED";
        public static final String NEW_COMMENT = "NEW_COMMENT";
        public static final String POST_DELETED = "POST_DELETED";
        public static final String COMMENT_DELETED = "COMMENT_DELETED";
        public static final String RELATIONSHIP_ENDED = "RELATIONSHIP_ENDED";
    }
} 