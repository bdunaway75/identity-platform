package io.github.blakedunaway.authserver.business.model;

import io.github.blakedunaway.authserver.business.model.enums.MetaDataKeys;
import io.github.blakedunaway.authserver.business.model.enums.TokenType;
import io.github.blakedunaway.authserver.util.AuthenticationUtility;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

import static org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER;
import static org.springframework.security.oauth2.server.authorization.OAuth2Authorization.Token.CLAIMS_METADATA_NAME;
import static org.springframework.security.oauth2.server.authorization.OAuth2Authorization.Token.INVALIDATED_METADATA_NAME;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthToken {

    private final UUID id;

    private final String kid;

    private final boolean isNew;

    private final Instant issuedAt;

    private final Instant expiresAt;

    private final Instant revokedAt;

    private final TokenType tokenType;

    private final Set<String> scopes;

    private final String subject;

    @ToString.Exclude
    private final String hashedTokenValue;

    private final Map<String, Object> metadata;

    private final Map<String, Object> claims;

    public static Builder fromId(final UUID id) {
        return new Builder(id);
    }

    public static Builder fromSpring(final OAuth2Authorization.Token<?> token) {
        Assert.notNull(token, "OAuth2Token cannot be null");
        return Builder.initTokenFromOAuth2Token(TokenType.getTokenTypeByOAuthTokenClass(token.getToken()
                                                                                             .getClass()), token);
    }

    public boolean isHashedToken(final String hashedTokenValue) {
        return this.getHashedTokenValue()
                   .equals(hashedTokenValue);
    }

    private OAuth2Token toOAuth2Token(final String tokenValue) {
        return switch (this.getTokenType()) {
            case ACCESS -> new OAuth2AccessToken(
                    BEARER, tokenValue, this.getIssuedAt(), this.getExpiresAt(), (this.getScopes() == null) ? Set.of() : this.getScopes()
            );
            case REFRESH -> new OAuth2RefreshToken(tokenValue, this.getIssuedAt(), this.getExpiresAt());
            case AUTHORIZATION_CODE -> new OAuth2AuthorizationCode(tokenValue, this.getIssuedAt(), this.getExpiresAt());
            case ID_TOKEN ->
                    new OidcIdToken(tokenValue, this.getIssuedAt(), this.getExpiresAt(), (this.getClaims() == null) ? new HashMap<>() : this.getClaims());
        };
    }

    public OAuth2Authorization.Builder attachToAuthorization(final OAuth2Authorization.Builder builder, final String tokenValue) {
        final OAuth2Token token = toOAuth2Token(tokenValue);
        final Set<String> reserved = new HashSet<>(Set.of(CLAIMS_METADATA_NAME,
                                                          MetaDataKeys.REVOKED_AT.getValue(),
                                                          MetaDataKeys.KID.getValue()));
        builder.token(token, meta -> {
            if (!this.getClaims()
                     .isEmpty()) {
                meta.put(CLAIMS_METADATA_NAME, this.getClaims());
            }
            if (this.getRevokedAt() != null) {
                meta.put(INVALIDATED_METADATA_NAME, Boolean.TRUE);
                meta.put(MetaDataKeys.REVOKED_AT.getValue(), this.getRevokedAt());
                reserved.add(INVALIDATED_METADATA_NAME);
            }
            if (this.getKid() != null && !this.getKid()
                                              .isBlank()) {
                meta.put(MetaDataKeys.KID.getValue(), this.getKid());
            }

            if (!this.getMetadata()
                     .isEmpty()) {
                for (final Map.Entry<String, Object> e : this.getMetadata()
                                                             .entrySet()) {
                    if (!reserved.contains(e.getKey())) {
                        meta.put(e.getKey(), e.getValue());
                    }
                }
            }
        });
        return builder;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthToken other)) {
            return false;
        }
        // Prefer business key if id can be null for new tokens
        if (this.id != null && other.id != null) {
            return this.id.equals(other.id);
        }
        return this.tokenType == other.tokenType
                && java.util.Objects.equals(this.hashedTokenValue, other.hashedTokenValue);
    }

    @Override
    public int hashCode() {
        return (id != null) ? id.hashCode()
                : java.util.Objects.hash(tokenType, hashedTokenValue);
    }

    public Builder toBuilder() {
        return new Builder(this.getId())
                .isNew(this.isNew())
                .hashedTokenValue(this.getHashedTokenValue())
                .tokenType(this.getTokenType())
                .subject(this.getSubject())
                .issuedAt(this.getIssuedAt())
                .expiresAt(this.getExpiresAt())
                .revokedAt(this.getRevokedAt())
                .metadata(meta -> meta.putAll(this.getMetadata()))
                .claims(meta -> meta.putAll(this.getClaims()))
                .scopes(scopesMutator -> scopesMutator.addAll(this.getScopes()))
                .kid(this.getKid());
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder {

        private UUID id;

        private String kid;

        private boolean isNew;

        private Instant issuedAt;

        private Instant expiresAt;

        private Instant revokedAt;

        private TokenType tokenType;

        private String subject;

        private String hashedTokenValue;

        private Set<String> scopes = new LinkedHashSet<>();

        private Map<String, Object> metadata = new LinkedHashMap<>();

        private Map<String, Object> claims = new LinkedHashMap<>();

        protected Builder(final UUID id) {
            this.id = id;
        }

        private static Builder initTokenFromOAuth2Token(final TokenType tokenType, final OAuth2Authorization.Token<?> token) {
            Assert.notNull(tokenType, "tokenType must not be null");
            final Builder builder = new Builder();
            final OAuth2Token oAuthToken = token.getToken();
            Assert.notNull(oAuthToken, "oAuthToken must not be null");

            final Map<String, Object> metaData = token.getMetadata() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(token.getMetadata());
            Assert.notEmpty(metaData, "metaData must not be empty");
            final String kid = (String) metaData.get(MetaDataKeys.KID.getValue());
            Assert.notNull(kid, "Token must have an attached key ID");
            if (tokenType == TokenType.ID_TOKEN) {
                final OidcIdToken id = (OidcIdToken) oAuthToken;
                if (!metaData.containsKey(CLAIMS_METADATA_NAME) && id.getClaims() != null) {
                    metaData.put(CLAIMS_METADATA_NAME, id.getClaims());
                }
            }

            builder.metadata(metaDataConsumer -> metaDataConsumer.putAll(metaData))
                   .tokenType(tokenType)
                   .hashedTokenValue(oAuthToken.getTokenValue())
                   .issuedAt(oAuthToken.getIssuedAt())
                   .expiresAt(oAuthToken.getExpiresAt())
                   .kid(kid);

            return switch (tokenType) {
                case ACCESS -> {
                    final OAuth2AccessToken oAuthAccessToken = (OAuth2AccessToken) oAuthToken;
                    yield builder.scopes(scopes -> scopes.addAll(oAuthAccessToken.getScopes()));
                }
                case REFRESH, AUTHORIZATION_CODE -> builder;

                case ID_TOKEN -> {
                    final OidcIdToken idToken = (OidcIdToken) oAuthToken;
                    final Object sub = idToken.getClaim(MetaDataKeys.SUBJECT.getValue());
                    if (sub instanceof String s && !s.isBlank()) {
                        builder.subject(s);
                    }
                    yield builder;
                }
            };

        }

        public Builder revokedAt(final Instant revokedAt) {
            this.revokedAt = revokedAt;
            return this;
        }

        public Builder kid(final String kid) {
            this.kid = kid;
            return this;
        }

        public Builder exists(final UUID id) {
            this.id = id;
            this.isNew = false;
            return this;
        }

        public Builder isNew(final boolean isNew) {
            this.isNew = isNew;
            return this;
        }

        public Builder subject(final String subject) {
            this.subject = subject;
            return this;
        }

        public Builder expiresAt(final Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder issuedAt(final Instant issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }

        public Builder hashedTokenValue(final String hashedTokenValue) {
            Assert.notNull(hashedTokenValue, "hashedTokenValue must not be null");
            this.hashedTokenValue = hashedTokenValue;
            return this;
        }

        public Builder tokenType(final TokenType tokenType) {
            Assert.notNull(tokenType, "tokenType must not be null");
            this.tokenType = tokenType;
            return this;
        }

        public Builder scopes(final Consumer<Set<String>> scopesMutator) {
            scopesMutator.accept(this.getScopes());
            return this;
        }

        public Builder metadata(final Consumer<Map<String, Object>> metadataMutator) {
            metadataMutator.accept(this.getMetadata());
            return this;
        }

        public Builder claims(final Consumer<Map<String, Object>> claimsMutator) {
            if (claimsMutator != null) {
                claimsMutator.accept(this.getClaims());
            }
            return this;
        }

        public AuthToken build() {
            Assert.notNull(this.getTokenType(), "tokenType");
            Assert.notNull(this.getIssuedAt(), "issuedAt");
            Assert.notNull(this.getExpiresAt(), "expiresAt");
            Assert.hasText(this.getHashedTokenValue(), "hashedTokenValue must not be null or empty");
            Assert.notNull(this.getKid(), "Key ID must not be null");
            if (!this.getIssuedAt()
                     .isBefore(this.getExpiresAt())) {
                throw new IllegalArgumentException("issuedAt must be before expiresAt");
            }

            claims(claimsMutator ->
                           claimsMutator.putAll(AuthenticationUtility.parseJsonKeyWithJsonMapValue(CLAIMS_METADATA_NAME, this.getMetadata())));

            if (this.getTokenType() == TokenType.ID_TOKEN && this.getClaims()
                                                                 .isEmpty()) {
                throw new IllegalStateException("ID_TOKEN requires non-empty claims");
            }

            return new AuthToken(
                    this.getId() != null ? this.getId() : UUID.randomUUID(),
                    this.getKid(),
                    this.isNew(),
                    this.getIssuedAt(),
                    this.getExpiresAt(),
                    this.getRevokedAt(),
                    this.getTokenType(),
                    this.getScopes() == null ? Set.of() : this.getScopes(),
                    this.getSubject(),
                    this.getHashedTokenValue(),
                    this.getMetadata(),
                    this.getClaims()
            );
        }

    }

}

