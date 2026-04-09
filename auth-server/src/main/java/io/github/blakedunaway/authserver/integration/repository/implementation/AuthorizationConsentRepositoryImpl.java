package io.github.blakedunaway.authserver.integration.repository.implementation;

import io.github.blakedunaway.authserver.business.model.AuthorizationConsent;
import io.github.blakedunaway.authserver.integration.entity.AuthorityEntity;
import io.github.blakedunaway.authserver.integration.entity.AuthorizationConsentEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientEntity;
import io.github.blakedunaway.authserver.integration.repository.gateway.AuthorizationConsentRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.AuthorityJpaRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.AuthorizationConsentJpaRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.RegisterClientJpaRepository;
import io.github.blakedunaway.authserver.mapper.AuthorizationConsentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// TODO [bdunaway][2026-Jan-30]: Implement purge at some point.
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationConsentRepositoryImpl implements AuthorizationConsentRepository {

    private final AuthorityJpaRepository authorityJpaRepository;

    private final AuthorizationConsentMapper authorizationConsentMapper;

    private final AuthorizationConsentJpaRepository authorizationConsentJpaRepository;

    private final RegisterClientJpaRepository registerClientJpaRepository;

    @Override
    @Transactional
    public AuthorizationConsent save(final AuthorizationConsent authorizationConsent) {
        final RegisteredClientEntity registeredClient = registerClientJpaRepository.findById(authorizationConsent.getRegisteredClientId())
                                                                                   .orElseThrow();
        final AuthorizationConsentEntity entity =
                authorizationConsentMapper.authorizationConsentToAuthorizationConsentEntity(authorizationConsent, registeredClient);
        authorizationConsentJpaRepository.findByRegisteredClientIdAndPrincipalName(entity.getRegisteredClientId(), entity.getPrincipalName())
                                         .ifPresent(found -> {
                                             entity.setConsentId(found.getConsentId());
                                         });
        attachManagedAuthoritiesToUnmanagedConsentEntity(entity);
        final AuthorizationConsentEntity savedAndManagedEntity = authorizationConsentJpaRepository.save(entity);
        return authorizationConsentMapper.authorizationConsentEntityToAuthorizationConsent(savedAndManagedEntity);
    }

    private void attachManagedAuthoritiesToUnmanagedConsentEntity(AuthorizationConsentEntity nonManagedEntity) {
        final Set<AuthorityEntity> attachedAuthorityEntities =
                authorityJpaRepository.findAllByRegisteredClient_RegisteredClientIdAndNameIn(
                        nonManagedEntity.getRegisteredClientId(),
                        nonManagedEntity.getAuthorities()
                                        .stream()
                                        .map(AuthorityEntity::getName)
                                        .map(String::toUpperCase)
                                        .collect(Collectors.toSet())
                );
        for (AuthorityEntity managed : attachedAuthorityEntities) {
            if (nonManagedEntity.getAuthorities().contains(managed)) {
                nonManagedEntity.getAuthorities().remove(managed); // removes unmanaged equivalent if present
                nonManagedEntity.getAuthorities().add(managed); // adds managed instance
            }
        }
    }

    @Override
    @Transactional
    public void remove(final AuthorizationConsent authorizationConsent) {
        authorizationConsentJpaRepository.findByRegisteredClientIdAndPrincipalName(authorizationConsent.getRegisteredClientId(),
                                                                                   authorizationConsent.getPrincipalName())
                                         .map(found
                                                      -> authorizationConsentJpaRepository.deleteByRegisteredClientIdAndPrincipalName(found.getRegisteredClientId(),
                                                                                                                                      found.getPrincipalName()))
                                         .orElseThrow();

    }

    @Override
    public AuthorizationConsent findByRegisteredClientIdAndPrincipalName(final UUID registeredClientId, final String principalName) {
        if (registeredClientId == null || principalName == null) {
            return null;
        }
        return authorizationConsentJpaRepository.findByRegisteredClientIdAndPrincipalName(registeredClientId, principalName)
                                                .map(authorizationConsentMapper::authorizationConsentEntityToAuthorizationConsent)
                                                .orElse(null);
    }

}
