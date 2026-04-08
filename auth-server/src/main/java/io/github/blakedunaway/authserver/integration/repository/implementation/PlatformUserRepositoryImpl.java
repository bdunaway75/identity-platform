package io.github.blakedunaway.authserver.integration.repository.implementation;

import io.github.blakedunaway.authserver.business.model.Authorities;
import io.github.blakedunaway.authserver.business.model.user.PlatformUser;
import io.github.blakedunaway.authserver.integration.entity.AuthorityEntity;
import io.github.blakedunaway.authserver.integration.entity.PlatformUserEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientEntity;
import io.github.blakedunaway.authserver.integration.repository.gateway.PlatformUserRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.AuthoritiesJpaRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.PlatformUserJpaRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.RegisterClientJpaRepository;
import io.github.blakedunaway.authserver.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformUserRepositoryImpl implements PlatformUserRepository {

    private final PlatformUserJpaRepository platformUserJpaRepository;

    private final UserMapper userMapper;

    private final AuthoritiesJpaRepository authoritiesJpaRepository;

    private final RegisterClientJpaRepository registerClientJpaRepository;

    @Override
    @Transactional
    public PlatformUser save(final PlatformUser platformUser) {
        final PlatformUserEntity platformUserEntity = userMapper.userToPlatformUserEntity(platformUser);
        platformUserEntity.setAuthorities(resolveManagedAuthorities(platformUser));
        platformUserEntity.setRegisteredClients(resolveManagedRegisteredClients(platformUser));
        platformUserJpaRepository.findByEmailIgnoreCase(platformUser.getEmail())
                                 .ifPresent(found -> platformUserEntity.setUserId(found.getUserId()));
        return userMapper.platformUserEntityToPlatformUser(platformUserJpaRepository.save(platformUserEntity));
    }

    @Override
    public Optional<PlatformUser> findByEmailIgnoreCase(final String email) {
        return platformUserJpaRepository.findByEmailIgnoreCase(email).map(userMapper::platformUserEntityToPlatformUser);
    }

    @Override
    public int getTotalUserCount(final String email) {
        if (email == null || email.isBlank()) {
            return 0;
        }
        return platformUserJpaRepository.getTotalUserCount(email);
    }

    @Override
    public int getTotalClientCount(final String email) {
        if (email == null || email.isBlank()) {
            return 0;
        }
        return platformUserJpaRepository.getTotalClientCount(email);
    }

    private Set<AuthorityEntity> resolveManagedAuthorities(final PlatformUser platformUser) {
        final Set<String> requestedAuthorityNames = platformUser.getAuthorities() == null
                                                    ? Collections.emptySet()
                                                    : platformUser.getAuthorities()
                                                                .stream()
                                                                .map(Authorities::getName)
                                                                .filter(name -> name != null && !name.isBlank())
                                                                .map(String::toUpperCase)
                                                                .collect(Collectors.toSet());

        if (requestedAuthorityNames.isEmpty()) {
            return new LinkedHashSet<>();
        }

        final Set<AuthorityEntity> attachedAuthorityEntities = authoritiesJpaRepository.findAllByNameIn(requestedAuthorityNames);
        final Set<String> existingAuthorityNames = attachedAuthorityEntities.stream()
                                                                            .map(AuthorityEntity::getName)
                                                                            .filter(name -> name != null && !name.isBlank())
                                                                            .map(String::toUpperCase)
                                                                            .collect(Collectors.toSet());

        final Set<AuthorityEntity> createdAuthorities = requestedAuthorityNames.stream()
                                                                               .filter(name -> !existingAuthorityNames.contains(name))
                                                                               .map(AuthorityEntity::create)
                                                                               .collect(Collectors.toSet());
        if (!createdAuthorities.isEmpty()) {
            attachedAuthorityEntities.addAll(authoritiesJpaRepository.saveAll(createdAuthorities));
        }

        return new LinkedHashSet<>(attachedAuthorityEntities);
    }

    private Set<RegisteredClientEntity> resolveManagedRegisteredClients(final PlatformUser platformUser) {
        final Set<UUID> registeredClientIds = platformUser.getRegisteredClientIds() == null
                                              ? Collections.emptySet()
                                              : platformUser.getRegisteredClientIds()
                                                          .stream()
                                                          .filter(id -> id != null)
                                                          .collect(Collectors.toCollection(LinkedHashSet::new));

        if (registeredClientIds.isEmpty()) {
            return new LinkedHashSet<>();
        }

        return registerClientJpaRepository.findAllById(registeredClientIds)
                                          .stream()
                                          .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
