package io.github.blakedunaway.authserver.integration.repository.jpa;

import io.github.blakedunaway.authserver.business.model.enums.SigningKeyStatus;
import io.github.blakedunaway.authserver.integration.entity.SigningKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SigningKeyJpaRepository extends JpaRepository<SigningKeyEntity, UUID> {

    List<SigningKeyEntity> findAllByStatusIn(final Collection<SigningKeyStatus> statuses);

    List<SigningKeyEntity> findByStatus(final SigningKeyStatus status);

    Optional<SigningKeyEntity> findByKid(final String kid);

    List<SigningKeyEntity> findSigningKeyEntitiesByKidIn(final Collection<String> kids);

}
