package io.github.mx0100.weblog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Password change request DTO
 * 
 * @author mx0100
 */
@Data
public class PasswordChangeRequest {
    
    @NotBlank(message = "Old password cannot be blank")
    private String oldPassword;
    
    @NotBlank(message = "New password cannot be blank")
    @Size(min = 6, max = 50, message = "New password must be between 6 and 50 characters")
    private String newPassword;
} 