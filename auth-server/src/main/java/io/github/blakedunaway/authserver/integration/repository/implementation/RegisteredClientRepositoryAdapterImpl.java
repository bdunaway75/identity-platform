package io.github.blakedunaway.authserver.integration.repository.implementation;

import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import io.github.blakedunaway.authserver.integration.repository.gateway.RegisteredClientInternalRepository;
import io.github.blakedunaway.authserver.mapper.RegisteredClientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegisteredClientRepositoryAdapterImpl implements RegisteredClientRepository {

    private final RegisteredClientInternalRepository internal;

    private final RegisteredClientMapper mapper;

    @Override
    @Transactional
    public void save(final RegisteredClient rc) {
        internal.save(mapper.registeredClientToRegisteredClientModel(rc)); // this will only ever be called to update a client, i take care of actual new clients
    }

    @Override
    public RegisteredClient findById(final String id) {
        final RegisteredClientModel model = internal.findById(id);
        return model == null ? null : model.toBuilder().toOAuth2RegisteredClient();
    }

    @Override
    public RegisteredClient findByClientId(final String clientId) {
        final RegisteredClientModel model = internal.findByClientId(clientId);
        return model == null ? null : model.toBuilder().toOAuth2RegisteredClient();
    }

}

