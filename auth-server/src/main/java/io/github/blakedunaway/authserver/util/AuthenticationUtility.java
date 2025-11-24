package io.github.blakedunaway.authserver.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class AuthenticationUtility {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);

    public static Map<String, Object> parseJsonKeyWithJsonMapValue(final String key, final Map<String, Object> jsonMap) {
        if (jsonMap == null || jsonMap.isEmpty() || key == null) {
            return new HashMap<>();
        }
        try {
            final Map<String, Object> results = MAPPER.convertValue(jsonMap.get(key), new TypeReference<>() {});
            return results == null ? new HashMap<>() : results;
        } catch (Exception e) {
            //TODO: add logging
            return new HashMap<>();
        }
    }

    public static boolean isArgon2Hash(String value) {
        return value != null && value.startsWith("$argon2");
    }

}
