package io.github.blakedunaway.authserver.business.api.dto;

import io.github.blakedunaway.authserver.business.model.AuthToken;
import io.github.blakedunaway.authserver.business.model.enums.TokenType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthTokenResponse {

    private final UUID id;

    private final String kid;

    private final Instant issuedAt;

    private final Instant expiresAt;

    private final Instant revokedAt;

    private final TokenType tokenType;

    private final String subject;

    public static AuthTokenResponse fromModel(final AuthToken authToken) {
        return new AuthTokenResponse(
                authToken.getId(),
                authToken.getKid(),
                authToken.getIssuedAt(),
                authToken.getExpiresAt(),
                authToken.getRevokedAt(),
                authToken.getTokenType(),
                authToken.getSubject()
        );
    }

}
