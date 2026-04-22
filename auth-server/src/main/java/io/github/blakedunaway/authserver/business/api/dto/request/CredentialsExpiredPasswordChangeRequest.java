package io.github.blakedunaway.authserver.business.api.dto.request;

import io.github.blakedunaway.authserver.business.validation.ValidEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CredentialsExpiredPasswordChangeRequest {

    @ValidEmail
    @NotBlank(message = "Email is required")
    private String email;

    private String clientId;

    @NotBlank(message = "Current password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String confirmPassword;
}
