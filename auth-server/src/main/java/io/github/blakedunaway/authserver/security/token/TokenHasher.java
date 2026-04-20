package io.github.blakedunaway.authserver.security.token;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public final class TokenHasher {

    private static volatile SecretKeySpec key;

    @Value("${security.token.pepper-base64}")
    private String pepperBase64;

    @PostConstruct
    void initialize() {
        key = buildKey(pepperBase64);
    }

    private static byte[] decodeAnyBase64(final String s) {
        final String padded = pad4(s);
        try {
            return Base64.getUrlDecoder().decode(padded);
        } catch (Exception e) {
            throw new IllegalStateException("pepper not valid base64/base64url", e);
        }
    }

    public static boolean isHmacSha256Base64Url(final String val) {
        if (val == null || val.length() != 43) {
            return false;
        }
        try {
            final byte[] decoded = Base64.getUrlDecoder().decode(val);
            return decoded.length == 32;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String pad4(final String s) {
        final int mod = s.length() % 4;
        return mod == 0 ? s : (s + "====".substring(mod));
    }

    public static String hmacCurrent(final String tokenValue) {
        try {
            if (isHmacSha256Base64Url(tokenValue)) {
                return tokenValue;
            }
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(currentKey());
            final byte[] digest = mac.doFinal(tokenValue.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failure", e);
        }
    }

    private static SecretKeySpec currentKey() {
        final SecretKeySpec resolvedKey = key;
        if (resolvedKey != null) {
            return resolvedKey;
        }
        throw new IllegalStateException("TokenHasher is not configured. Ensure security.token.pepper-base64 is injected.");
    }

    private static SecretKeySpec buildKey(final String pepperBase64) {
        if (pepperBase64 == null || pepperBase64.isBlank()) {
            throw new IllegalStateException("security.token.pepper-base64 missing");
        }

        final byte[] raw = decodeAnyBase64(pepperBase64.trim());
        if (raw.length < 32) {
            throw new IllegalStateException("pepper too short: " + raw.length + " bytes; need >= 32");
        }
        return new SecretKeySpec(raw, "HmacSHA256");
    }

}

