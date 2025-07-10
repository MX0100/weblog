package io.github.mx0100.weblog.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Comment batch request DTO
 * 
 * @author mx0100
 */
@Data
public class CommentBatchRequest {
    
    @NotEmpty(message = "Comment IDs cannot be empty")
    private List<Long> commentIds;
} 