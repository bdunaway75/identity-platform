package io.github.blakedunaway.authserver.mapper;

import io.github.blakedunaway.authserver.business.model.Authorities;
import io.github.blakedunaway.authserver.business.model.AuthorizationConsent;
import io.github.blakedunaway.authserver.integration.entity.AuthoritiesEntity;
import io.github.blakedunaway.authserver.integration.entity.AuthorizationConsentEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.stream.Collectors;

@Component
public class AuthorizationConsentMapper {

    public AuthorizationConsentEntity authorizationConsentToAuthorizationConsentEntity(final AuthorizationConsent authorizationConsent) {
        Assert.notNull(authorizationConsent, "authorizationConsent cannot be null");
        return AuthorizationConsentEntity.create(
                authorizationConsent.getConsentId(),
                authorizationConsent.getRegisteredClientId(),
                authorizationConsent.getPrincipalName(),
                authorizationConsent.isNew(),
                authorizationConsent.getAuthorities()
                                    .stream()
                                    .map(model -> AuthoritiesEntity.create(model.getName()))
                                    .collect(Collectors.toSet())
        );
    }

    public AuthorizationConsent authorizationConsentEntityToAuthorizationConsent(final AuthorizationConsentEntity authorizationConsentEntity) {
        Assert.notNull(authorizationConsentEntity, "authorizationConsent cannot be null");
        return AuthorizationConsent.fromId(authorizationConsentEntity.getConsentId())
                                   .authorities(auths -> auths.addAll(authorizationConsentEntity.getAuthorities()
                                                                                                .stream()
                                                                                                .map(entity -> Authorities.builder()
                                                                                                                          .name(entity.getName()
                                                                                                                                      .toUpperCase())
                                                                                                                          .authorityId(entity.getAuthorityId())
                                                                                                                          .build())
                                                                                                .collect(Collectors.toSet())))
                                   .registeredClientId(authorizationConsentEntity.getRegisteredClientId())
                                   .principalName(authorizationConsentEntity.getPrincipalName())
                                   .isNew(authorizationConsentEntity.isNew())
                                   .build();
    }

}
