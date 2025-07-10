package io.github.mx0100.weblog.dto.response;

import lombok.Data;

/**
 * Login response DTO
 * 
 * @author mx0100
 */
@Data
public class LoginResponse {
    
    private String token;
    private UserResponse user;
} 