package io.github.blakedunaway.authserver.integration.entity;

import io.github.blakedunaway.authserver.integration.TokenSettingsJson;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "registered_client")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegisteredClientEntity {

    @Id
    @Column(name = "registered_client_id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID registeredClientId;

    @Column(name = "client_id", updatable = false, nullable = false, unique = true)
    private String clientId;

    @Column(name = "client_id_issued_at")
    private LocalDateTime clientIdIssuedAt;

    @Column(name = "client_secret")
    private String clientSecret;

    @Column(name = "client_secret_expires_at")
    private LocalDateTime clientSecretExpiresAt;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "client_settings", nullable = false)
    private Map<String, Object> clientSettings = new LinkedHashMap<>();

    @Column(name = "token_settings", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private TokenSettingsJson tokenSettings;

    // ---------- children ----------
    @OneToMany(mappedBy = "registeredClient", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true)
    private Set<RegisteredClientAuthMethodEntity> clientAuthenticationMethods;

    @OneToMany(mappedBy = "registeredClient", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true)
    private Set<RegisteredClientGrantTypeEntity> authorizationGrantTypes;

    @OneToMany(mappedBy = "registeredClient", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true)
    private Set<RegisteredClientRedirectUriEntity> redirectUris;

    @OneToMany(mappedBy = "registeredClient", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true)
    private Set<RegisteredClientPostLogoutRedirectUriEntity> postLogoutRedirectUris;

    @Setter
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "registered_client_scopes",
            joinColumns = @JoinColumn(name = "registered_client_id"),
            inverseJoinColumns = @JoinColumn(name = "scope_id")
    )
    private Set<RegisteredClientScopeEntity> scopes;

    @OneToMany(mappedBy = "registeredClient", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AuthorizationEntity> authorizations;

    public static RegisteredClientEntity createFromId(final UUID registeredClientId) {
        final RegisteredClientEntity rc = new RegisteredClientEntity();
        rc.registeredClientId = registeredClientId;
        return rc;
    }

    /**
     * Factory for mappers/services
     */
    public static RegisteredClientEntity create(
            final UUID registeredClientId,
            final String clientId,
            final LocalDateTime clientIdIssuedAt,
            final String clientSecret,
            final LocalDateTime clientSecretExpiresAt,
            final String clientName,
            final Map<String, Object> clientSettings,
            final TokenSettingsJson tokenSettings) {
        final RegisteredClientEntity clientEntity = new RegisteredClientEntity();
        clientEntity.registeredClientId = registeredClientId;
        clientEntity.clientId = clientId;
        clientEntity.clientIdIssuedAt = clientIdIssuedAt;
        clientEntity.clientSecret = clientSecret;
        clientEntity.clientSecretExpiresAt = clientSecretExpiresAt;
        clientEntity.clientName = clientName;
        clientEntity.clientSettings = new LinkedHashMap<>(clientSettings != null ? clientSettings : new HashMap<>());
        clientEntity.tokenSettings = tokenSettings;
        return clientEntity;
    }

    public void overwriteBasics(
            final String clientId,
            final LocalDateTime clientIdIssuedAt,
            final String clientSecret,
            final LocalDateTime clientSecretExpiresAt,
            final String clientName,
            final Map<String, Object> clientSettings,
            final TokenSettingsJson tokenSettings
    ) {
        this.clientId = clientId;
        this.clientIdIssuedAt = clientIdIssuedAt;
        this.clientSecret = clientSecret;
        this.clientSecretExpiresAt = clientSecretExpiresAt;
        this.clientName = clientName;
        this.tokenSettings = tokenSettings;

        this.clientSettings.clear();
        if (clientSettings != null) {
            this.clientSettings.putAll(clientSettings);
        }
    }

    public void addClientAuthenticationMethod(final String method) {
        if (method == null || method.isBlank()) {
            return;
        }
        if (this.getClientAuthenticationMethods() == null) {
            this.clientAuthenticationMethods = new HashSet<>();
        }
        boolean exists =
                clientAuthenticationMethods.stream().anyMatch(e -> method.equals(e.getClientAuthMethod()));
        if (!exists) {
            clientAuthenticationMethods.add(new RegisteredClientAuthMethodEntity(this, method));
        }
    }

    public void replaceClientAuthenticationMethods(final Collection<String> methods) {
        Set<String> target =
                methods == null ?
                Set.of()
                                : methods.stream()
                                         .filter(Objects::nonNull)
                                         .map(String::trim)
                                         .filter(s -> !s.isEmpty())
                                         .collect(Collectors.toSet());
        clientAuthenticationMethods.removeIf(e -> !target.contains(e.getClientAuthMethod()));
        for (final String method : target) {
            addClientAuthenticationMethod(method);
        }
    }

    public void addAuthorizationGrantType(final String grantType) {
        if (grantType == null || grantType.isBlank()) {
            return;
        }
        if (this.getAuthorizationGrantTypes() == null) {
            this.authorizationGrantTypes = new HashSet<>();
        }
        boolean exists =
                authorizationGrantTypes.stream()
                                       .anyMatch(grantTypeEntity ->
                                                         grantType.equals(grantTypeEntity.getAuthorizationGrantType()));
        if (!exists) {
            authorizationGrantTypes.add(new RegisteredClientGrantTypeEntity(this, grantType));
        }
    }

    public void replaceAuthorizationGrantTypes(final Collection<String> grantTypes) {
        Set<String> target =
                grantTypes == null ?
                Set.of() : grantTypes.stream()
                                     .filter(Objects::nonNull)
                                     .map(String::trim)
                                     .filter(s -> !s.isEmpty())
                                     .collect(Collectors.toSet());
        authorizationGrantTypes.removeIf(e -> !target.contains(e.getAuthorizationGrantType()));
        for (final String grant : target) {
            addAuthorizationGrantType(grant);
        }
    }

    public void addRedirectUri(final String uri) {
        if (uri == null || uri.isBlank()) {
            return;
        }
        if (this.getRedirectUris() == null) {
            this.redirectUris = new HashSet<>();
        }
        boolean exists = redirectUris.stream().anyMatch(e -> uri.equals(e.getRedirectUri()));
        if (!exists) {
            redirectUris.add(new RegisteredClientRedirectUriEntity(this, uri));
        }
    }

    public void replaceRedirectUris(final Collection<String> uris) {
        Set<String> target =
                uris == null ?
                Set.of()
                             : uris.stream()
                                   .filter(Objects::nonNull)
                                   .map(String::trim)
                                   .filter(s -> !s.isEmpty())
                                   .collect(Collectors.toSet());
        redirectUris.removeIf(e -> !target.contains(e.getRedirectUri()));
        for (final String uri : target) {
            addRedirectUri(uri);
        }
    }

    public void addPostLogoutRedirectUri(final String uri) {
        if (uri == null || uri.isBlank()) {
            return;
        }
        if (this.postLogoutRedirectUris == null) {
            this.postLogoutRedirectUris = new HashSet<>();
        }
        boolean exists = postLogoutRedirectUris.stream()
                                               .anyMatch(uriEntity -> uri.equals(uriEntity.getPostLogoutRedirectUri()));
        if (!exists) {
            postLogoutRedirectUris.add(new RegisteredClientPostLogoutRedirectUriEntity(this, uri));
        }
    }

    public void replacePostLogoutRedirectUris(final Collection<String> uris) {
        Set<String> target =
                uris == null ?
                Set.of()
                             : uris.stream()
                                   .filter(Objects::nonNull)
                                   .map(String::trim)
                                   .filter(s -> !s.isEmpty())
                                   .collect(Collectors.toSet());
        postLogoutRedirectUris.removeIf(uriEntity -> !target.contains(uriEntity.getPostLogoutRedirectUri()));
        for (final String uri : target) {
            addPostLogoutRedirectUri(uri);
        }
    }

    public void addScope(final String scope) {
        if (scope == null || scope.isBlank()) {
            return;
        }
        if (this.scopes == null) {
            this.scopes = new HashSet<>();
        }
        boolean exists = scopes.stream().anyMatch(scopeEntity -> scope.equals(scopeEntity.getScope()));
        if (!exists) {
            scopes.add(new RegisteredClientScopeEntity(null, scope));
        }
    }

    public void replaceScopes(final Collection<String> newScopes) {
        Set<String> target =
                newScopes == null ?
                Set.of()
                                  : newScopes.stream()
                                             .filter(Objects::nonNull)
                                             .map(String::trim)
                                             .filter(s -> !s.isEmpty())
                                             .collect(Collectors.toSet());
        scopes.removeIf(e -> !target.contains(e.getScope()));
        for (final String scope : target) {
            addScope(scope);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RegisteredClientEntity other)) {
            return false;
        }
        return registeredClientId != null && registeredClientId.equals(other.getRegisteredClientId());
    }

    @Override
    public int hashCode() {
        return registeredClientId != null ? registeredClientId.hashCode() : 0;
    }

}
