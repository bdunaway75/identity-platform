package io.github.blakedunaway.authserver.integration.repository.implementation;

import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientEntity;
import io.github.blakedunaway.authserver.integration.repository.gateway.RegisteredClientInternalRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.RegisterClientJpaRepository;
import io.github.blakedunaway.authserver.mapper.RegisteredClientMapper;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegisteredClientRepositoryImpl implements RegisteredClientInternalRepository {

    private final RegisteredClientMapper registeredClientMapper;

    private final RegisterClientJpaRepository registerClientJpaRepository;

    @Transactional
    public RegisteredClientModel save(final RegisteredClientModel newModel) {
        if (registerClientJpaRepository.findByClientId(newModel.getClientId()).isPresent()) {
            throw new EntityExistsException("ClientId already exists: " + newModel.getClientId());
        }
        final RegisteredClientEntity clientEntity = registeredClientMapper.registeredModelClientToRegisteredClientEntity(newModel);
        registerClientJpaRepository.save(clientEntity);
        return newModel;
    }

    @Transactional
    public RegisteredClientModel update(final RegisteredClientModel updatedModel) {
        if (updatedModel.getClientId() == null) {
            throw new IllegalArgumentException("clientId is required for update()");
        }
        registerClientJpaRepository.findByClientId(updatedModel.getClientId()).map(client -> {
                                       registeredClientMapper.updateEntity(updatedModel, client);
                                       return client;
                                   })
                                   .orElseThrow(() -> new EntityNotFoundException(
                                           "Client with clientId " + updatedModel.getClientId() + " does not exist"));

        return updatedModel;
    }

    public RegisteredClientModel findById(final String id) {
        return registeredClientMapper.registeredClientEntityToRegisteredClientModel(registerClientJpaRepository.findById(id)
                                                                                                               .orElse(null));
    }

    public RegisteredClientModel findByClientId(final String clientId) {
        return registeredClientMapper.registeredClientEntityToRegisteredClientModel(registerClientJpaRepository.findByClientId(clientId)
                                                                                                               .orElse(null));
    }

}
