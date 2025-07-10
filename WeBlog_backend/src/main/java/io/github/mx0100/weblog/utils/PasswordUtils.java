package io.github.mx0100.weblog.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Password utility class
 * 
 * @author mx0100
 */
@Component
public class PasswordUtils {
    
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    /**
     * Encode password
     * 
     * @param rawPassword raw password
     * @return encoded password
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return passwordEncoder.encode(rawPassword);
    }
    
    /**
     * Verify password
     * 
     * @param rawPassword raw password
     * @param encodedPassword encoded password
     * @return true if matches
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
} 