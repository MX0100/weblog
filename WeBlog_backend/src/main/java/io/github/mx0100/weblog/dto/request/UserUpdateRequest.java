package io.github.mx0100.weblog.dto.request;

import io.github.mx0100.weblog.entity.enums.Gender;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * User update request DTO
 * 
 * @author mx0100
 */
@Data
public class UserUpdateRequest {
    
    @Size(min = 1, max = 50, message = "Nickname must be between 1 and 50 characters")
    private String nickname;
    
    private Gender gender;
    
    private String profileimg;
    
    @Size(max = 10, message = "Maximum 10 hobbies allowed")
    private List<String> hobby;
} 