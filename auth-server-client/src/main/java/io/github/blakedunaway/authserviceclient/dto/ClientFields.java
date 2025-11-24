package io.github.blakedunaway.authserviceclient.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public interface ClientFields {

    String getClientId();

    LocalDateTime getClientIdIssuedAt();

    String getClientSecret();

    LocalDateTime getClientSecretExpiresAt();

    String getClientName();

    Set<String> getClientAuthenticationMethods();

    Set<String> getAuthorizationGrantTypes();

    Set<String> getRedirectUris();

    Set<String> getPostLogoutRedirectUris();

    Set<String> getScopes();

    Map<String, Object> getClientSettings();

    Map<String, Object> getTokenSettings();

}
