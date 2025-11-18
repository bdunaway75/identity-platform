package io.github.blakedunaway.authserver.integration.repository.gateway;


import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;

public interface RegisteredClientInternalRepository {

    RegisteredClientModel findByClientId(final String clientId);

    RegisteredClientModel findById(final String id);

    RegisteredClientModel save(final RegisteredClientModel registeredClient);

    RegisteredClientModel update(final RegisteredClientModel registeredClient);

}
