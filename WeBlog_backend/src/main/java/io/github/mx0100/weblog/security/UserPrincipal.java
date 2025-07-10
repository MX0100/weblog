package io.github.mx0100.weblog.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * User principal for Spring Security context
 * Contains user information from JWT token
 * 
 * @author mx0100
 */
@Data
@AllArgsConstructor
public class UserPrincipal {
    
    private Long userId;
    private String username;
} 