package io.github.blakedunaway.authserver.integration.repository.gateway;

import io.github.blakedunaway.authserver.business.model.DemoAccessCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DemoAccessCodeRepository {

    DemoAccessCode save(final DemoAccessCode demoAccessCode);

    Optional<DemoAccessCode> findById(final UUID id);

    Optional<DemoAccessCode> findByAccessCode(final String accessCode);

    Optional<DemoAccessCode> findByPlatformUserId(final UUID platformUserId);

    List<DemoAccessCode> findAll();

}
