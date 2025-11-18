package io.github.blakedunaway.authserver.integration.repository.gateway;

import io.github.blakedunaway.authserver.business.model.User;

public interface UserRepository {

    User findByEmail(final String email);

    User save(final User user);

    User findByRegisteredClient_IdAndEmail(final String registeredClientId, final String email);

}
