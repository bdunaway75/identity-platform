package io.github.blakedunaway.authserver.integration.repository.gateway;

import io.github.blakedunaway.authserver.business.model.AuthorizationConsent;

import java.util.UUID;

public interface AuthorizationConsentRepository {

    AuthorizationConsent save(final AuthorizationConsent authorizationConsent);

    void remove(final AuthorizationConsent authorizationConsent);

    AuthorizationConsent findById(final UUID registeredClientId, String principalName);

}
