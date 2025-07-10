package io.github.mx0100.weblog.utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Time utility class
 * Handle time operations, backend stores UTC time uniformly
 * 
 * @author mx0100
 */
public class TimeUtils {
    
    /**
     * Get current UTC time
     * 
     * @return current UTC time
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
} 