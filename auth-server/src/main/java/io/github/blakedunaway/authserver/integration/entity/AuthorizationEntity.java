package io.github.blakedunaway.authserver.integration.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "auth_authorization")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthorizationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID authId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "registered_client_id", nullable = false, updatable = false)
    private RegisteredClientEntity registeredClient;

    @Column(name = "principal_name", nullable = false, length = 256)
    private String principalName;

    @Column(name = "authorization_grant_type", nullable = false, length = 64)
    private String authorizationGrantType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "authorization_scope", joinColumns = @JoinColumn(name = "authorization_id"))
    @Column(name = "scope", nullable = false, length = 128)
    private Set<String> authorizedScopes;

    @OneToMany(mappedBy = "authorizationEntity", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, orphanRemoval = true)
    private Set<AuthTokenEntity> tokens;

    public static AuthorizationEntity create(
            final UUID id,
            final RegisteredClientEntity registeredClientEntity,
            final String principalName,
            final String authorizationGrantType,
            final Set<String> authorizedScopes,
            final Set<AuthTokenEntity> tokens) {
        final AuthorizationEntity authorization = new AuthorizationEntity();
        authorization.authId = id;
        authorization.principalName = principalName;
        authorization.authorizationGrantType = authorizationGrantType;
        authorization.authorizedScopes = authorizedScopes;
        authorization.replaceTokens(tokens);
        authorization.registeredClient = registeredClientEntity;
        return authorization;
    }

    public String getId() {
        return authId != null ? authId.toString() : null;
    }

    public void addToken(final AuthTokenEntity token) {
        if (tokens == null) {
            tokens = new HashSet<>();
        }
        final boolean exists = tokens.stream().anyMatch(t -> t.equals(token));
        if (!exists) {
            tokens.add(token.setAuthorizationEntity(this));
        }
    }

    public void replaceTokens(final Set<AuthTokenEntity> tokens) {
        if (this.tokens != null) {
            this.tokens.clear();
        }
        if (tokens != null) {
            tokens.forEach(this::addToken);
        }
    }

    @Override
    public boolean equals(final Object o) {
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
