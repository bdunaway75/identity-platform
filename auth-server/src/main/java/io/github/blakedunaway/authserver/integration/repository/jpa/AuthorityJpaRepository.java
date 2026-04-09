package io.github.blakedunaway.authserver.integration.repository.jpa;

import io.github.blakedunaway.authserver.integration.entity.AuthorityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

@Repository
public interface AuthorityJpaRepository extends JpaRepository<AuthorityEntity, UUID> {

    Set<AuthorityEntity> findAllByRegisteredClient_RegisteredClientId(final UUID registeredClientId);

    Set<AuthorityEntity> findAllByRegisteredClient_RegisteredClientIdAndNameIn(final UUID registeredClientId,
                                                                               final Set<String> names);

    Set<AuthorityEntity> findAllByRegisteredClient_ClientIdAndNameIn(String clientId, Collection<String> names);

    void removeAllByNameInAndRegisteredClient_RegisteredClientId(final Collection<String> names, final UUID registeredClientId);
}
