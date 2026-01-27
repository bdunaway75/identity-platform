package io.github.blakedunaway.authserver.security.token;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/**
 * Validation againsts incoming JWTs intended for the AS api.
 */
@Component
public class AppSignatureValidator implements OAuth2TokenValidator<Jwt> {

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {

        final String clientId = token.getClaimAsString("azp");
        final String audience = token.getAudience().get(0);
        final Instant issuedAt = token.getIssuedAt();
        final String tokenSig = token.getClaimAsString("app_sig");

        if (clientId == null || tokenSig == null || issuedAt == null) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Missing required claims", null)
            );
        }

        final String expected = TokenHasher.hmacCurrent(clientId + audience + issuedAt);

        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                tokenSig.getBytes(StandardCharsets.UTF_8))) {

            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid app signature", null)
            );
        }

        return OAuth2TokenValidatorResult.success();

    }

}
