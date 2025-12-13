package io.github.blakedunaway.authserver.integration.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "registered_client")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA
public class RegisteredClientEntity implements Persistable<String> {

    @Id
    @Column(name = "registered_client_id", updatable = false, nullable = false)
    private String registeredClientId;

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
    @Column(name = "client_settings", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> clientSettings = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "token_settings", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> tokenSettings = new LinkedHashMap<>();

    // ---------- children ----------
    @OneToMany(mappedBy = "registeredClient", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true)
    private Set<RegisteredClientAuthMethodEntity> clientAuthenticationMethods = new LinkedHashSet<>();

    @OneToMany(mappedBy = "registeredClient", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true)
    private Set<RegisteredClientGrantTypeEntity> authorizationGrantTypes = new LinkedHashSet<>();

    @OneToMany(mappedBy = "registeredClient", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true)
    private Set<RegisteredClientRedirectUriEntity> redirectUris = new LinkedHashSet<>();

    @OneToMany(mappedBy = "registeredClient", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true)
    private Set<RegisteredClientPostLogoutRedirectUriEntity> postLogoutRedirectUris = new LinkedHashSet<>();

    @OneToMany(mappedBy = "registeredClient", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true)
    private Set<RegisteredClientScopeEntity> scopes = new LinkedHashSet<>();

    public static RegisteredClientEntity createFromId(final String registeredClientId) {
        final RegisteredClientEntity rc = new RegisteredClientEntity();
        rc.registeredClientId = registeredClientId;
        return rc;
    }

    /**
     * Factory for mappers/services
     */
    public static RegisteredClientEntity create(
            final String registeredClientId,
            final String clientId,
            final LocalDateTime clientIdIssuedAt,
            final String clientSecret,
            final LocalDateTime clientSecretExpiresAt,
            final String clientName,
            final Map<String, Object> clientSettings,
            final Map<String, Object> tokenSettings) {
        final RegisteredClientEntity clientEntity = new RegisteredClientEntity();
        clientEntity.registeredClientId = registeredClientId;
        clientEntity.clientId = clientId;
        clientEntity.clientIdIssuedAt = clientIdIssuedAt;
        clientEntity.clientSecret = clientSecret;
        clientEntity.clientSecretExpiresAt = clientSecretExpiresAt;
        clientEntity.clientName = clientName;
        clientEntity.clientSettings = new LinkedHashMap<>(
                clientSettings != null ? clientSettings : new HashMap<>()
        );
        clientEntity.tokenSettings = new LinkedHashMap<>(
                tokenSettings != null ? tokenSettings : new HashMap<>()
        );
        return clientEntity;
    }

    public void overwriteBasics(
            final String clientId,
            final LocalDateTime clientIdIssuedAt,
            final String clientSecret,
            final LocalDateTime clientSecretExpiresAt,
            final String clientName,
            final Map<String, Object> clientSettings,
            final Map<String, Object> tokenSettings
    ) {
        this.clientId = clientId;
        this.clientIdIssuedAt = clientIdIssuedAt;
        this.clientSecret = clientSecret;
        this.clientSecretExpiresAt = clientSecretExpiresAt;
        this.clientName = clientName;

        this.clientSettings.clear();
        if (clientSettings != null) {
            this.clientSettings.putAll(clientSettings);
        }

        this.tokenSettings.clear();
        if (tokenSettings != null) {
            this.tokenSettings.putAll(tokenSettings);
        }
    }

    public void addClientAuthenticationMethod(final String method) {
        if (method == null || method.isBlank()) {
            return;
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
                        Set.<String>of()
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
        boolean exists = authorizationGrantTypes.stream()
                                                .anyMatch(e -> grantType.equals(e.getAuthorizationGrantType()));
        if (!exists) {
            authorizationGrantTypes.add(new RegisteredClientGrantTypeEntity(this, grantType));
        }
    }

    public void replaceAuthorizationGrantTypes(final Collection<String> grantTypes) {
        Set<String> target =
                grantTypes == null ?
                        Set.<String>of()
                        : grantTypes.stream()
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
        boolean exists = redirectUris.stream().anyMatch(e -> uri.equals(e.getRedirectUri()));
        if (!exists) {
            redirectUris.add(new RegisteredClientRedirectUriEntity(this, uri));
        }
    }

    public void replaceRedirectUris(final Collection<String> uris) {
        Set<String> target =
                uris == null ?
                        Set.<String>of()
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
        boolean exists = postLogoutRedirectUris.stream().anyMatch(e -> uri.equals(e.getPostLogoutRedirectUri()));
        if (!exists) {
            postLogoutRedirectUris.add(new RegisteredClientPostLogoutRedirectUriEntity(this, uri));
        }
    }

    public void replacePostLogoutRedirectUris(final Collection<String> uris) {
        Set<String> target =
                uris == null ?
                        Set.<String>of()
                        : uris.stream()
                              .filter(Objects::nonNull)
                              .map(String::trim)
                              .filter(s -> !s.isEmpty())
                              .collect(Collectors.toSet());
        postLogoutRedirectUris.removeIf(e -> !target.contains(e.getPostLogoutRedirectUri()));
        for (final String uri : target) {
            addPostLogoutRedirectUri(uri);
        }
    }

    public void addScope(final String scope) {
        if (scope == null || scope.isBlank()) {
            return;
        }
        boolean exists = scopes.stream().anyMatch(e -> scope.equals(e.getScope()));
        if (!exists) {
            scopes.add(new RegisteredClientScopeEntity(this, scope));
        }
    }

    public void replaceScopes(final Collection<String> newScopes) {
        Set<String> target =
                newScopes == null ?
                        Set.<String>of()
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

    @Override
    public String getId() {
        return registeredClientId != null ? registeredClientId : null;
    }

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }


}
