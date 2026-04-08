package io.github.blakedunaway.authserver.integration.entity;

import io.github.blakedunaway.authserver.business.model.enums.TokenType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(
        name = "auth_token",
        indexes = {
                @Index(name = "ix_auth_token_authorization", columnList = "authorization_id"),
                @Index(name = "ix_auth_token_authorization_subject", columnList = "authorization_id, subject"),
                @Index(name = "ix_auth_token_expires_at", columnList = "expired_at"),
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthTokenEntity {

    @Id
    @Setter
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID tokenId;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expired_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false)
    private TokenType tokenType;

    @Column(name = "subject")
    private String subject;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "authorization_id", nullable = false)
    private AuthorizationEntity authorizationEntity;

    @Column(name = "token_value_hash", nullable = false, unique = true)
    private String tokenValueHash;

    @Column(name = "kid")
    private String kid;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false)
    private Map<String, Object> metadataJson;

    public static AuthTokenEntity create(final UUID id,
                                         final String kid,
                                         final String tokenValueHash,
                                         final Instant issuedAt,
                                         final Instant expiresAt,
                                         final Instant revokedAt,
                                         final TokenType tokenType,
                                         final String subject,
                                         final Map<String, Object> metadataJson) {
        Objects.requireNonNull(tokenValueHash, "tokenValueHash must not be null");
        Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        Objects.requireNonNull(tokenType, "tokenType must not be null");

        final AuthTokenEntity tokenEntity = new AuthTokenEntity();
        tokenEntity.tokenId = id;
        tokenEntity.kid = kid;
        tokenEntity.tokenValueHash = tokenValueHash;
        tokenEntity.issuedAt = issuedAt;
        tokenEntity.expiresAt = expiresAt;
        tokenEntity.revokedAt = revokedAt;
        tokenEntity.tokenType = tokenType;
        tokenEntity.subject = subject;
        tokenEntity.metadataJson = metadataJson;
        return tokenEntity;
    }

    public AuthTokenEntity setAuthorizationEntity(final AuthorizationEntity parent) {
        this.authorizationEntity = parent;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthTokenEntity other)) {
            return false;
        }
        return tokenValueHash != null && tokenValueHash.equals(other.tokenValueHash);
    }

    @Override
    public int hashCode() {
        return tokenValueHash != null ? tokenValueHash.hashCode() : 0;
    }

}