package io.github.blakedunaway.authserver.integration.repository.jpa;

import io.github.blakedunaway.authserver.integration.entity.DemoAccessCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DemoAccessCodeJpaRepository extends JpaRepository<DemoAccessCodeEntity, UUID> {

    Optional<DemoAccessCodeEntity> findByAccessCode(final String accessCode);

    Optional<DemoAccessCodeEntity> findByUser_UserId(final UUID userId);

}
