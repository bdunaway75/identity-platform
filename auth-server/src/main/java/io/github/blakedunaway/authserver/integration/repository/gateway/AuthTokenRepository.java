package io.github.blakedunaway.authserver.integration.repository.gateway;

import io.github.blakedunaway.authserver.business.model.AuthToken;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AuthTokenRepository {

    List<AuthToken> findAllByRegisteredClientIds(final Set<UUID> registeredClientIds);

    boolean invalidateByIdAndRegisteredClientIds(final UUID authTokenId,
                                                 final Set<UUID> registeredClientIds,
                                                 final Instant revokedAt);

    int invalidateAllByRegisteredClientId(final UUID registeredClientId, final Instant revokedAt);

}
