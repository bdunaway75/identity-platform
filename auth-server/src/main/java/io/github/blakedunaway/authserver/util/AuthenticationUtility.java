package io.github.blakedunaway.authserver.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.experimental.UtilityClass;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.HashMap;
import java.util.Map;
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

}
