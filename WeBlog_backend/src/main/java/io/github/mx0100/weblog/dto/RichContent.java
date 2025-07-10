package io.github.mx0100.weblog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Rich text content data structure
 * Supports both plain text and rich text formats
 * 
 * @author mx0100
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RichContent {
    
    /**
     * Content type: "plain_text" or "rich_text"
     */
    private String type;
    
    /**
     * Format version for future compatibility
     */
    private String version;
    
    /**
     * Rich text content in Delta format (for Quill.js)
     * Only present when type is "rich_text"
     */
    private Map<String, Object> delta;
    
    /**
     * Plain text representation
     * Always present for backward compatibility and search indexing
     */
    private String plainText;
    
    /**
     * Create a plain text content
     */
    public static RichContent createPlainText(String text) {
        return RichContent.builder()
                .type("plain_text")
                .version("1.0")
                .plainText(text != null ? text : "")
                .build();
    }
    
    /**
     * Create a rich text content
     */
    public static RichContent createRichText(Map<String, Object> delta, String plainText) {
        return RichContent.builder()
                .type("rich_text")
                .version("1.0")
                .delta(delta)
                .plainText(plainText != null ? plainText : "")
                .build();
    }
    
    /**
     * Check if this is rich text content
     */
    public boolean isRichText() {
        return "rich_text".equals(type) && delta != null;
    }
    
    /**
     * Check if this is plain text content
     */
    public boolean isPlainText() {
        return "plain_text".equals(type) || delta == null;
    }
    
    /**
     * Get display text (plain text representation)
     */
    public String getDisplayText() {
        return plainText != null ? plainText : "";
    }
    
    /**
     * Validate content structure
     */
    public boolean isValid() {
        if (type == null || plainText == null) {
            return false;
        }
        
        if ("rich_text".equals(type)) {
            return delta != null;
        }
        
        return "plain_text".equals(type);
    }
} 