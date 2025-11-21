package io.github.blakedunaway.authserver.business.validation;

import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.CollectionUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public final class ClientRegistrationValidator implements ConstraintValidator<ValidClient, RegisteredClientModel> {

    private static final Set<ClientAuthenticationMethod> CONF_METHODS = Set.of(
            ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
            ClientAuthenticationMethod.CLIENT_SECRET_POST,
            ClientAuthenticationMethod.CLIENT_SECRET_JWT,
            ClientAuthenticationMethod.PRIVATE_KEY_JWT,
            ClientAuthenticationMethod.TLS_CLIENT_AUTH,
            ClientAuthenticationMethod.SELF_SIGNED_TLS_CLIENT_AUTH
    );

    private static boolean isPublicClient(final RegisteredClientModel c) {
        final boolean hasSecret = c.getClientSecret() != null && !c.getClientSecret().isBlank();
        final boolean declaresConfidential = c.getClientAuthenticationMethods() != null
                                             && c.getClientAuthenticationMethods().stream().anyMatch(CONF_METHODS::contains);
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

    private static void add(final ConstraintValidatorContext ctx, final String node, final String msg) {
        ctx.buildConstraintViolationWithTemplate(msg)
           .addPropertyNode(node)
           .addConstraintViolation();
    }

    private static boolean validateRedirectUri(final String redirectUri) {
        try {
            final URI validRedirectUri = new URI(redirectUri);
            return validRedirectUri.getFragment() == null;
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    @Override
    public boolean isValid(final RegisteredClientModel clientModel, final ConstraintValidatorContext ctx) {
        if (clientModel == null) {
            return true;
        }

        boolean valid = true;
        ctx.disableDefaultConstraintViolation();

        if (isBlank(clientModel.getClientName())) {
            add(ctx, "clientName", "client_name is required");
            valid = false;
        }
        if (isBlank(clientModel.getClientId())) {
            add(ctx, "clientId", "client_id is required");
            valid = false;
        }
        if (clientModel.getClientSettings() == null) {
            add(ctx, "clientSettings", "client_settings is required");
            valid = false;
        }
        if (clientModel.getTokenSettings() == null) {
            add(ctx, "tokenSettings", "token_settings is required");
            valid = false;
        }
        if (clientModel.getAuthorizationGrantTypes() == null || clientModel.getAuthorizationGrantTypes().isEmpty()) {
            add(ctx, "authorizationGrantTypes", "at least one authorization_grant_type is required");
            valid = false;
        }
        if (!valid) {
            return false;
        }

        if (clientModel.getRedirectUris() != null) {
            valid = validateRedirectUris(clientModel, ctx);
        }
        if (clientModel.getScopes() != null) {
            valid = validateScopes(clientModel, ctx);
        }
        if (clientModel.getPostLogoutRedirectUris() != null) {
            valid = validatePostLogoutRedirectUris(clientModel, ctx);
        }

        final boolean isPublic = isPublicClient(clientModel);

        final boolean hasAuthCode = clientModel.getAuthorizationGrantTypes().contains(AuthorizationGrantType.AUTHORIZATION_CODE);
        final boolean hasClientCreds = clientModel.getAuthorizationGrantTypes().contains(AuthorizationGrantType.CLIENT_CREDENTIALS);
        final boolean requirePkce = clientModel.getClientSettings().isRequireProofKey();

        if (isPublic) {
            // Public cannot use client_credentials
            if (hasClientCreds) {
                add(ctx, "authorizationGrantTypes", "public clients cannot use client_credentials");
                valid = false;
            }
            // If public uses authorization_code, PKCE must be required
            if (hasAuthCode && !requirePkce) {
                add(ctx, "clientSettings.requireProofKey", "public clients using authorization_code must require PKCE");
                valid = false;
            }
            // PKCE only applies to authorization_code
            if (requirePkce && !hasAuthCode) {
                add(ctx, "clientSettings.requireProofKey", "PKCE is only applicable to authorization_code");
                valid = false;
            }
        } else {
            // Confidential/private: PKCE is not applicable to client_credentials
            if (hasClientCreds && requirePkce) {
                add(ctx, "clientSettings.requireProofKey", "PKCE is not applicable to client_credentials");
                valid = false;
            }
            // If PKCE required but not using authorization_code, reject
            if (requirePkce && !hasAuthCode) {
                add(ctx, "clientSettings.requireProofKey", "PKCE is only applicable to authorization_code");
                valid = false;
            }
        }
        return valid;
    }

    private boolean validateScopes(final RegisteredClientModel c, final ConstraintValidatorContext ctx) {
        if (CollectionUtils.isEmpty(c.getScopes())) {
            return true;
        }
        boolean valid = true;
        for (final String scope : c.getScopes()) {
            if (!validateScope(scope)) {
                add(ctx, "scopes", "scope \"" + scope + "\" contains invalid characters");
                if (valid) {
                    valid = false;
                }

            }
        }
        return valid;
    }

    private boolean validateRedirectUris(final RegisteredClientModel c, final ConstraintValidatorContext ctx) {
        if (CollectionUtils.isEmpty(c.getRedirectUris())) {
            return true;
        }

        boolean valid = true;
        for (final String redirectUri : c.getRedirectUris()) {
            if (!validateRedirectUri(redirectUri)) {
                add(ctx, "redirectUris", redirectUri + " contains invalid characters");
                if (valid) {
                    valid = false;
                }
            }
        }
        return valid;
    }

    private boolean validatePostLogoutRedirectUris(final RegisteredClientModel c, final ConstraintValidatorContext ctx) {
        if (CollectionUtils.isEmpty(c.getPostLogoutRedirectUris())) {
            return true;
        }
        boolean valid = true;
        for (final String postLogoutRedirectUri : c.getPostLogoutRedirectUris()) {
            if (!validateRedirectUri(postLogoutRedirectUri)) {
                add(ctx, "redirectUris", postLogoutRedirectUri + " contains invalid characters");
                if (valid) {
                    valid = false;
                }
            }
        }
        return valid;
    }

}
