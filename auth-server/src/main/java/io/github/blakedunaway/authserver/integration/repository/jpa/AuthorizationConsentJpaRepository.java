package io.github.blakedunaway.authserver.integration.repository.jpa;

import io.github.blakedunaway.authserver.integration.entity.AuthorizationConsentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthorizationConsentJpaRepository extends JpaRepository<AuthorizationConsentEntity, UUID> {

    AuthorizationConsentEntity save(final AuthorizationConsentEntity authorizationConsentEntity);

    Integer deleteByRegisteredClientIdAndPrincipalName (final String registeredClientId, final String principalName);

    Optional<AuthorizationConsentEntity> findByRegisteredClientIdAndPrincipalName(final String registeredClientId, final String principalName);

}
