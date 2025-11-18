package io.github.blakedunaway.authserver.business.service;

import lombok.experimental.UtilityClass;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@UtilityClass
public class TokenHasher {

    private final SecretKeySpec key;

    @Value(value = "${security.token.pepper-base64}")
    private String b64;

    static {
        if (b64.isBlank()) {
            throw new IllegalStateException("security.token.pepper-base64 missing");
        }
        final byte[] raw = decodeAnyBase64(b64.trim());
        if (raw.length < 32) {
            throw new IllegalStateException("pepper too short: " + raw.length + " bytes; need >= 32");
        }
        key = new javax.crypto.spec.SecretKeySpec(raw, "HmacSHA256");

    }

    private static byte[] decodeAnyBase64(final String s) {
        final String padded = pad4(s);
        try {
            return Base64.getUrlDecoder().decode(padded);
        } catch (IllegalArgumentException ignore) { /* fall through */ }
        // normalize to standard and try again
        final String std = pad4(s.replace('-', '+').replace('_', '/'));
        try {
            return java.util.Base64.getDecoder().decode(std);
        } catch (IllegalArgumentException e) {
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
        int mod = s.length() % 4;
        return mod == 0 ? s : (s + "====".substring(mod));
    }

    public String hmacCurrent(final String tokenValue) {
        try {
            if (isHmacSha256Base64Url(tokenValue)) {
                return tokenValue;
            }
            final javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(key);
            final byte[] digest = mac.doFinal(tokenValue.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failure", e);
        }
    }

}

