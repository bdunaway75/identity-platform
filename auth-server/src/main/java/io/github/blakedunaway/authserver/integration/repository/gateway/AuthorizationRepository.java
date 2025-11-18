package io.github.blakedunaway.authserver.integration.repository.gateway;

import io.github.blakedunaway.authserver.business.model.Authorization;

import java.util.List;

public interface AuthorizationRepository {

    Authorization save(final Authorization authorization);

    void remove(final String id);

    Authorization findById(final String id);

    Authorization findByToken(final String token, final String tokenType);

    Authorization findByTokenAttribute(final String attributeKey, final String attributeValue);

    List<Authorization> findAll();

}
