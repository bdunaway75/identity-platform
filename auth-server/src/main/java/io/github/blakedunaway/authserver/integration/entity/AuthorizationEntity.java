package io.github.blakedunaway.authserver.integration.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "auth_authorization", indexes = {
        @Index(name = "ix_auth_rcid", columnList = "registered_client_id"),
        @Index(name = "ix_auth_principal", columnList = "principal_name"),
        @Index(name = "ix_auth_grant_type", columnList = "authorization_grant_type")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthorizationEntity implements Persistable<String> {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID authId;

    @Transient
    private boolean isNew = true;

    @Column(name = "registered_client_id", nullable = false, updatable = false, length = 100)
    private String registeredClientId;

    @Column(name = "principal_name", nullable = false, length = 256)
    private String principalName;

    @Column(name = "authorization_grant_type", nullable = false, length = 64)
    private String authorizationGrantType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "authorization_scope",
            joinColumns = @JoinColumn(name = "authorization_id")
    )
    @Column(name = "scope", nullable = false, length = 128)
    private Set<String> authorizedScopes = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> attributes = new HashMap<>();

    @OneToMany(
            mappedBy = "authorizationEntity",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true
    )
    private Set<AuthTokenEntity> tokens = new HashSet<>();

    public static AuthorizationEntity create(
            final UUID id,
            boolean isNew,
            final String registeredClientId,
            final String principalName,
            final String authorizationGrantType,
            final Set<String> authorizedScopes,
            final Map<String, Object> attributes,
            final Set<AuthTokenEntity> tokens) {
        final AuthorizationEntity authorization = new AuthorizationEntity();
        authorization.setNew(isNew);
        authorization.setAuthId(id);
        authorization.setRegisteredClientId(registeredClientId);
        authorization.setPrincipalName(principalName);
        authorization.setAuthorizationGrantType(authorizationGrantType);
        authorization.setAuthorizedScopes(authorizedScopes);
        authorization.replaceTokens(tokens);
        authorization.setAttributes(attributes);
        return authorization;
    }

    public String getId() {
        return authId != null ? authId.toString() : null;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void addAuthorizedScope(final String scope) {
        if (scope != null) {
            if (authorizedScopes.isEmpty()) {
                authorizedScopes = new HashSet<>();
            }
            authorizedScopes.add(scope);
        }
    }

    public void removeAuthorizedScope(final String scope) {
        if (scope != null && !authorizedScopes.isEmpty()) {
            authorizedScopes.remove(scope);
        }
    }

    public void replaceAuthorizedScopes(final Set<String> newScopes) {
        if (newScopes == null) {
            this.authorizedScopes.clear();
            return;
        }

        this.authorizedScopes.retainAll(newScopes);
        this.authorizedScopes.addAll(newScopes);
    }


    public void addToken(final AuthTokenEntity token) {
        if (tokens == null) {
            tokens = new HashSet<>();
        }
        boolean exists = tokens.stream().anyMatch(t -> t.equals(token));
        if (!exists) {
            tokens.add(token.setAuthorizationEntity(this));
        }
    }

    public void removeToken(final AuthTokenEntity token) {
        if (tokens != null) {
            tokens.removeIf(t -> t.equals(token));
        }
    }

    public void replaceTokens(final Set<AuthTokenEntity> tokens) {
        this.tokens.clear();
        if (tokens != null) {
            tokens.forEach(this::addToken);
        }
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthorizationEntity other)) {
            return false;
        }
        return authId != null && authId.equals(other.getAuthId());
    }

    @Override
    public int hashCode() {
        return authId != null ? authId.hashCode() : 0;
    }


}
