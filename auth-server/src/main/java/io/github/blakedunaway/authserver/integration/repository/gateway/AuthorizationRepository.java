package io.github.blakedunaway.authserver.integration.repository.gateway;

import io.github.blakedunaway.authserver.business.model.Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

import java.util.List;
import java.util.UUID;

public interface AuthorizationRepository {

    Authorization save(final OAuth2Authorization authorization);

    void remove(final String id);

    Authorization findById(final UUID authId);

    Authorization findByToken(final String token, final String tokenType);

    List<Authorization> findAll();

}
