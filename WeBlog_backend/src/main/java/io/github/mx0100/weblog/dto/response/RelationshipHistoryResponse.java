package io.github.mx0100.weblog.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Relationship history response DTO
 * 
 * @author mx0100
 */
@Data
public class RelationshipHistoryResponse {
    
    private Long id;
    private Long partnerId;
    private String partnerUsername;
    private String partnerNickname;
    private String relationshipType;
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endedAt;
} 