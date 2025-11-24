package io.github.blakedunaway.authserviceclient.dto;


import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Jacksonized
@Builder(toBuilder = true)
@Value
public class RegisteredClientDto implements ClientFields {

    String clientId;

    LocalDateTime clientIdIssuedAt;

    String clientSecret;

    LocalDateTime clientSecretExpiresAt;

    String clientName;

    Set<String> clientAuthenticationMethods;

    Set<String> authorizationGrantTypes;

    Set<String> redirectUris;

    Set<String> postLogoutRedirectUris;

    Set<String> scopes;

    Map<String, Object> clientSettings;

    Map<String, Object> tokenSettings;

}
