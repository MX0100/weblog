package io.github.mx0100.weblog.utils;

import io.github.mx0100.weblog.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * JWT utility class
 * 
 * @author mx0100
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {
    
    private final JwtConfig jwtConfig;
    
    /**
     * Generate JWT token
     * 
     * @param userId user ID
     * @param username username
     * @return JWT token
     */
    public String generateToken(Long userId, String username) {
        LocalDateTime now = TimeUtils.nowUtc();
        LocalDateTime expiration = now.plusHours(jwtConfig.getExpiration());
        
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .issuedAt(Date.from(now.toInstant(ZoneOffset.UTC)))
                .expiration(Date.from(expiration.toInstant(ZoneOffset.UTC)))
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * Get user ID from token
     * 
     * @param token JWT token
     * @return user ID
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            log.error("Failed to get user ID from token", e);
            return null;
        }
    }
    
    /**
     * Get username from token
     * 
     * @param token JWT token
     * @return username
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("username", String.class);
        } catch (Exception e) {
            log.error("Failed to get username from token", e);
            return null;
        }
    }
    
    /**
     * Validate JWT token
     * 
     * @param token JWT token
     * @return true if valid
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT token is malformed: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT token compact is empty: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT token validation failed", e);
        }
        return false;
    }
    
    /**
     * Parse JWT token
     * 
     * @param token JWT token
     * @return claims
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * Get signing key
     * 
     * @return signing key
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
    }
} 