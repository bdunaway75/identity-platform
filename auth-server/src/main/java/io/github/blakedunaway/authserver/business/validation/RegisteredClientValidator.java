package io.github.blakedunaway.authserver.business.validation;

import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import io.github.blakedunaway.authserver.util.AuthenticationUtility;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
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

public final class RegisteredClientValidator {
    private static final Set<String> ALLOWED_PROTOCOLS = Set.of("https", "http");
    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "::1");

    private static boolean isPublicClient(final RegisteredClientModel client) {
        final boolean hasSecret = client.getClientSecret() != null && !client.getClientSecret().isBlank();
        return !(hasSecret || AuthenticationUtility.declaredConfidential(client.getClientAuthenticationMethods()));
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
            if (!validRedirectUri.isAbsolute()) {
                return false;
            }

            final String scheme = validRedirectUri.getScheme();
            if (scheme == null || !ALLOWED_PROTOCOLS.contains(scheme.toLowerCase())) {
                return false;
            }

            if (validRedirectUri.getHost() == null || validRedirectUri.getHost().isBlank()) {
                return false;
            }

            if (validRedirectUri.getUserInfo() != null) {
                return false;
            }

            if (validRedirectUri.getFragment() != null) {
                return false;
            }

            if ("http".equalsIgnoreCase(scheme) && !LOOPBACK_HOSTS.contains(validRedirectUri.getHost().toLowerCase())) {
                return false;
            }

            return true;
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    private static void add(final Map<String, List<String>> map, final String key, final String value) {
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    public static Map<String, List<String>> isValid(final RegisteredClientModel client) {
        final Map<String, List<String>> validations = new HashMap<>();

        if (client == null) {
            return validations;
        }

        boolean valid = true;

        if (client.getClientSettings() == null) {
            add(validations, "clientSettings", "client_settings is required");
            valid = false;
        }
        if (client.getTokenSettings() == null) {
            add(validations, "tokenSettings", "token_settings is required");
            valid = false;
        }
        if (client.getAuthorizationGrantTypes() == null || client.getAuthorizationGrantTypes().isEmpty()) {
            add(validations, "authorizationGrantTypes", "at least one authorization_grant_type is required");
            valid = false;
        }
        if (!valid) {
            return validations;
        }

        if (client.getRedirectUris() != null) {
            validateRedirectUris(client, validations);
        }
        if (client.getScopes() != null) {
            validateScopes(client, validations);
        }
        if (client.getPostLogoutRedirectUris() != null) {
            validatePostLogoutRedirectUris(client, validations);
        }

        final Set<AuthorizationGrantType> normalizedAuthGrants = client.getAuthorizationGrantTypes()
                                                                       .stream()
                                                                       .map(AuthorizationGrantType::new)
                                                                       .collect(Collectors.toSet());
        final boolean isPublic = isPublicClient(client);
        final boolean hasAuthCode = normalizedAuthGrants.contains(AuthorizationGrantType.AUTHORIZATION_CODE);
        final boolean hasClientCreds = normalizedAuthGrants.contains(AuthorizationGrantType.CLIENT_CREDENTIALS);
        final boolean requirePkce = ClientSettings.withSettings(client.getClientSettings()).build().isRequireProofKey();

        if (!AuthenticationUtility.AUTH_METHODS.containsAll(normalizedAuthGrants)) {
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

    private static boolean validateScopes(final RegisteredClientModel client, final Map<String, List<String>> map) {
        if (CollectionUtils.isEmpty(client.getScopes())) {
            return true;
        }
        boolean valid = true;
        for (final String scope : client.getScopes()) {
            if (!validateScope(scope)) {
                add(map, "scopes", "scope \"" + scope + "\" contains invalid characters");
                if (valid) {
                    valid = false;
                }

            }
        }
        return valid;
    }

    private static boolean validateRedirectUris(final RegisteredClientModel client, final Map<String, List<String>> map) {
        if (CollectionUtils.isEmpty(client.getRedirectUris())) {
            return true;
        }

        boolean valid = true;
        for (final String redirectUri : client.getRedirectUris()) {
            if (!validateRedirectUri(redirectUri)) {
                add(map, "redirectUris", redirectUri + " contains invalid characters");
                if (valid) {
                    valid = false;
                }
            }
        }
        return valid;
    }

    private static boolean validatePostLogoutRedirectUris(final RegisteredClientModel client, final Map<String, List<String>> map) {
        if (CollectionUtils.isEmpty(client.getPostLogoutRedirectUris())) {
            return true;
        }
        boolean valid = true;
        for (final String postLogoutRedirectUri : client.getPostLogoutRedirectUris()) {
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
