package io.github.blakedunaway.authserver.business.model.enums;

import io.github.blakedunaway.authserver.business.model.AuthToken;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.util.Assert;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum TokenType {
    ACCESS("access_token", OAuth2AccessToken.class) {
        @Override
        public  OAuth2AccessToken applyToken(final AuthToken token, final String tokenValue) {
            return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, tokenValue, token.getIssuedAt(), token.getExpiresAt(), token.getScopes());
        }
    },
    REFRESH("refresh_token", OAuth2RefreshToken.class) {
        @Override
        public OAuth2RefreshToken applyToken(AuthToken token, String tokenValue) {
            return new OAuth2RefreshToken(tokenValue, token.getIssuedAt(), token.getExpiresAt());
        }
    },
    AUTHORIZATION_CODE("authorization_code", OAuth2AuthorizationCode.class) {
        @Override
        public OAuth2AuthorizationCode applyToken(AuthToken token, String tokenValue) {
            return new OAuth2AuthorizationCode(tokenValue, token.getIssuedAt(), token.getExpiresAt());
        }
    },
    ID_TOKEN("id_token", OidcIdToken.class) {
        @Override
        public OidcIdToken applyToken(AuthToken token, String tokenValue) {
            return new OidcIdToken(tokenValue, token.getIssuedAt(), token.getExpiresAt(), token.getClaims());
        }
    };

    private final String wireName;

    private final Class<? extends OAuth2Token> associatedOAuth2TokenClass;

    private static final Map<Class<? extends OAuth2Token>, TokenType> TOKEN_MAP = new HashMap<>();

    static {
        for (TokenType t : values()) {
            TOKEN_MAP.put(t.associatedOAuth2TokenClass, t);
        }
    }

    public static TokenType getTokenTypeByOAuthTokenClass(final Class<? extends OAuth2Token> tokenClass) {
        Assert.notNull(tokenClass, "OAuth2Token cannot be null");
        final TokenType tokenType = TOKEN_MAP.get(tokenClass);
        if (tokenType == null) {
            throw new IllegalArgumentException("No token type of " + tokenClass + " exists!");
        }
        return tokenType;
    }

    public static Optional<TokenType> getTokenTypeByWireName(final String wireName) {
        for (final TokenType tokenType : values()) {
            if (tokenType.wireName.equals(wireName)) {
                return Optional.of(tokenType);
            }
        }
        return Optional.empty();
    }

    public abstract <T extends OAuth2Token> T applyToken(final AuthToken token, final String tokenValue);


}
