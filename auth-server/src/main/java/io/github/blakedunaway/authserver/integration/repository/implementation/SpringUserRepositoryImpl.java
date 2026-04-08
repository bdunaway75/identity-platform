package io.github.blakedunaway.authserver.integration.repository.implementation;

import io.github.blakedunaway.authserver.business.model.Authorities;
import io.github.blakedunaway.authserver.business.model.user.ClientUser;
import io.github.blakedunaway.authserver.integration.entity.AuthorityEntity;
import io.github.blakedunaway.authserver.integration.entity.ClientUserEntity;
import io.github.blakedunaway.authserver.integration.repository.gateway.UserRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.AuthoritiesJpaRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.ClientUserJpaRepository;
import io.github.blakedunaway.authserver.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpringUserRepositoryImpl implements UserRepository {

    private final ClientUserJpaRepository clientUserJpaRepository;

    private final UserMapper userMapper;

    private final AuthoritiesJpaRepository authoritiesJpaRepository;

    @Override
    @Transactional
    public ClientUser save(final ClientUser clientUser) {
        final ClientUserEntity clientUserEntity = userMapper.userToUserEntity(clientUser);
        clientUserJpaRepository.findByEmailAndClientId(clientUser.getEmail(), clientUser.getClientId()).ifPresent(found -> clientUserEntity.setUserId(found.getUserId()));
        clientUserEntity.setAuthorities(resolveManagedAuthorities(clientUser));
        return userMapper.userEntityToUser(clientUserJpaRepository.save(clientUserEntity));
    }

    private Set<AuthorityEntity> resolveManagedAuthorities(final ClientUser clientUser) {
        final Set<String> requestedAuthorityNames = clientUser.getAuthorities() == null
                                                    ? Collections.emptySet()
                                                    : clientUser.getAuthorities()
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

    @Override
    public Optional<ClientUser> findByClient_IdAndEmail(final String clientId, final String email) {
        return clientUserJpaRepository.findByEmailAndClientId(email, clientId).map(userMapper::userEntityToUser);
    }

    @Override
    public int getUserCount(final String clientId) {
        return clientUserJpaRepository.countAllByClientId(clientId);
    }

    @Override
    public List<ClientUser> findAllByRegisteredClientIds(final Set<UUID> registeredClientIds) {
        if (registeredClientIds == null || registeredClientIds.isEmpty()) {
            return List.of();
        }

        return clientUserJpaRepository.findAllByRegisteredClientIds(registeredClientIds)
                                      .stream()
                                      .map(userMapper::userEntityToUser)
                                      .toList();
    }

    @Override
    public Optional<ClientUser> findByIdAndRegisteredClientIds(final UUID clientUserId, final Set<UUID> registeredClientIds) {
        if (clientUserId == null || registeredClientIds == null || registeredClientIds.isEmpty()) {
            return Optional.empty();
        }

        return clientUserJpaRepository.findByIdAndRegisteredClientIds(clientUserId, registeredClientIds)
                                      .map(userMapper::userEntityToUser);
    }

}

