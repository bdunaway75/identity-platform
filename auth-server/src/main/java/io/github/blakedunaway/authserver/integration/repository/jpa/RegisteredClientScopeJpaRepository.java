package io.github.blakedunaway.authserver.integration.repository.jpa;

import io.github.blakedunaway.authserver.integration.entity.RegisteredClientScopeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface RegisteredClientScopeJpaRepository extends JpaRepository<RegisteredClientScopeEntity, UUID> {

    Set<RegisteredClientScopeEntity> findAllByScopeIn(Set<String> scopes);
}
