package io.github.blakedunaway.authserver.business.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.oauth2.server.authorization.settings.ConfigurationSettingNames;

import java.time.Duration;
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

    private final String settingName;

    private final Class<?> settingType;

    public Object coerce(final Object value) {
        if (value == null) {
            return null;
        }
        if (settingType.isInstance(value)) {
            return value;
        }
        if (settingType == Duration.class) {
            if (value instanceof Number num) {
                return Duration.ofSeconds(num.longValue());
            }
            if (value instanceof String s) {
                return Duration.parse(s);
            }
        }

        if (settingType == Boolean.class) {
            if (value instanceof Boolean b) {
                return b;
            }
            if (value instanceof String s) {
                return Boolean.parseBoolean(s);
            }
        }

        if (settingType == String.class) {
            return value.toString();
        }

        throw new IllegalArgumentException(
                "Cannot coerce value [" + value + "] into [" + settingType.getSimpleName() + "]"
                        + " for setting [" + settingName + "]"
        );
    }

    public static TokenSettingsTypes fromSettingName(final String name) {
        for (final TokenSettingsTypes type : values()) {
            if (type.settingName.equals(name)) {
                return type;
            }
        }
        return null;
    }

    public static Map<String, Object> normalize(final Map<String, Object> raw) {
        final Map<String, Object> normalized = new LinkedHashMap<>(raw);
        raw.forEach((key, value) -> {
            final TokenSettingsTypes tsType = fromSettingName(key);
            if (tsType != null) {
                normalized.put(key, tsType.coerce(value));
            }
            else {
                normalized.put(key, value); // maybe custom non SAS
            }
        });

        return normalized;
    }
}
