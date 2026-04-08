package io.github.blakedunaway.authserver.business.api.dto;

import io.github.blakedunaway.authserver.business.model.CreatedRegisteredClient;
import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class RegisteredClientView {

    private final UUID id;

    private final String clientId;

    private final LocalDateTime clientIdIssuedAt;

    private final String clientSecret;

    private final String clientName;

    private final Set<String> clientAuthenticationMethods;

    private final Set<String> authorizationGrantTypes;

    private final Set<String> redirectUris;

    private final Set<String> postLogoutRedirectUris;

    private final Set<String> scopes;

    private final Map<String, Object> clientSettings;

    private final Map<String, Object> tokenSettings;

    public static RegisteredClientView fromModel(final RegisteredClientModel model) {
        if (model == null) {
            return null;
        }

        return RegisteredClientView.builder()
                                   .id(model.getId())
                                   .clientId(model.getClientId())
                                   .clientIdIssuedAt(model.getClientIdIssuedAt())
                                   .clientSecret(null)
                                   .clientName(model.getClientName())
                                   .clientAuthenticationMethods(model.getClientAuthenticationMethods())
                                   .authorizationGrantTypes(model.getAuthorizationGrantTypes())
                                   .redirectUris(model.getRedirectUris())
                                   .postLogoutRedirectUris(model.getPostLogoutRedirectUris())
                                   .scopes(model.getScopes())
                                   .clientSettings(model.getClientSettings())
                                   .tokenSettings(model.getTokenSettings())
                                   .build();
    }

    public static RegisteredClientView fromCreatedRegisteredClient(final CreatedRegisteredClient createdRegisteredClient) {
        if (createdRegisteredClient == null || createdRegisteredClient.getRegisteredClient() == null) {
            return null;
        }

        final RegisteredClientModel model = createdRegisteredClient.getRegisteredClient();
        return RegisteredClientView.builder()
                                   .id(model.getId())
                                   .clientId(model.getClientId())
                                   .clientIdIssuedAt(model.getClientIdIssuedAt())
                                   .clientSecret(createdRegisteredClient.getRawClientSecret())
                                   .clientName(model.getClientName())
                                   .clientAuthenticationMethods(model.getClientAuthenticationMethods())
                                   .authorizationGrantTypes(model.getAuthorizationGrantTypes())
                                   .redirectUris(model.getRedirectUris())
                                   .postLogoutRedirectUris(model.getPostLogoutRedirectUris())
                                   .scopes(model.getScopes())
                                   .clientSettings(model.getClientSettings())
                                   .tokenSettings(model.getTokenSettings())
                                   .build();
    }
}
