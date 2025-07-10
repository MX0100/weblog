package io.github.mx0100.weblog.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response status code enumeration
 * 
 * @author mx0100
 */
@Getter
@AllArgsConstructor
public enum ResponseCode {
    
    // Success
    SUCCESS(200, "Success"),
    
    // Client errors
    BAD_REQUEST(400, "Bad request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Resource not found"),
    CONFLICT(409, "Data conflict"),
    
    // Server errors
    INTERNAL_ERROR(500, "Internal server error"),
    
    // Business errors
    USER_NOT_FOUND(1001, "User not found"),
    USERNAME_EXISTS(1002, "Username already exists"),
    INVALID_PASSWORD(1003, "Invalid password"),
    TOKEN_EXPIRED(1004, "Token expired"),
    POST_NOT_FOUND(1005, "Post not found"),
    COMMENT_NOT_FOUND(1006, "Comment not found"),
    PERMISSION_DENIED(1007, "Permission denied");
    
    private final Integer code;
    private final String message;
} 