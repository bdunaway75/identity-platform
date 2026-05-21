package io.github.blakedunaway.authserver.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.validation.BindingResult;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@UtilityClass
public class AuthenticationUtility {

    public final Set<ClientAuthenticationMethod> CONF_METHODS = Set.of(
            ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
            ClientAuthenticationMethod.CLIENT_SECRET_POST,
            ClientAuthenticationMethod.CLIENT_SECRET_JWT,
            ClientAuthenticationMethod.PRIVATE_KEY_JWT,
            ClientAuthenticationMethod.TLS_CLIENT_AUTH,
            ClientAuthenticationMethod.SELF_SIGNED_TLS_CLIENT_AUTH
    );

    public final Set<AuthorizationGrantType> AUTH_METHODS = Set.of(
            AuthorizationGrantType.AUTHORIZATION_CODE,
            AuthorizationGrantType.CLIENT_CREDENTIALS,
            AuthorizationGrantType.DEVICE_CODE,
            AuthorizationGrantType.JWT_BEARER,
            AuthorizationGrantType.REFRESH_TOKEN,
            AuthorizationGrantType.TOKEN_EXCHANGE
    );

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);

    public static boolean declaredConfidential(final Set<String> methods) {
        return methods != null
               && methods.stream()
                         .map(ClientAuthenticationMethod::new)
                         .anyMatch(CONF_METHODS::contains);
    }

    public static Map<String, Object> parseJsonKeyWithJsonMapValue(final String key, final Map<String, Object> jsonMap) {
        if (jsonMap == null || jsonMap.isEmpty() || key == null) {
            return new HashMap<>();
        }
        try {
            final Map<String, Object> results = MAPPER.convertValue(jsonMap.get(key), new TypeReference<>() {});
            return results == null ? new HashMap<>() : results;
        } catch (final Exception e) {
            return new HashMap<>();
        }
    }

    public static boolean isArgon2Hash(final String value) {
        return value != null && value.startsWith("$argon2");
    }

    public static void rejectGlobalError(final BindingResult bindingResult, final String code) {
        if (bindingResult == null || StringUtils.isBlank(code)) {
            return;
        }
        bindingResult.reject(code);
    }

    public static void rejectAuthenticationFailure(final BindingResult bindingResult, final AuthenticationException exception) {
        rejectGlobalError(bindingResult, resolveAuthenticationFailureCode(exception));
    }

    public static String resolveAuthenticationFailureCode(final AuthenticationException exception) {
        return switch (exception) {
            case LockedException ignored -> "validation.auth.locked";
            case DisabledException ignored -> "validation.auth.disabled";
            case AccountExpiredException ignored -> "validation.auth.accountExpired";
            case CredentialsExpiredException ignored -> "validation.auth.credentialsExpired";
            case AuthenticationServiceException ignored -> "validation.auth.serviceFailure";
            default -> "validation.auth.failed";
        };
    }

    public static String buildOAuthErrorPath(final String error, final String errorDescription) {
        return UriComponentsBuilder.fromPath("/oauth-error")
                                   .queryParam("error", error)
                                   .queryParam("error_description", errorDescription)
                                   .build()
                                   .encode()
                                   .toUriString();
    }

    public static String buildPlatformLoginRedirect(final String message) {
        return buildLoginRedirect("/platform/login", null, "message", message);
    }

    public static String buildClientLoginRedirect(final String clientId, final String message) {
        return buildLoginRedirect("/login", clientId, "message", message);
    }

    public static String buildPlatformLoginErrorRedirect(final String error) {
        return buildLoginRedirect("/platform/login", null, "error", error);
    }

    public static String buildClientLoginErrorRedirect(final String clientId, final String error) {
        return buildLoginRedirect("/login", clientId, "error", error);
    }

    private static String buildLoginRedirect(final String path,
                                             final String clientId,
                                             final String parameterName,
                                             final String parameterValue) {
        return UriComponentsBuilder.fromPath(path)
                                   .queryParamIfPresent("client_id", Optional.ofNullable(clientId))
                                   .queryParam(parameterName, parameterValue)
                                   .build()
                                   .encode()
                                   .toUriString();
    }

}
