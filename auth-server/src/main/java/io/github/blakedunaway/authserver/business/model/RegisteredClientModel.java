package io.github.blakedunaway.authserver.business.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class RegisteredClientModel {

    private final UUID id;

    @With
    private final String clientId;

    @With
    private final LocalDateTime clientIdIssuedAt;

    @With
    private final String clientSecret;

    @With
    private final LocalDateTime clientSecretExpiresAt;

    private final String clientName;

    private final Set<ClientAuthenticationMethod> clientAuthenticationMethods;

    private final Set<AuthorizationGrantType> authorizationGrantTypes;

    private final Set<String> redirectUris;

    private final Set<String> postLogoutRedirectUris;

    private final Set<String> scopes;

    private final Set<String> authorities;

    private final Set<String> roles;

    private final ClientSettings clientSettings;

    private final TokenSettings tokenSettings;

    public Map<String, Object> getTokenSettings() {
        return this.tokenSettings == null ? Map.of() : new HashMap<>(this.tokenSettings.getSettings());
    }

    public Map<String, Object> getClientSettings() {
        return this.clientSettings == null ? Map.of() : new HashMap<>(this.clientSettings.getSettings());
    }

    public Set<String> getAuthorizationGrantTypes() {
        return this.authorizationGrantTypes == null
               ? Set.of()
               : this.authorizationGrantTypes.stream().map(AuthorizationGrantType::getValue).collect(Collectors.toSet());
    }

    public Set<String> getClientAuthenticationMethods() {
        return this.clientAuthenticationMethods == null
               ? Set.of()
               : this.clientAuthenticationMethods.stream().map(ClientAuthenticationMethod::toString).collect(Collectors.toSet());
    }

    public Set<String> getRedirectUris() {
        return this.redirectUris == null ? Set.of() : Set.copyOf(this.redirectUris);
    }

    public Set<String> getPostLogoutRedirectUris() {
        return this.postLogoutRedirectUris == null ? Set.of() : Set.copyOf(this.postLogoutRedirectUris);
    }

    public Set<String> getScopes() {
        return this.scopes == null ? Set.of() : Set.copyOf(this.scopes);
    }

    public Set<String> getAuthorities() {
        return this.authorities == null ? Set.of() : Set.copyOf(this.authorities);
    }

    public Set<String> getRoles() {
        return this.roles == null ? Set.of() : Set.copyOf(this.roles);
    }

    public RegisteredClient toOAuth2RegisteredClient() {
        Assert.notNull(clientId, "Client ID must not be null");
        Assert.notNull(clientIdIssuedAt, "Client ID issuedAt must not be null");
        Assert.notNull(clientName, "Client name must not be null");
        final Instant clientSecretExpireAt = clientSecretExpiresAt == null ? null : clientSecretExpiresAt.atZone(ZoneId.systemDefault()).toInstant();
        final ClientSettings resolvedClientSettings = clientSettings == null ? ClientSettings.builder().build() : clientSettings;
        final TokenSettings resolvedTokenSettings = tokenSettings == null ? TokenSettings.builder().build() : tokenSettings;
        final Set<ClientAuthenticationMethod> resolvedAuthenticationMethods =
                clientAuthenticationMethods == null ? Set.of() : clientAuthenticationMethods;
        final Set<AuthorizationGrantType> resolvedGrantTypes = authorizationGrantTypes == null ? Set.of() : authorizationGrantTypes;
        final Set<String> resolvedRedirectUris = getRedirectUris();
        final Set<String> resolvedPostLogoutRedirectUris = getPostLogoutRedirectUris();
        final Set<String> resolvedScopes = getScopes();

        return RegisteredClient.withId(String.valueOf(id))
                               .clientId(clientId)
                               .clientIdIssuedAt(clientIdIssuedAt.atZone(ZoneId.systemDefault()).toInstant())
                               .clientSecret(clientSecret)
                               .clientSecretExpiresAt(clientSecretExpireAt)
                               .clientName(clientName)
                               .clientSettings(resolvedClientSettings)
                               .tokenSettings(resolvedTokenSettings)
                               .clientAuthenticationMethods(set -> set.addAll(resolvedAuthenticationMethods))
                               .authorizationGrantTypes(set -> set.addAll(resolvedGrantTypes))
                               .redirectUris(set -> set.addAll(resolvedRedirectUris))
                               .postLogoutRedirectUris(set -> set.addAll(resolvedPostLogoutRedirectUris))
                               .scopes(set -> set.addAll(resolvedScopes))
                               .build();
    }
}
