package io.github.blakedunaway.authserver.integration.repository.gateway;

import io.github.blakedunaway.authserver.business.model.AuthorizationConsent;

public interface AuthorizationConsentRepository {

    AuthorizationConsent save(final AuthorizationConsent authorizationConsent);

    void remove(final AuthorizationConsent authorizationConsent);

    AuthorizationConsent findById(final String registeredClientId, String principalName);

}
