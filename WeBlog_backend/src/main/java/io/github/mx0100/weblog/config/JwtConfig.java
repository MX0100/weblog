package io.github.mx0100.weblog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT configuration properties
 * 
 * @author mx0100
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    
    /**
     * JWT secret key (256-bit secure key)
     */
    private String secret = "weblog-super-secure-jwt-secret-key-2024-for-production-use-only-do-not-share";
    
    /**
     * JWT token expiration time in hours
     */
    private Long expiration = 24L;
    
    /**
     * JWT token prefix
     */
    private String tokenPrefix = "Bearer ";
    
    /**
     * JWT token header name
     */
    private String headerName = "Authorization";
} 