package io.github.blakedunaway.authserver.integration.repository.gateway;

import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface AuthorityRepository {

    void saveAll(final UUID registeredClientId, final Set<String> authorities);

}
