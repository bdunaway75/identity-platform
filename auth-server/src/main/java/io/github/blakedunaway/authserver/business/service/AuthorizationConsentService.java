package io.github.blakedunaway.authserver.business.service;

import io.github.blakedunaway.authserver.business.model.AuthorizationConsent;
import io.github.blakedunaway.authserver.integration.repository.gateway.AuthorizationConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@RequiredArgsConstructor
@Service
public class AuthorizationConsentService implements OAuth2AuthorizationConsentService {

    private final AuthorizationConsentRepository authorizationConsentRepository;

    @Override
    public void save(final OAuth2AuthorizationConsent authorizationConsent) {
        Assert.notNull(authorizationConsent, "AuthorizationConsent must not be null");
        Assert.hasText(authorizationConsent.getRegisteredClientId(), "RegisteredClientId must not be empty");
        Assert.hasText(authorizationConsent.getPrincipalName(), "PrincipalName must not be empty");
        AuthorizationConsent.Builder model = AuthorizationConsent.Builder.fromSpring(authorizationConsent);
        final AuthorizationConsent persistedConsent = authorizationConsentRepository.findById(model.getRegisteredClientId(),
                                                                                              model.getPrincipalName());
        if (persistedConsent != null) {
            model = model.isNew(false).consentId(persistedConsent.getConsentId());
        }
        authorizationConsentRepository.save(model.build());
    }

    @Override
    public void remove(final OAuth2AuthorizationConsent authorizationConsent) {
        authorizationConsentRepository.remove(AuthorizationConsent.Builder.fromSpring(authorizationConsent).build());
    }

    @Override
    public OAuth2AuthorizationConsent findById(final String registeredClientId, final String principalName) {
        Assert.hasText(registeredClientId, "registeredClientId must not be empty");
        Assert.hasText(principalName, "principalName must not be empty");
        final AuthorizationConsent consent = authorizationConsentRepository.findById(registeredClientId, principalName);
        if (consent == null) {
            return null;
        }
        return consent.toSpring();
    }

}
