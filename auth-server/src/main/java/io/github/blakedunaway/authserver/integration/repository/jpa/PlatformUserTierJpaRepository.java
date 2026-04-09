package io.github.blakedunaway.authserver.integration.repository.jpa;

import io.github.blakedunaway.authserver.integration.entity.PlatformUserTierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformUserTierJpaRepository extends JpaRepository<PlatformUserTierEntity, UUID> {

    Optional<PlatformUserTierEntity> findByTierNameIgnoreCase(final String tierName);

    Optional<PlatformUserTierEntity> findByStripPriceId(final String stripPriceId);

}
