package io.github.blakedunaway.authserver.business.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Builder(toBuilder = true)
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisteredClientRequest {

    private String clientId;

    private LocalDateTime clientIdIssuedAt;

    private String clientSecret;

    private LocalDateTime clientSecretExpiresAt;

    private String clientName;

    private Set<String> clientAuthenticationMethods;

    private Set<String> authorizationGrantTypes;

    private Set<String> redirectUris;

    private Set<String> postLogoutRedirectUris;

    private Set<String> scopes;

    private Set<String> authorities;

    private Set<String> roles;

    private Map<String, Object> clientSettings;

    private Map<String, Object> tokenSettings;

    public Map<String, Object> getClientSettings() {
        return clientSettings == null ? new HashMap<>() : new HashMap<>(clientSettings);
    }

    public Map<String, Object> getTokenSettings() {
        return tokenSettings == null ? new HashMap<>() : new HashMap<>(tokenSettings);
    }

    public Set<String> getClientAuthenticationMethods() {
        return clientAuthenticationMethods == null ? Set.of() : Set.copyOf(clientAuthenticationMethods);
    }

    public Set<String> getAuthorizationGrantTypes() {
        return authorizationGrantTypes == null ? Set.of() : Set.copyOf(authorizationGrantTypes);
    }

    public Set<String> getRedirectUris() {
        return redirectUris == null ? Set.of() : Set.copyOf(redirectUris);
    }

    public Set<String> getPostLogoutRedirectUris() {
        return postLogoutRedirectUris == null ? Set.of() : Set.copyOf(postLogoutRedirectUris);
    }

    public Set<String> getScopes() {
        return scopes == null ? Set.of() : Set.copyOf(scopes);
    }

    public Set<String> getAuthorities() {
        return authorities == null ? Set.of() : Set.copyOf(authorities);
    }

    public Set<String> getRoles() {
        return roles == null ? Set.of() : Set.copyOf(roles);
    }

}
