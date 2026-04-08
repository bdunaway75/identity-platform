package io.github.blakedunaway.authserver.integration.repository.gateway;

import io.github.blakedunaway.authserver.business.model.user.ClientUser;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserRepository {

    ClientUser save(final ClientUser clientUser);

    Optional<ClientUser> findByClient_IdAndEmail(final String clientId, final String email);

    int getUserCount(final String clientId);

    List<ClientUser> findAllByRegisteredClientIds(final Set<UUID> registeredClientIds);

    Optional<ClientUser> findByIdAndRegisteredClientIds(final UUID clientUserId, final Set<UUID> registeredClientIds);
}
