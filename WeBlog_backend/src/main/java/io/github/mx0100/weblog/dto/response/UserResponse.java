package io.github.mx0100.weblog.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.mx0100.weblog.entity.enums.Gender;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User response DTO
 * 
 * @author mx0100
 */
@Data
public class UserResponse {
    
    private Long userId;
    private String username;
    private String nickname;
    private Gender gender;
    private String profileimg;
    private List<String> hobby;
    
    // Relationship status fields
    private String relationshipStatus;
    private Long partnerId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
} 