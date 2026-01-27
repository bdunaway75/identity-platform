package io.github.blakedunaway.authserver.business.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public enum ClientAuthenticationMethodResolver {
    CLIENT_SECRET_BASIC("client_secret_basic", ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
    CLIENT_SECRET_POST("client_secret_post", ClientAuthenticationMethod.CLIENT_SECRET_POST),
    CLIENT_SECRET_JWT("client_secret_jwt", ClientAuthenticationMethod.CLIENT_SECRET_JWT),
    PRIVATE_KEY_JWT("private_key_jwt", ClientAuthenticationMethod.PRIVATE_KEY_JWT),
    TLS_CLIENT_AUTH("tls_client_auth", ClientAuthenticationMethod.TLS_CLIENT_AUTH),
    NONE("none", ClientAuthenticationMethod.NONE),
    SELF_SIGNED_TLS_CLIENT_AUTH("self_signed_tls_client_auth",  ClientAuthenticationMethod.SELF_SIGNED_TLS_CLIENT_AUTH);

    private static final Map<String, ClientAuthenticationMethod> BY_NAME = new HashMap<>();

    private final String name;

    private final ClientAuthenticationMethod associatedOauthMethod;

    static {
        for (ClientAuthenticationMethodResolver resolver : ClientAuthenticationMethodResolver.values()) {
            BY_NAME.put(resolver.getName(), resolver.getAssociatedOauthMethod());
        }
    }

    public static ClientAuthenticationMethod resolve(final String name) {
        final ClientAuthenticationMethod result = BY_NAME.get(name);
        if (result == null) {
            throw new IllegalArgumentException("No such authentication method: " + name);
        }
        return result;
    }
}
