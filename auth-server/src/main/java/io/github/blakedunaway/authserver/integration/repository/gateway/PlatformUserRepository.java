package io.github.blakedunaway.authserver.integration.repository.gateway;

import io.github.blakedunaway.authserver.business.model.user.PlatformUser;

import java.util.Optional;
import java.util.UUID;

public interface PlatformUserRepository {

    PlatformUser save(final PlatformUser platformUser);

    Optional<PlatformUser> findByEmailIgnoreCase(final String email);

    int getTotalUserCount(final String email);

    int getTotalClientCount(final String email);

    PlatformUser loadPlatformUserById(final UUID uuid);

}
