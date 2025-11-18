package io.github.blakedunaway.authserver.integration.entity;

import io.github.blakedunaway.authserver.business.model.enums.TokenType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.LinkedHashMap;
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
                @Index(name = "ix_auth_token_kid", columnList = "kid")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthTokenEntity implements Persistable<String> {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID tokenId;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expired_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, length = 32)
    private TokenType tokenType;

    @Column(name = "subject", length = 256)
    private String subject;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "authorization_id", nullable = false)
    private AuthorizationEntity authorizationEntity;


    @Column(name = "token_value_hash", nullable = false, length = 64, unique = true)
    private String tokenValueHash;

    @Column(name = "kid", nullable = false)
    private String kid;

    @ManyToOne(fetch = LAZY, optional = true)
    @JoinColumn(name = "kid", referencedColumnName = "kid", insertable = false, updatable = false)
    private SigningKeyEntity signingKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadataJson = new LinkedHashMap<>();

    @Transient
    private boolean isNew = true;

    public static AuthTokenEntity create(UUID id,
                                         String kid,
                                         boolean isNew,
                                         String tokenValueHash,
                                         Instant issuedAt,
                                         Instant expiresAt,
                                         Instant revokedAt,
                                         TokenType tokenType,
                                         String subject,
                                         Map<String, Object> metadataJson) {
        Objects.requireNonNull(tokenValueHash, "tokenValueHash must not be null");
        Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        Objects.requireNonNull(tokenType, "tokenType must not be null");

        AuthTokenEntity e = new AuthTokenEntity();
        e.isNew = isNew;
        e.tokenId = id;
        e.kid = kid;
        e.tokenValueHash = tokenValueHash;
        e.issuedAt = issuedAt;
        e.expiresAt = expiresAt;
        e.revokedAt = revokedAt;
        e.tokenType = tokenType;
        e.subject = subject;
        e.metadataJson = metadataJson;
        return e;
    }

    public static AuthTokenEntity createFromId(UUID id) {
        final AuthTokenEntity authTokenEntity = new AuthTokenEntity();
        authTokenEntity.tokenId = id;
        return authTokenEntity;
    }

    /**
     * Used by your mapper merge path.
     */
    public void updateBasicFields(Instant expiresAt, Instant revokedAt, Map<String, Object> metadataJson) {
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.metadataJson = metadataJson;
    }

    /* ---------- Equality (by id once assigned) ---------- */

    public AuthTokenEntity setAuthorizationEntity(AuthorizationEntity parent) {
        this.authorizationEntity = parent;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthTokenEntity other)) {
            return false;
        }
        return tokenId != null && tokenId.equals(other.tokenId);
    }

    @Override
    public String getId() {
        return tokenId != null ? tokenId.toString() : null;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }


    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public int hashCode() {
        // constant for transient entities; stable once persisted
        return 31;
    }

}