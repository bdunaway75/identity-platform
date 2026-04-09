package io.github.blakedunaway.authserver.mapper;

import io.github.blakedunaway.authserver.business.model.Authority;
import io.github.blakedunaway.authserver.business.model.AuthorizationConsent;
import io.github.blakedunaway.authserver.integration.entity.AuthorityEntity;
import io.github.blakedunaway.authserver.integration.entity.AuthorizationConsentEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.stream.Collectors;

@Component
public class AuthorizationConsentMapper {

    public AuthorizationConsentEntity authorizationConsentToAuthorizationConsentEntity(final AuthorizationConsent authorizationConsent,
                                                                                       final RegisteredClientEntity registeredClient) {
        Assert.notNull(authorizationConsent, "authorizationConsent cannot be null");
        Assert.notNull(registeredClient, "registeredClient cannot be null");
        return AuthorizationConsentEntity.create(
                authorizationConsent.getConsentId(),
                authorizationConsent.getRegisteredClientId(),
                authorizationConsent.getPrincipalName(),
                authorizationConsent.getAuthorities()
                                    .stream()
                                    .map(model -> new AuthorityEntity(model.getName(), registeredClient))
                                    .collect(Collectors.toSet())
        );
    }

    public AuthorizationConsent authorizationConsentEntityToAuthorizationConsent(final AuthorizationConsentEntity authorizationConsentEntity) {
        Assert.notNull(authorizationConsentEntity, "authorizationConsent cannot be null");
        return AuthorizationConsent.fromId(authorizationConsentEntity.getConsentId())
                                   .authorities(auths -> auths.addAll(authorizationConsentEntity.getAuthorities()
                                                                                                .stream()
                                                                                                .map(entity -> Authority.builder()
                                                                                                                        .name(entity.getName()
                                                                                                                                      .toUpperCase())
                                                                                                                        .authorityId(entity.getAuthorityId())
                                                                                                                        .build())
                                                                                                .collect(Collectors.toSet())))
                                   .registeredClientId(authorizationConsentEntity.getRegisteredClientId())
                                   .principalName(authorizationConsentEntity.getPrincipalName())
                                   .build();
    }

}
