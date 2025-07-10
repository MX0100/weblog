package io.github.mx0100.weblog.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mx0100.weblog.dto.RichContent;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JPA converter for RichContent to JSONb conversion
 * 
 * @author mx0100
 */
@Slf4j
@Component
@Converter(autoApply = false)
public class RichContentConverter implements AttributeConverter<RichContent, String> {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String convertToDatabaseColumn(RichContent attribute) {
        if (attribute == null) {
            return null;
        }
        
        try {
            String json = objectMapper.writeValueAsString(attribute);
            log.debug("Converting RichContent to JSON: {}", json);
            return json;
        } catch (JsonProcessingException e) {
            log.error("Error converting RichContent to JSON: {}", e.getMessage(), e);
            // Fallback to plain text representation
            return convertPlainTextFallback(attribute);
        }
    }
    
    @Override
    public RichContent convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return RichContent.createPlainText("");
        }
        
        try {
            // Try to parse as JSON first
            if (dbData.trim().startsWith("{")) {
                RichContent content = objectMapper.readValue(dbData, RichContent.class);
                log.debug("Converted JSON to RichContent: {}", content);
                
                // Validate and fix if necessary
                if (!content.isValid()) {
                    log.warn("Invalid RichContent structure, converting to plain text: {}", dbData);
                    return RichContent.createPlainText(content.getPlainText());
                }
                
                return content;
            } else {
                // Treat as legacy plain text
                log.debug("Converting legacy plain text to RichContent: {}", dbData);
                return RichContent.createPlainText(dbData);
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON to RichContent, treating as plain text: {}", e.getMessage());
            // Fallback: treat as plain text
            return RichContent.createPlainText(dbData);
        }
    }
    
    /**
     * Fallback method to convert RichContent to plain text JSON when serialization fails
     */
    private String convertPlainTextFallback(RichContent content) {
        try {
            RichContent fallback = RichContent.createPlainText(content.getDisplayText());
            return objectMapper.writeValueAsString(fallback);
        } catch (JsonProcessingException e) {
            log.error("Even fallback conversion failed: {}", e.getMessage(), e);
            // Last resort: return a minimal JSON structure
            return String.format("{\"type\":\"plain_text\",\"version\":\"1.0\",\"plainText\":\"%s\"}", 
                                content.getDisplayText().replace("\"", "\\\""));
        }
    }
} 