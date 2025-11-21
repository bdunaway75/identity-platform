package io.github.blakedunaway.authserver.integration.repository.jpa;

import io.github.blakedunaway.authserver.integration.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmailIgnoreCase(final String email);

    UserEntity findByRegisteredClient_IdAndEmail(final String registeredClientId, final String email);
}
