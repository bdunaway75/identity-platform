package io.github.blakedunaway.authserver.integration.repository.implementation;

import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientScopeEntity;
import io.github.blakedunaway.authserver.integration.repository.gateway.RegisteredClientInternalRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.RegisterClientJpaRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.RegisteredClientScopeJpaRepository;
import io.github.blakedunaway.authserver.mapper.RegisteredClientMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegisteredClientRepositoryImpl implements RegisteredClientInternalRepository {

    private final RegisteredClientMapper registeredClientMapper;

    private final RegisterClientJpaRepository registerClientJpaRepository;

    private final RegisteredClientScopeJpaRepository registeredClientScopeJpaRepository;

    @Transactional
    public RegisteredClientModel save(final RegisteredClientModel model) {
        Assert.isNull(model.getClientId(), "Client id must be null for new RegisteredClient");
        Assert.isNull(model.getId(), "ID must be null for new RegisteredClient");
        final RegisteredClientEntity entity =
                registeredClientMapper.registeredModelClientToRegisteredClientEntity(model.withClientId(UUID.randomUUID().toString())
                                                                                          .withClientIdIssuedAt(LocalDateTime.now()));
        entity.setScopes(resolveManagedScopes(model.getScopes()));

        return registeredClientMapper.registeredClientEntityToRegisteredClientModel(registerClientJpaRepository.save(entity));
    }

    @Transactional
    public RegisteredClientModel update(final RegisteredClientModel updatedModel) {
        if (updatedModel.getClientId() == null) {
            throw new IllegalArgumentException("clientId is required for update()");
        }
        registerClientJpaRepository.findByClientId(updatedModel.getClientId()).map(client -> {
                                       registeredClientMapper.updateEntity(updatedModel, client);
                                       client.setScopes(resolveManagedScopes(updatedModel.getScopes()));
                                       return client;
                                   })
                                   .orElseThrow(() -> new EntityNotFoundException(
                                           "Client with clientId " + updatedModel.getClientId() + " does not exist"));

        return updatedModel;
    }

    public RegisteredClientModel findById(final String id) {
        return registeredClientMapper.registeredClientEntityToRegisteredClientModel(registerClientJpaRepository.findById(UUID.fromString(id))
                                                                                                               .orElse(null));
    }

    public RegisteredClientModel findByClientId(final String clientId) {
        return registeredClientMapper.registeredClientEntityToRegisteredClientModel(registerClientJpaRepository.findByClientId(clientId)
                                                                                                               .orElse(null));
    }

    @Override
    public Set<RegisteredClientModel> findAllByIds(final Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }

        return registerClientJpaRepository.findAllById(ids)
                                          .stream()
                                          .map(registeredClientMapper::registeredClientEntityToRegisteredClientModel)
                                          .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<RegisteredClientScopeEntity> resolveManagedScopes(final Set<String> scopes) {
        final Set<String> requestedScopes = scopes == null
                                            ? Collections.emptySet()
                                            : scopes.stream()
                                                    .filter(scope -> scope != null && !scope.isBlank())
                                                    .map(String::trim)
                                                    .collect(Collectors.toCollection(LinkedHashSet::new));

        if (requestedScopes.isEmpty()) {
            return new LinkedHashSet<>();
        }

        final Set<RegisteredClientScopeEntity> attachedScopes = registeredClientScopeJpaRepository.findAllByScopeIn(requestedScopes);
        final Set<String> existingScopes = attachedScopes.stream()
                                                         .map(RegisteredClientScopeEntity::getScope)
                                                         .filter(scope -> scope != null && !scope.isBlank())
                                                         .collect(Collectors.toSet());

        final Set<RegisteredClientScopeEntity> createdScopes = requestedScopes.stream()
                                                                              .filter(scope -> !existingScopes.contains(scope))
                                                                              .map(scope -> new RegisteredClientScopeEntity(null, scope))
                                                                              .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!createdScopes.isEmpty()) {
            attachedScopes.addAll(registeredClientScopeJpaRepository.saveAll(createdScopes));
        }

        return new LinkedHashSet<>(attachedScopes);
    }

}
