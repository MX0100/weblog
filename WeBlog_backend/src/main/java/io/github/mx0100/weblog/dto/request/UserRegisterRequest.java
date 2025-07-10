package io.github.mx0100.weblog.dto.request;

import io.github.mx0100.weblog.entity.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * User register request DTO
 * 
 * @author mx0100
 */
@Data
public class UserRegisterRequest {
    
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers and underscores")
    private String username;
    
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
    private String password;
    
    @NotBlank(message = "Nickname cannot be blank")
    @Size(min = 1, max = 50, message = "Nickname must be between 1 and 50 characters")
    private String nickname;
    
    private Gender gender;
    
    private String profileimg;
    
    @Size(max = 10, message = "Maximum 10 hobbies allowed")
    private List<String> hobby;
} 