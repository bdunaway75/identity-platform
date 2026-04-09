package io.github.blakedunaway.authserver.business.service;

import io.github.blakedunaway.authserver.business.model.AuthorizationConsent;
import io.github.blakedunaway.authserver.integration.repository.gateway.AuthorizationConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AuthorizationConsentService implements OAuth2AuthorizationConsentService {

    private final AuthorizationConsentRepository authorizationConsentRepository;

    @Override
    public void save(final OAuth2AuthorizationConsent authorizationConsent) {
        Assert.notNull(authorizationConsent, "AuthorizationConsent must not be null");
        authorizationConsentRepository.save(AuthorizationConsent.Builder.fromSpring(authorizationConsent));
    }

    @Override
    public void remove(final OAuth2AuthorizationConsent authorizationConsent) {
        authorizationConsentRepository.remove(AuthorizationConsent.Builder.fromSpring(authorizationConsent));
    }

    @Override
    public OAuth2AuthorizationConsent findById(final String registeredClientId, final String principalName) {
        Assert.hasText(registeredClientId, "registeredClientId must not be empty");
        Assert.hasText(principalName, "principalName must not be empty");
        final AuthorizationConsent consent = authorizationConsentRepository.findByRegisteredClientIdAndPrincipalName(UUID.fromString(registeredClientId), principalName);
        if (consent == null) {
            return null;
        }
        return consent.toSpring();
    }

}
