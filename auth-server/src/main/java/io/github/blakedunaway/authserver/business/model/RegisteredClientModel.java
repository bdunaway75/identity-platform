package io.github.blakedunaway.authserver.business.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import lombok.extern.jackson.Jacksonized;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Jacksonized
@Getter
@Builder(toBuilder = true)
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

    private final ClientSettings clientSettings;

    private final TokenSettings tokenSettings;

    public Map<String, Object> getTokenSettings() {
        return this.tokenSettings.getSettings();
    }

    public Map<String, Object> getClientSettings() {
        return this.clientSettings.getSettings();
    }

    public Set<String> getAuthorizationGrantTypes() {
        return this.authorizationGrantTypes.stream().map(AuthorizationGrantType::getValue).collect(Collectors.toSet());
    }

    public Set<String> getClientAuthenticationMethods() {
        return this.clientAuthenticationMethods.stream().map(ClientAuthenticationMethod::toString).collect(Collectors.toSet());
    }

    public RegisteredClient toOAuth2RegisteredClient() {
        Assert.notNull(clientId, "Client ID must not be null");
        Assert.notNull(clientIdIssuedAt, "Client ID issuedAt must not be null");
        Assert.notNull(clientName, "Client name must not be null");
        final Instant clientSecretExpireAt = clientSecretExpiresAt == null ? null : clientSecretExpiresAt.atZone(ZoneId.systemDefault()).toInstant();

        return RegisteredClient.withId(String.valueOf(id))
                               .clientId(clientId)
                               .clientIdIssuedAt(clientIdIssuedAt.atZone(ZoneId.systemDefault()).toInstant())
                               .clientSecret(clientSecret)
                               .clientSecretExpiresAt(clientSecretExpireAt)
                               .clientName(clientName)
                               .clientSettings(clientSettings)
                               .tokenSettings(tokenSettings)
                               .clientAuthenticationMethods(set -> set.addAll(clientAuthenticationMethods))
                               .authorizationGrantTypes(set -> set.addAll(authorizationGrantTypes))
                               .redirectUris(set -> set.addAll(redirectUris))
                               .postLogoutRedirectUris(set -> set.addAll(postLogoutRedirectUris))
                               .scopes(set -> set.addAll(scopes))
                               .build();
    }
}
