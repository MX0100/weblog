package io.github.mx0100.weblog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for actions requiring a partner's username
 */
@Data
public class PartnerUsernameRequest {

    @NotBlank(message = "Partner username cannot be blank")
    private String partnerUsername;
} 