package io.github.blakedunaway.authserver.business.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

@Getter
@RequiredArgsConstructor
public enum AuthorizationGrantTypeInternal {
    REFRESH_TOKEN("refresh_token", AuthorizationGrantType.REFRESH_TOKEN),
    AUTHORIZATION_CODE("authorization_code",  AuthorizationGrantType.AUTHORIZATION_CODE),
    CLIENT_CREDENTIALS("client_credentials",  AuthorizationGrantType.CLIENT_CREDENTIALS),;

    private final String wireName;

    private final AuthorizationGrantType authorizationGrantType;

    public static AuthorizationGrantTypeInternal findByName(final String wireName) {
        for (AuthorizationGrantTypeInternal authorizationGrantTypeInternal : AuthorizationGrantTypeInternal.values()) {
            if (authorizationGrantTypeInternal.getWireName().equals(wireName)) {
                return authorizationGrantTypeInternal;
            }
        }
        throw  new IllegalArgumentException("AuthorizationGrantTypeInternal of type " + wireName +" not found");
    }
}
