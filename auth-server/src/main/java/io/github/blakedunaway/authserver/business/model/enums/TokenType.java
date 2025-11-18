package io.github.blakedunaway.authserver.business.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.util.Assert;

import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum TokenType {
    ACCESS("access_token", OAuth2AccessToken.class),
    REFRESH("refresh_token", OAuth2RefreshToken.class),
    AUTHORIZATION_CODE("authorization_code", OAuth2AuthorizationCode.class),
    ID_TOKEN("id_token", OidcIdToken.class);

    private final String wireName;

    private final Class<? extends OAuth2Token> associatedOAuth2TokenClass;

    public static TokenType getTokenTypeByOAuthTokenClass(final Class<? extends OAuth2Token> tokenClass) {
        Assert.notNull(tokenClass, "OAuth2Token cannot be null");
        for (final TokenType tokenType : values()) {
            if (tokenType.associatedOAuth2TokenClass.equals(tokenClass)) {
                return tokenType;
            }
        }
        throw new IllegalArgumentException("No token type of " + tokenClass + " exists!");
    }

    public static Optional<TokenType> getTokenTypeByWireName(final String wireName) {
        for (final TokenType tokenType : values()) {
            if (tokenType.wireName.equals(wireName)) {
                return Optional.of(tokenType);
            }
        }
        return Optional.empty();
    }
}
