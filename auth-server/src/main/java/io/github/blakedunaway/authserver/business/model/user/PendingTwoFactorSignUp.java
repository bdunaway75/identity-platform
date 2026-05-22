package io.github.blakedunaway.authserver.business.model.user;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class PendingTwoFactorSignUp {

    private String email;

    private String passwordHash;

    private String clientId;

    private String verificationSessionId;

    private boolean platformFlow;

}
