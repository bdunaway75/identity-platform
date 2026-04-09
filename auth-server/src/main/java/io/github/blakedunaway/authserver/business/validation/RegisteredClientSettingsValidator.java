package io.github.blakedunaway.authserver.business.validation;

import java.util.Map;
import java.util.Set;

public class RegisteredClientSettingsValidator {

    private static final String REQUIRE_PROOF_KEY = "settings.client.require-proof-key";
    private static final String REQUIRE_AUTHORIZATION_CONSENT = "settings.client.require-authorization-consent";

    private static final Set<String> TOKEN_KEYS = Set.of(
            "accessTokenTimeToLive",
            "refreshTokenTimeToLive",
            "authorizationCodeTimeToLive",
            "reuseRefreshTokens"
    );

    private static final Set<String> CLIENT_KEYS = Set.of(
            REQUIRE_PROOF_KEY,
            REQUIRE_AUTHORIZATION_CONSENT
    );

    public static void validateTokenSettings(final Map<String, Object> map) {
        if (map == null) {
            return;
        }

        for (final String key : map.keySet()) {
            if (!TOKEN_KEYS.contains(key)) {
                throw new IllegalArgumentException("Invalid tokenSettings key: " + key);
            }
        }

        validateNumber(map, "accessTokenTimeToLiveMinutes");
        validateNumber(map, "refreshTokenTimeToLiveMinutes");
        validateNumber(map, "authorizationCodeTimeToLiveMinutes");
        validateBoolean(map, "reuseRefreshTokens");
    }

    public static void validateClientSettings(final Map<String, Object> map) {
        if (map == null) {
            return;
        }

        for (final String key : map.keySet()) {
            if (!CLIENT_KEYS.contains(key)) {
                throw new IllegalArgumentException("Invalid clientSettings key: " + key);
            }
        }

        validateBoolean(map, REQUIRE_PROOF_KEY);
        validateBoolean(map, REQUIRE_AUTHORIZATION_CONSENT);
    }

    private static void validateNumber(final Map<String, Object> map, final String key) {
        final Object value = map.get(key);
        if (value == null) {
            return;
        }

        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(key + " must be a number");
        }

        final long val = ((Number) value).longValue();
        if (val <= 0) {
            throw new IllegalArgumentException(key + " must be > 0");
        }
    }

    private static void validateBoolean(final Map<String, Object> map, final String key) {
        final Object value = map.get(key);
        if (value == null) {
            return;
        }

        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException(key + " must be a boolean");
        }
    }

}
