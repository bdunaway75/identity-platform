package io.github.blakedunaway.authserver.business.model;

import io.github.blakedunaway.authserver.business.model.enums.TokenType;
import io.github.blakedunaway.authserver.util.AuthenticationUtility;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.springframework.security.oauth2.server.authorization.OAuth2Authorization.Token.CLAIMS_METADATA_NAME;

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

    public static Builder builder() {
        return new Builder();
    }

    public OAuth2Token toOAuth2Token() {
        return this.getTokenType().applyToken(this, this.hashedTokenValue);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthToken other)) {
            return false;
        }
        if (this.id != null && other.id != null) {
            return this.id.equals(other.id);
        }
        return this.tokenType == other.tokenType
               && Objects.equals(this.hashedTokenValue, other.hashedTokenValue);
    }

    @Override
    public int hashCode() {
        return (id != null) ? id.hashCode()
                            : Objects.hash(tokenType, hashedTokenValue);
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

        private final Set<String> scopes = new HashSet<>();

        private final Map<String, Object> metadata = new LinkedHashMap<>();

        private final Map<String, Object> claims = new LinkedHashMap<>();

        protected Builder(final UUID id) {
            this.id = id;
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

