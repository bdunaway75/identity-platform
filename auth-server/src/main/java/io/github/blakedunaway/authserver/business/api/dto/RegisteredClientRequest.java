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

    private Map<String, Object> clientSettings;

    private Map<String, Object> tokenSettings;

    public Map<String, Object> getClientSettings() {
        return new  HashMap<>(clientSettings);
    }

    public Map<String, Object> getTokenSettings() {
        return new HashMap<>(this.tokenSettings);
    }

}
