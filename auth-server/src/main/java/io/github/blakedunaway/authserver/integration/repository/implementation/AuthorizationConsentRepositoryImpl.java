package io.github.blakedunaway.authserver.integration.repository.implementation;

import io.github.blakedunaway.authserver.business.model.AuthorizationConsent;
import io.github.blakedunaway.authserver.integration.entity.AuthoritiesEntity;
import io.github.blakedunaway.authserver.integration.entity.AuthorizationConsentEntity;
import io.github.blakedunaway.authserver.integration.repository.gateway.AuthorizationConsentRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.AuthoritiesJpaRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.AuthorizationConsentJpaRepository;
import io.github.blakedunaway.authserver.mapper.AuthorizationConsentMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationConsentRepositoryImpl implements AuthorizationConsentRepository {

    private final AuthoritiesJpaRepository authoritiesJpaRepository;

    private final AuthorizationConsentMapper authorizationConsentMapper;

    private final AuthorizationConsentJpaRepository authorizationConsentJpaRepository;

    @Override
    @Transactional
    public AuthorizationConsent save(final AuthorizationConsent authorizationConsent) {
        final AuthorizationConsentEntity entity = authorizationConsentMapper.authorizationConsentToAuthorizationConsentEntity(authorizationConsent);
        final Set<AuthoritiesEntity> authoritiesEntities = authoritiesJpaRepository.findAllByNameIn(entity.getAuthorities()
                                                                                                          .stream()
                                                                                                          .map(AuthoritiesEntity::getName)
                                                                                                          .map(String::toUpperCase)
                                                                                                          .collect(Collectors.toSet()));
        authoritiesEntities.addAll(entity.getAuthorities());
        entity.setAuthorities(authoritiesEntities);
        authoritiesEntities.forEach(auth -> {
            if (auth.getAuthorityId() == null) {
                authoritiesEntities.add(authoritiesJpaRepository.save(auth));
            }
        });
        return authorizationConsentMapper.authorizationConsentEntityToAuthorizationConsent(authorizationConsentJpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void remove(final AuthorizationConsent authorizationConsent) {
        final AuthorizationConsentEntity entity = authorizationConsentJpaRepository.findByRegisteredClientIdAndPrincipalName(authorizationConsent.getRegisteredClientId(),
                                                                                                                             authorizationConsent.getPrincipalName())
                                                                                   .orElse(null);
        if (entity == null) {
            throw new EntityNotFoundException("Error: AuthorizationConsent not found with principal name " + authorizationConsent.getPrincipalName());
        }
        authorizationConsentJpaRepository.deleteByRegisteredClientIdAndPrincipalName(entity.getRegisteredClientId(), entity.getPrincipalName());
    }

    @Override
    public AuthorizationConsent findById(final String registeredClientId, final String principalName) {
        if (registeredClientId == null || principalName == null) {
            return null;
        }
        final AuthorizationConsentEntity entity = authorizationConsentJpaRepository.findByRegisteredClientIdAndPrincipalName(registeredClientId,
                                                                                                                             principalName)
                                                                                   .orElse(null);
        if (entity == null) {
            return null;
        }
        entity.setNew(false);
        return authorizationConsentMapper.authorizationConsentEntityToAuthorizationConsent(entity);
    }

}
