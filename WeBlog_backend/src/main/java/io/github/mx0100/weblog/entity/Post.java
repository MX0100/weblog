package io.github.mx0100.weblog.entity;

import io.github.mx0100.weblog.dto.RichContent;
import io.github.mx0100.weblog.utils.RichContentConverter;
import io.github.mx0100.weblog.utils.TimeUtils;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Post entity with rich text content support
 * 
 * @author mx0100
 */
@Data
@Entity
@Table(name = "posts")
@EqualsAndHashCode(callSuper = false)
public class Post {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * Rich text content field (JSONb format)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_rich", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = RichContentConverter.class)
    private RichContent richContent;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Set rich content
     */
    public void setRichContent(RichContent richContent) {
        this.richContent = richContent != null ? richContent : RichContent.createPlainText("");
    }
    
    /**
     * Set plain text content (convenience method)
     */
    public void setPlainTextContent(String plainText) {
        this.richContent = RichContent.createPlainText(plainText != null ? plainText : "");
    }
    
    /**
     * Get plain text representation of content
     */
    public String getPlainTextContent() {
        return richContent != null ? richContent.getDisplayText() : "";
    }
    
    /**
     * Check if post has rich text content (not plain text)
     */
    public boolean hasRichTextContent() {
        return richContent != null && richContent.isRichText();
    }
    
    /**
     * Get content for legacy API compatibility
     */
    public String getContent() {
        return getPlainTextContent();
    }
    
    /**
     * Pre-persist hook to set UTC timestamps and validate content
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = TimeUtils.nowUtc();
        this.createdAt = now;
        this.updatedAt = now;
        
        // Ensure rich content is valid
        if (richContent == null) {
            this.richContent = RichContent.createPlainText("");
        }
    }
    
    /**
     * Pre-update hook to set UTC timestamp and validate content
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = TimeUtils.nowUtc();
        
        // Ensure rich content is valid
        if (richContent == null) {
            this.richContent = RichContent.createPlainText("");
        }
    }
} 