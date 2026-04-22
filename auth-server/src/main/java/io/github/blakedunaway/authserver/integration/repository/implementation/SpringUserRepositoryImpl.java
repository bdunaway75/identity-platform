package io.github.blakedunaway.authserver.integration.repository.implementation;

import io.github.blakedunaway.authserver.business.model.Authority;
import io.github.blakedunaway.authserver.business.model.user.ClientUser;
import io.github.blakedunaway.authserver.integration.entity.AuthorityEntity;
import io.github.blakedunaway.authserver.integration.entity.ClientUserEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientEntity;
import io.github.blakedunaway.authserver.integration.repository.gateway.UserRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.AuthorityJpaRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.ClientUserJpaRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.RegisterClientJpaRepository;
import io.github.blakedunaway.authserver.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

    private final AuthorityJpaRepository authorityJpaRepository;

    private final RegisterClientJpaRepository registerClientJpaRepository;

    @Override
    @Transactional
    public ClientUser save(final ClientUser clientUser) {
        final ClientUserEntity clientUserEntity = userMapper.clientUserToClientUserEntity(clientUser);
        clientUserJpaRepository.findByEmailAndClientId(clientUser.getEmail(), clientUser.getClientId()).ifPresent(found -> clientUserEntity.setUserId(found.getUserId()));
        clientUserEntity.setAuthorities(resolveManagedAuthorities(clientUser));
        return userMapper.clientUserEntityToClientUser(clientUserJpaRepository.save(clientUserEntity));
    }

    private Set<AuthorityEntity> resolveManagedAuthorities(final ClientUser clientUser) {
        final Set<String> requestedAuthorityNames = clientUser.getAuthorities() == null
                                                    ? Collections.emptySet()
                                                    : clientUser.getAuthorities()
                                                                .stream()
                                                                .map(Authority::getName)
                                                                .filter(name -> name != null && !name.isBlank())
                                                                .map(String::toUpperCase)
                                                                .collect(Collectors.toSet());

        if (requestedAuthorityNames.isEmpty()) {
            return new HashSet<>();
        }

        final UUID registeredClientId = registerClientJpaRepository.findByClientId(clientUser.getClientId())
                                                                   .map(RegisteredClientEntity::getRegisteredClientId)
                                                                   .orElse(null);
        if (registeredClientId == null) {
            return new HashSet<>();
        }

        final Set<AuthorityEntity> attachedAuthorityEntities =
                authorityJpaRepository.findAllByRegisteredClient_RegisteredClientIdAndNameIn(registeredClientId, requestedAuthorityNames);

        return new HashSet<>(attachedAuthorityEntities);
    }

    @Override
    public Optional<ClientUser> findByClient_IdAndEmail(final String clientId, final String email) {
        return clientUserJpaRepository.findByEmailAndClientId(email, clientId).map(userMapper::clientUserEntityToClientUser);
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
                                      .map(userMapper::clientUserEntityToClientUser)
                                      .toList();
    }

    @Override
    public Optional<ClientUser> findByIdAndRegisteredClientIds(final UUID clientUserId, final Set<UUID> registeredClientIds) {
        if (clientUserId == null || registeredClientIds == null || registeredClientIds.isEmpty()) {
            return Optional.empty();
        }

        return clientUserJpaRepository.findByIdAndRegisteredClientIds(clientUserId, registeredClientIds)
                                      .map(userMapper::clientUserEntityToClientUser);
    }

}

