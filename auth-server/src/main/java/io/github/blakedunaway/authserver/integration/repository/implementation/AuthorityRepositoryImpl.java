package io.github.blakedunaway.authserver.integration.repository.implementation;

import io.github.blakedunaway.authserver.integration.entity.AuthorityEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientEntity;
import io.github.blakedunaway.authserver.integration.repository.gateway.AuthorityRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.AuthorityJpaRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.RegisterClientJpaRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorityRepositoryImpl implements AuthorityRepository {

    private final AuthorityJpaRepository authorityJpaRepository;

    private final RegisterClientJpaRepository  registerClientJpaRepository;

    @Override
    @Transactional
    public void saveAll(final UUID registeredClientId, final Set<String> authorities) {
        if (registeredClientId == null || authorities == null) {
            return;
        }

        final RegisteredClientEntity entity = registerClientJpaRepository.findById(registeredClientId).orElse(null);

        if (entity == null) {
            return;
        }

        final Set<String> normalizedAuthorities = authorities.stream().map(String::toUpperCase).collect(Collectors.toSet());

        final Set<String> registeredAuthorities = authorityJpaRepository.findAllByRegisteredClient_RegisteredClientId(registeredClientId)
                                                                        .stream()
                                                                        .map(AuthorityEntity::getName)
                                                                        .collect(Collectors.toSet());
        final Set<String> newNames = normalizedAuthorities.stream()
                                                .filter(name -> !registeredAuthorities.contains(name))
                                                .collect(Collectors.toCollection(HashSet::new));

        final Set<String> removedNames = registeredAuthorities.stream()
                                                              .map(String::toUpperCase)
                                                              .filter(name -> !normalizedAuthorities.contains(name))
                                                              .collect(Collectors.toCollection(HashSet::new));
        if (CollectionUtils.isNotEmpty(newNames)) {
            final Set<AuthorityEntity> created = newNames.stream()
                                                         .map(name -> new AuthorityEntity(name, entity))
                                                         .collect(Collectors.toCollection(HashSet::new));
            authorityJpaRepository.saveAll(created);
        }
        if (CollectionUtils.isNotEmpty(removedNames)) {
            authorityJpaRepository.removeAllByNameInAndRegisteredClient_RegisteredClientId(removedNames, registeredClientId);
        }

    }

}
