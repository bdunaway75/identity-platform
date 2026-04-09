package io.github.blakedunaway.authserver.integration.repository.implementation;

import io.github.blakedunaway.authserver.business.model.Authority;
import io.github.blakedunaway.authserver.business.model.user.PlatformUserTier;
import io.github.blakedunaway.authserver.business.model.user.PlatformUser;
import io.github.blakedunaway.authserver.integration.entity.AuthorityEntity;
import io.github.blakedunaway.authserver.integration.entity.PlatformUserEntity;
import io.github.blakedunaway.authserver.integration.entity.PlatformUserTierEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientEntity;
import io.github.blakedunaway.authserver.integration.repository.gateway.PlatformUserRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.AuthorityJpaRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.PlatformUserJpaRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.PlatformUserTierJpaRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.RegisterClientJpaRepository;
import io.github.blakedunaway.authserver.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashSet;
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

    private final AuthorityJpaRepository authorityJpaRepository;

    private final RegisterClientJpaRepository registerClientJpaRepository;

    private final PlatformUserTierJpaRepository platformUserTierJpaRepository;

    @Value("${auth-server.frontend.client-id}")
    private String frontEndClientId;

    @Override
    @Transactional
    public PlatformUser save(final PlatformUser platformUser) {
        final PlatformUserEntity platformUserEntity = userMapper.userToPlatformUserEntity(platformUser);
        final PlatformUserEntity existingPlatformUserEntity = platformUserJpaRepository.findByEmailIgnoreCase(platformUser.getEmail())
                                                                                       .orElse(null);
        if (existingPlatformUserEntity != null) {
            platformUserEntity.setUserId(existingPlatformUserEntity.getUserId());
        }

        platformUserEntity.setTier(resolveManagedTier(platformUser.getTier(), existingPlatformUserEntity));
        platformUserEntity.setAuthorities(resolveManagedAuthorities(platformUser));
        platformUserEntity.setRegisteredClients(resolveManagedRegisteredClients(platformUser));
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

    @Override
    public PlatformUser loadPlatformUserById(UUID uuid) {
        return platformUserJpaRepository.findById(uuid).map(userMapper::platformUserEntityToPlatformUser).orElse(null);
    }

    private Set<AuthorityEntity> resolveManagedAuthorities(final PlatformUser platformUser) {
        final Set<String> requestedAuthorityNames = platformUser.getAuthorities() == null
                                                    ? Collections.emptySet()
                                                    : platformUser.getAuthorities()
                                                                  .stream()
                                                                  .map(Authority::getName)
                                                                  .filter(name -> name != null && !name.isBlank())
                                                                  .map(String::toUpperCase)
                                                                  .collect(Collectors.toSet());

        if (requestedAuthorityNames.isEmpty()) {
            return new HashSet<>();
        }

        final RegisteredClientEntity entity = registerClientJpaRepository.findByClientId(frontEndClientId).orElseThrow();

        final Set<AuthorityEntity> attachedAuthorityEntities =
                authorityJpaRepository.findAllByRegisteredClient_ClientIdAndNameIn(frontEndClientId, requestedAuthorityNames);
        final Set<String> existingAuthorityNames = attachedAuthorityEntities.stream()
                                                                            .map(AuthorityEntity::getName)
                                                                            .map(String::toUpperCase)
                                                                            .filter(requestedAuthorityNames::contains)
                                                                            .collect(Collectors.toSet());

        final Set<AuthorityEntity> createdAuthorities = requestedAuthorityNames.stream()
                                                                               .filter(name -> !existingAuthorityNames.contains(name))
                                                                               .map(name -> new AuthorityEntity(name, entity))
                                                                               .collect(Collectors.toSet());
        if (!createdAuthorities.isEmpty()) {
            attachedAuthorityEntities.addAll(authorityJpaRepository.saveAll(createdAuthorities));
        }

        return new HashSet<>(attachedAuthorityEntities);
    }

    private Set<RegisteredClientEntity> resolveManagedRegisteredClients(final PlatformUser platformUser) {
        final Set<UUID> registeredClientIds = platformUser.getRegisteredClientIds() == null
                                              ? Collections.emptySet()
                                              : platformUser.getRegisteredClientIds()
                                                            .stream()
                                                            .filter(id -> id != null)
                                                            .collect(Collectors.toCollection(HashSet::new));

        if (registeredClientIds.isEmpty()) {
            return new HashSet<>();
        }

        return registerClientJpaRepository.findAllById(registeredClientIds)
                                          .stream()
                                          .collect(Collectors.toCollection(HashSet::new));
    }

    private PlatformUserTierEntity resolveManagedTier(final PlatformUserTier requestedTier,
                                                      final PlatformUserEntity existingPlatformUserEntity) {
        if (requestedTier == null) {
            return existingPlatformUserEntity == null ? null : existingPlatformUserEntity.getTier();
        }

        if (requestedTier.getId() != null) {
            return platformUserTierJpaRepository.findById(requestedTier.getId())
                                               .orElseThrow(() -> new IllegalArgumentException("Platform user tier not found"));
        }

        if (requestedTier.getName() != null && !requestedTier.getName().isBlank()) {
            return platformUserTierJpaRepository.findByTierNameIgnoreCase(requestedTier.getName())
                                               .orElseThrow(() -> new IllegalArgumentException("Platform user tier not found"));
        }

        final PlatformUserTierEntity resolvedTier = existingPlatformUserEntity == null ? null : existingPlatformUserEntity.getTier();
        Assert.notNull(resolvedTier, "Platform user tier is required");
        return resolvedTier;
    }

}
