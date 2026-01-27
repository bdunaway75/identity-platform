package io.github.blakedunaway.authserver.integration.repository.gateway;

import io.github.blakedunaway.authserver.business.model.User;

import java.util.UUID;

public interface UserRepository {

    User findByEmail(final String email);

    User save(final User user);

    User findByClient_IdAndEmail(final String clientId, final String email);

}
