package io.github.mx0100.weblog.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.mx0100.weblog.dto.RichContent;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Post response DTO with rich text content support
 * 
 * @author mx0100
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostResponse {
    
    private Long postId;
    private Long userId;
    private AuthorInfo author;
    
    /**
     * Rich text content
     */
    private RichContent richContent;
    
    private List<Long> comments; // 评论ID列表
    private Integer commentsCount; // 评论数量
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    /**
     * Author info nested class
     */
    @Data
    public static class AuthorInfo {
        private Long userId;
        private String username;
        private String nickname;
    }
    
    /**
     * Check if post has rich text content
     */
    public boolean hasRichTextContent() {
        return richContent != null && richContent.isRichText();
    }
    
    /**
     * Get plain text representation for backward compatibility
     */
    public String getPlainTextContent() {
        return richContent != null ? richContent.getDisplayText() : "";
    }
} 