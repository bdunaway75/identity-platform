package io.github.blakedunaway.authserver.business.validation;

import io.github.blakedunaway.authserviceclient.dto.ClientFields;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.util.CollectionUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ClientRegistrationValidator {

    private static final Set<ClientAuthenticationMethod> CONF_METHODS = Set.of(
            ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
            ClientAuthenticationMethod.CLIENT_SECRET_POST,
            ClientAuthenticationMethod.CLIENT_SECRET_JWT,
            ClientAuthenticationMethod.PRIVATE_KEY_JWT,
            ClientAuthenticationMethod.TLS_CLIENT_AUTH,
            ClientAuthenticationMethod.SELF_SIGNED_TLS_CLIENT_AUTH
    );

    private static final Set<AuthorizationGrantType> AUTH_METHODS = Set.of(
            AuthorizationGrantType.AUTHORIZATION_CODE,
            AuthorizationGrantType.CLIENT_CREDENTIALS,
            AuthorizationGrantType.DEVICE_CODE,
            AuthorizationGrantType.JWT_BEARER,
            AuthorizationGrantType.REFRESH_TOKEN,
            AuthorizationGrantType.TOKEN_EXCHANGE
    );

    private static boolean isPublicClient(final ClientFields clientFields) {
        final boolean hasSecret = clientFields.getClientSecret() != null && !clientFields.getClientSecret().isBlank();
        final boolean declaresConfidential = clientFields.getClientAuthenticationMethods() != null
                                             && clientFields.getClientAuthenticationMethods()
                                                            .stream()
                                                            .map(ClientAuthenticationMethod::new)
                                                            .anyMatch(CONF_METHODS::contains);
        return !(hasSecret || declaresConfidential);
    }

    private static boolean validateScope(final String scope) {
        return scope == null || scope.chars()
                                     .allMatch(c -> withinTheRangeOf(c, 0x21, 0x21) || withinTheRangeOf(c, 0x23, 0x5B)
                                                    || withinTheRangeOf(c, 0x5D, 0x7E));
    }

    private static boolean withinTheRangeOf(final int c, final int min, final int max) {
        return c >= min && c <= max;
    }

    private static boolean isBlank(final String s) {
        return s == null || s.isBlank();
    }

    private static boolean validateRedirectUri(final String redirectUri) {
        try {
            final URI validRedirectUri = new URI(redirectUri);
            return validRedirectUri.getFragment() == null;
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    private static void add(final Map<String, List<String>> map, final String key, final String value) {
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    public static Map<String, List<String>> isValid(final ClientFields clientFields) {
        final Map<String, List<String>> validations = new HashMap<>();

        if (clientFields == null) {
            return validations;
        }

        boolean valid = true;

        if (isBlank(clientFields.getClientName())) {
            add(validations, "clientName", "client_name is required");
            valid = false;
        }
        if (isBlank(clientFields.getClientId())) {
            add(validations, "clientId", "client_id is required");
            valid = false;
        }
        if (clientFields.getClientSettings() == null) {
            add(validations, "clientSettings", "client_settings is required");
            valid = false;
        }
        if (clientFields.getTokenSettings() == null) {
            add(validations, "tokenSettings", "token_settings is required");
            valid = false;
        }
        if (clientFields.getAuthorizationGrantTypes() == null || clientFields.getAuthorizationGrantTypes().isEmpty()) {
            add(validations, "authorizationGrantTypes", "at least one authorization_grant_type is required");
            valid = false;
        }
        if (!valid) {
            return validations;
        }

        if (clientFields.getRedirectUris() != null) {
            validateRedirectUris(clientFields, validations);
        }
        if (clientFields.getScopes() != null) {
            validateScopes(clientFields, validations);
        }
        if (clientFields.getPostLogoutRedirectUris() != null) {
            validatePostLogoutRedirectUris(clientFields, validations);
        }

        final Set<AuthorizationGrantType> normalizedAuthGrants = clientFields.getAuthorizationGrantTypes()
                                                                             .stream()
                                                                             .map(AuthorizationGrantType::new)
                                                                             .collect(Collectors.toSet());
        final boolean isPublic = isPublicClient(clientFields);
        final boolean hasAuthCode = normalizedAuthGrants.contains(AuthorizationGrantType.AUTHORIZATION_CODE);
        final boolean hasClientCreds = normalizedAuthGrants.contains(AuthorizationGrantType.CLIENT_CREDENTIALS);
        final boolean requirePkce = ClientSettings.withSettings(clientFields.getClientSettings()).build().isRequireProofKey();

        if (!AUTH_METHODS.containsAll(normalizedAuthGrants)) {
            add(validations, "authorizationGrantTypes", "at least one invalid authorization_grant_type found.");
        }
        if (isPublic) {
            // Public cannot use client_credentials
            if (hasClientCreds) {
                add(validations, "authorizationGrantTypes", "public clients cannot use client_credentials");
            }
            // If public uses authorization_code, PKCE must be required
            if (hasAuthCode && !requirePkce) {
                add(validations, "clientSettings.requireProofKey", "public clients using authorization_code must require PKCE");
            }
            // PKCE only applies to authorization_code
        } else {
            // Confidential/private: PKCE is not applicable to client_credentials
            if (hasClientCreds && requirePkce) {
                add(validations, "clientSettings.requireProofKey", "PKCE is not applicable to client_credentials");
            }
        }
        if (requirePkce && !hasAuthCode) {
            add(validations, "clientSettings.requireProofKey", "PKCE is only applicable to authorization_code");
        }
        return validations;
    }

    private static boolean validateScopes(final ClientFields clientFields, final Map<String, List<String>> map) {
        if (CollectionUtils.isEmpty(clientFields.getScopes())) {
            return true;
        }
        boolean valid = true;
        for (final String scope : clientFields.getScopes()) {
            if (!validateScope(scope)) {
                add(map, "scopes", "scope \"" + scope + "\" contains invalid characters");
                if (valid) {
                    valid = false;
                }

            }
        }
        return valid;
    }

    private static boolean validateRedirectUris(final ClientFields clientFields, final Map<String, List<String>> map) {
        if (CollectionUtils.isEmpty(clientFields.getRedirectUris())) {
            return true;
        }

        boolean valid = true;
        for (final String redirectUri : clientFields.getRedirectUris()) {
            if (!validateRedirectUri(redirectUri)) {
                add(map, "redirectUris", redirectUri + " contains invalid characters");
                if (valid) {
                    valid = false;
                }
            }
        }
        return valid;
    }

    private static boolean validatePostLogoutRedirectUris(final ClientFields clientFields, final Map<String, List<String>> map) {
        if (CollectionUtils.isEmpty(clientFields.getPostLogoutRedirectUris())) {
            return true;
        }
        boolean valid = true;
        for (final String postLogoutRedirectUri : clientFields.getPostLogoutRedirectUris()) {
            if (!validateRedirectUri(postLogoutRedirectUri)) {
                add(map, "postLogoutRedirectUris", postLogoutRedirectUri + " contains invalid characters");
                if (valid) {
                    valid = false;
                }
            }
        }
        return valid;
    }

}
