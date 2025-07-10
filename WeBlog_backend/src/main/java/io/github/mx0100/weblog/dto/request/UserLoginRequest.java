package io.github.mx0100.weblog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * User login request DTO
 * 
 * @author mx0100
 */
@Data
public class UserLoginRequest {
    
    @NotBlank(message = "Username cannot be blank")
    private String username;
    
    @NotBlank(message = "Password cannot be blank")
    private String password;
} 