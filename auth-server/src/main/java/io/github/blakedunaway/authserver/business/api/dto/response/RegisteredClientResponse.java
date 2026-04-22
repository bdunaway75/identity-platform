package io.github.blakedunaway.authserver.business.api.dto.response;

import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class RegisteredClientResponse {

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

    private final Set<String> authorities;

    private final Set<String> roles;

    private final Map<String, Object> clientSettings;

    private final Map<String, Object> tokenSettings;

    public static RegisteredClientResponse fromModel(final RegisteredClientModel model) {
        if (model == null) {
            return null;
        }

        return RegisteredClientResponse.builder()
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
                                       .authorities(model.getAuthorities())
                                       .roles(model.getRoles())
                                       .clientSettings(model.getClientSettings())
                                       .tokenSettings(model.getTokenSettings())
                                       .build();
    }

    public static RegisteredClientResponse fromCreatedModel(final RegisteredClientModel model) {
        if (model == null) {
            return null;
        }

        return RegisteredClientResponse.builder()
                                       .id(model.getId())
                                       .clientId(model.getClientId())
                                       .clientIdIssuedAt(model.getClientIdIssuedAt())
                                       .clientSecret(model.getClientSecret())
                                       .clientName(model.getClientName())
                                       .clientAuthenticationMethods(model.getClientAuthenticationMethods())
                                       .authorizationGrantTypes(model.getAuthorizationGrantTypes())
                                       .redirectUris(model.getRedirectUris())
                                       .postLogoutRedirectUris(model.getPostLogoutRedirectUris())
                                       .scopes(model.getScopes())
                                       .authorities(model.getAuthorities())
                                       .roles(model.getRoles())
                                       .clientSettings(model.getClientSettings())
                                       .tokenSettings(model.getTokenSettings())
                                       .build();
    }
}
