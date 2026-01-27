package io.github.blakedunaway.authserver.integration.repository.jpa;

import io.github.blakedunaway.authserver.integration.entity.RegisteredClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegisterClientJpaRepository extends JpaRepository<RegisteredClientEntity, UUID> {

    RegisteredClientEntity save(final RegisteredClientEntity registeredClientEntity);

    Optional<RegisteredClientEntity> findByClientId(final String clientId);

}
