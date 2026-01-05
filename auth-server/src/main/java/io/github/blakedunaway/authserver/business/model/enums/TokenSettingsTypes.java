package io.github.blakedunaway.authserver.business.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.oauth2.server.authorization.settings.ConfigurationSettingNames;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public enum TokenSettingsTypes {
    ACCESS_TOKEN_TTL(
            ConfigurationSettingNames.Token.ACCESS_TOKEN_TIME_TO_LIVE,
            Duration.class
    ),
    REFRESH_TOKEN_TTL(
            ConfigurationSettingNames.Token.REFRESH_TOKEN_TIME_TO_LIVE,
            Duration.class
    ),
    AUTHORIZATION_CODE_TTL(
            ConfigurationSettingNames.Token.AUTHORIZATION_CODE_TIME_TO_LIVE,
            Duration.class
    ),
    DEVICE_CODE_TTL(
            ConfigurationSettingNames.Token.DEVICE_CODE_TIME_TO_LIVE,
            Duration.class
    ),
    REUSE_REFRESH_TOKENS(
            ConfigurationSettingNames.Token.REUSE_REFRESH_TOKENS,
            Boolean.class
    ),
    ID_TOKEN_SIGNATURE_ENABLED(
            ConfigurationSettingNames.Token.ID_TOKEN_SIGNATURE_ALGORITHM,
            String.class
    );

    private static final Map<String, TokenSettingsTypes> BY_NAME = new HashMap<>();

    private final String settingName;

    private final Class<?> settingType;

    static {
        for (TokenSettingsTypes type : TokenSettingsTypes.values()) {
            BY_NAME.put(type.getSettingName(), type);
        }
    }

    public Object coerce(final Object value) {
        if (value == null) {
            return null;
        }
        if (this.getSettingType().isInstance(value)) {
            return value;
        }
        if (this.getSettingType() == Duration.class) {
            if (value instanceof Number num) {
                return Duration.ofSeconds(num.longValue());
            }
            if (value instanceof String s) {
                return Duration.parse(s);
            }
        }

        if (this.getSettingType() == Boolean.class) {
            if (value instanceof Boolean b) {
                return b;
            }
            if (value instanceof String s) {
                return Boolean.parseBoolean(s);
            }
        }

        if (this.getSettingType() == String.class) {
            return value.toString();
        }

        throw new IllegalArgumentException(
                "Cannot coerce value [" + value + "] into [" + this.getSettingType().getSimpleName() + "]"
                        + " for setting [" + this.getSettingName() + "]"
        );
    }

    public static TokenSettingsTypes fromSettingName(final String name) {
        final TokenSettingsTypes result = BY_NAME.get(name);
        if (result == null) {
            throw new IllegalArgumentException("Unknown setting name [" + name + "]");
        }
        return result;
    }

    public static Map<String, Object> normalize(final Map<String, Object> rawSettings) {
        final Map<String, Object> normalized = new HashMap<>();
        rawSettings.forEach((key, value) -> {
            final TokenSettingsTypes tsType = fromSettingName(key);
                normalized.put(key, tsType.coerce(value));
        });

        return normalized;
    }
}
