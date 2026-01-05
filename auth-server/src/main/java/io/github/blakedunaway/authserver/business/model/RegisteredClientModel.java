package io.github.blakedunaway.authserver.business.model;

import io.github.blakedunaway.authserviceclient.dto.ClientFields;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
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
import java.util.stream.Collectors;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class RegisteredClientModel implements ClientFields {

    private final String id;

    private final String clientId;

    private final Instant clientIdIssuedAt;

    private final String clientSecret;

    private final Instant clientSecretExpiresAt;

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
        return this.authorizationGrantTypes.stream().map(AuthorizationGrantType::toString).collect(Collectors.toSet());
    }

    public Set<String> getClientAuthenticationMethods() {
        return this.clientAuthenticationMethods.stream().map(ClientAuthenticationMethod::toString).collect(Collectors.toSet());
    }

    public LocalDateTime getClientIdIssuedAt() {
        return LocalDateTime.ofInstant(this.clientIdIssuedAt, ZoneId.systemDefault());
    }

    public LocalDateTime getClientSecretExpiresAt() {
        return LocalDateTime.ofInstant(this.clientSecretExpiresAt, ZoneId.systemDefault());
    }

    public static class RegisteredClientModelBuilder {
        public RegisteredClient toOAuth2RegisteredClient() {
            Assert.notNull(clientId, "Client ID must not be null");
            Assert.notNull(clientIdIssuedAt, "Client ID issuedAt must not be null");
            Assert.notNull(clientName, "Client name must not be null");

            return RegisteredClient.withId(id)
                                   .clientId(clientId)
                                   .clientIdIssuedAt(clientIdIssuedAt)
                                   .clientSecret(clientSecret)
                                   .clientSecretExpiresAt(clientSecretExpiresAt)
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

}
