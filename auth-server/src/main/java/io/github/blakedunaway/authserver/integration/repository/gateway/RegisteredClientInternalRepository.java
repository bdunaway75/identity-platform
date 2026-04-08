package io.github.blakedunaway.authserver.integration.repository.gateway;


import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;

import java.util.Set;
import java.util.UUID;

public interface RegisteredClientInternalRepository {

    RegisteredClientModel findByClientId(final String clientId);

    RegisteredClientModel findById(final String id);

    Set<RegisteredClientModel> findAllByIds(final Set<UUID> ids);

    RegisteredClientModel save(final RegisteredClientModel registeredClient);

    RegisteredClientModel update(final RegisteredClientModel registeredClient);

}
