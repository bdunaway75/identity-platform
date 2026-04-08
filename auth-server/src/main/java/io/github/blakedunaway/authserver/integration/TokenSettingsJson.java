package io.github.blakedunaway.authserver.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.blakedunaway.authserver.business.model.enums.TokenFormat;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;

@Getter
@Setter
@Builder(toBuilder = true)
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenSettingsJson {

    @Builder.Default
    private Duration authorizationCodeTimeToLive = Duration.ofMinutes(5);

    @Builder.Default
    private Duration accessTokenTimeToLive = Duration.ofMinutes(5);

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private TokenFormat accessTokenFormat = TokenFormat.SELF;

    @Builder.Default
    private Duration deviceCodeTimeToLive = Duration.ofMinutes(5);

    @Builder.Default
    private boolean reuseRefreshTokens = true;

    @Builder.Default
    private Duration refreshTokenTimeToLive = Duration.ofDays(1);

    @Builder.Default
    private String idTokenSignatureAlgorithm = "RS256";

    @Builder.Default
    private boolean x509CertificateBoundAccessTokens = false;

}
