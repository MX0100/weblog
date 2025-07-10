package io.github.mx0100.weblog.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * Pair request DTO
 * 
 * @author mx0100
 */
@Data
public class PairRequest {
    
    @NotBlank(message = "Partner username cannot be blank")
    private String partnerUsername;
} 