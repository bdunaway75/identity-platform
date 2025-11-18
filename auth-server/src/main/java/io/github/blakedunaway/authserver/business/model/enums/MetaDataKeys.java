package io.github.blakedunaway.authserver.business.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MetaDataKeys {
    EXPIRES_IN("expires_in"),
    SCOPE("scope"),
    STATE("state"),
    CODE("code"),
    REDIRECT_URI("redirect_uri"),
    CLIENT_ID("client_id"),
    CLIENT_SECRET("client_secret"),
    GRANT_TYPE("grant_type"),
    CODE_CHALLENGE("code_challenge"),
    CODE_CHALLENGE_METHOD("code_challenge_method"),
    CODE_VERIFIER("code_verifier"),
    KID("kid"),
    REVOKED_AT("revoked_at"),
    ISSUER("issuer"),
    SUBJECT("sub"),
    JTI("jti");

    private final String value;
}
