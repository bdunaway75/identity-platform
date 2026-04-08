package io.github.blakedunaway.authserver.business.service;

import io.github.blakedunaway.authserver.business.model.AuthToken;
import io.github.blakedunaway.authserver.integration.repository.gateway.AuthTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final AuthTokenRepository authTokenRepository;

    public List<AuthToken> findAllByRegisteredClientIds(final Set<UUID> registeredClientIds) {
        if (registeredClientIds == null || registeredClientIds.isEmpty()) {
            return List.of();
        }
        return authTokenRepository.findAllByRegisteredClientIds(registeredClientIds);
    }

    public boolean invalidateByIdAndRegisteredClientIds(final UUID authTokenId,
                                                        final Set<UUID> registeredClientIds) {
        if (registeredClientIds == null || registeredClientIds.isEmpty()) {
            return false;
        }
        return authTokenRepository.invalidateByIdAndRegisteredClientIds(authTokenId, registeredClientIds, Instant.now());
    }

    public int invalidateAllByRegisteredClientId(final UUID registeredClientId) {
        return authTokenRepository.invalidateAllByRegisteredClientId(registeredClientId, Instant.now());
    }

}
