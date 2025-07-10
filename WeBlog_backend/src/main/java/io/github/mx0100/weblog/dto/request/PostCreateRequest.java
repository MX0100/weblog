package io.github.mx0100.weblog.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.mx0100.weblog.dto.RichContent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Post create request DTO with rich text content support
 * 
 * @author mx0100
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostCreateRequest {
    
    /**
     * Rich text content (required)
     */
    @Valid
    @NotNull(message = "Rich content cannot be null")
    private RichContent richContent;
    
    /**
     * Validate that rich content is provided and valid
     */
    public boolean hasValidContent() {
        return richContent != null && richContent.isValid() && 
               !richContent.getDisplayText().trim().isEmpty();
    }
    
    /**
     * Get effective content
     */
    public RichContent getEffectiveContent() {
        return richContent;
    }
    
    /**
     * Get plain text representation for validation
     */
    public String getPlainTextForValidation() {
        return richContent != null ? richContent.getDisplayText() : "";
    }
    
    /**
     * Set plain text content (convenience method)
     * Creates a plain text RichContent
     */
    public void setPlainTextContent(String plainText) {
        this.richContent = RichContent.createPlainText(plainText != null ? plainText : "");
    }
    
    /**
     * Custom validation: ensure content is valid and not too long
     */
    public boolean isValid() {
        return hasValidContent() && 
               getPlainTextForValidation().length() <= 10000;
    }
} 