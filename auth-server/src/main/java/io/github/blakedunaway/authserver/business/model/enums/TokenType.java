package io.github.blakedunaway.authserver.business.model.enums;

import io.github.blakedunaway.authserver.business.model.AuthToken;
import io.github.blakedunaway.authserver.security.token.TokenHasher;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.springframework.security.oauth2.server.authorization.OAuth2Authorization.Token.CLAIMS_METADATA_NAME;

@Getter
@RequiredArgsConstructor
public enum TokenType {
    ACCESS("access_token", OAuth2AccessToken.class) {
        @Override
        public OAuth2AccessToken applyToken(final AuthToken token, final String tokenValue) {
            return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                                         tokenValue,
                                         token.getIssuedAt(),
                                         token.getExpiresAt(),
                                         token.getScopes());
        }

        @Override
        public  AuthToken applyToken(OAuth2Authorization.Token<?> oAuth2Token) {
            OAuth2AccessToken accessToken = cast(oAuth2Token.getToken());
            return this.buildBasicModel(oAuth2Token).scopes(scopes -> scopes.addAll(accessToken.getScopes())).build();
        }
    },
    REFRESH("refresh_token", OAuth2RefreshToken.class) {
        @Override
        public OAuth2RefreshToken applyToken(AuthToken token, String tokenValue) {
            return new OAuth2RefreshToken(tokenValue, token.getIssuedAt(), token.getExpiresAt());
        }

        @Override
        public  AuthToken applyToken(OAuth2Authorization.Token<?> oAuth2Token) {
            cast(oAuth2Token.getToken());
            return this.buildBasicModel(oAuth2Token).build();
        }
    },
    AUTHORIZATION_CODE("code", OAuth2AuthorizationCode.class) {
        @Override
        public OAuth2AuthorizationCode applyToken(AuthToken token, String tokenValue) {
            return new OAuth2AuthorizationCode(tokenValue, token.getIssuedAt(), token.getExpiresAt());
        }

        @Override
        public  AuthToken applyToken(OAuth2Authorization.Token<?> oAuth2Token) {
            cast(oAuth2Token.getToken());
            return this.buildBasicModel(oAuth2Token).build();
        }
    },
    ID_TOKEN("id_token", OidcIdToken.class) {
        @Override
        public OidcIdToken applyToken(AuthToken token, String tokenValue) {
            return new OidcIdToken(tokenValue, token.getIssuedAt(), token.getExpiresAt(), token.getClaims());
        }

        @Override
        public  AuthToken applyToken(OAuth2Authorization.Token<?> oAuth2Token) {
            OidcIdToken idToken = cast(oAuth2Token.getToken());
            AuthToken.Builder basicAuthTokenBuilder = this.buildBasicModel(oAuth2Token);
            if (idToken.getClaims() != null) {
                if (!oAuth2Token.getMetadata().containsKey(CLAIMS_METADATA_NAME)) {
                    basicAuthTokenBuilder.metadata(metadata -> metadata.put(CLAIMS_METADATA_NAME, idToken.getClaims())); // TODO look into duplications from buildBasicModel
                }
                if (idToken.getClaim(MetaDataKeys.SUBJECT.getValue()) != null) {
                    basicAuthTokenBuilder.subject(idToken.getClaim(MetaDataKeys.SUBJECT.getValue()));
                }
            }
            return basicAuthTokenBuilder.build();
        }
    };

    private static final Map<Class<? extends OAuth2Token>, TokenType> TOKEN_MAP = new HashMap<>();

    static {
        for (TokenType type : values()) {
            TOKEN_MAP.put(type.getAssociatedOAuth2TokenClass(), type);
        }
    }

    private final String wireName;

    private final Class<? extends OAuth2Token> associatedOAuth2TokenClass;

    public static TokenType getTokenTypeByOAuthTokenClass(final Class<? extends OAuth2Token> tokenClass) {
        Assert.notNull(tokenClass, "OAuth2Token cannot be null");
        final TokenType tokenType = TOKEN_MAP.get(tokenClass);
        if (tokenType == null) {
            throw new IllegalArgumentException("No token type of " + tokenClass + " exists!");
        }
        return tokenType;
    }

    public static TokenType getTokenTypeByWireName(final String wireName) {
        if (wireName == null) {
            return null;
        }
        for (final TokenType tokenType : values()) {
            if (tokenType.getWireName().equals(wireName)) {
                return tokenType;
            }
        }
        throw new IllegalArgumentException("No token type of " + wireName + " exists!");
    }

    public abstract OAuth2Token applyToken(final AuthToken token, final String tokenValue);

    public abstract AuthToken applyToken(final OAuth2Authorization.Token<?> oAuth2Token);

    @SuppressWarnings("unchecked")
    public <T extends OAuth2Token> T cast(OAuth2Token token) {
        if (this.getAssociatedOAuth2TokenClass().isInstance(token)) {
            return (T) token;
        }
        throw new IllegalArgumentException(
                "Token is not of type " + this.getAssociatedOAuth2TokenClass().getSimpleName()
        );
    }

    public <T extends OAuth2Token> AuthToken.Builder buildBasicModel(OAuth2Authorization.Token<T> oAuth2Token) {
        OAuth2Token rawToken = oAuth2Token.getToken();
        return AuthToken.builder()
                        .metadata(mutator -> mutator.putAll(oAuth2Token.getMetadata()))
                        .tokenType(this)
                        .hashedTokenValue(TokenHasher.isHmacSha256Base64Url(rawToken.getTokenValue())
                                          ? rawToken.getTokenValue()
                                          : TokenHasher.hmacCurrent(rawToken.getTokenValue()))
                        .issuedAt(rawToken.getIssuedAt())
                        .expiresAt(rawToken.getExpiresAt());
    }

    public static Set<AuthToken> retrieveFromSpring(OAuth2Authorization authorization) {
        Set<AuthToken> foundTokens = new HashSet<>();
        for (TokenType tokenType : TOKEN_MAP.values()) {
            OAuth2Authorization.Token<?> springToken = authorization.getToken(tokenType.associatedOAuth2TokenClass);
            if (springToken != null) {
                foundTokens.add(tokenType.applyToken(springToken));
            }
        }
        return foundTokens;
    }


}
