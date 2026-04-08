package io.github.blakedunaway.authserver.integration.repository.implementation;

import io.github.blakedunaway.authserver.business.model.AuthToken;
import io.github.blakedunaway.authserver.integration.repository.gateway.AuthTokenRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.AuthTokenJpaRepository;
import io.github.blakedunaway.authserver.mapper.AuthTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthTokenRepositoryImpl implements AuthTokenRepository {

    private final AuthTokenJpaRepository authTokenJpaRepository;

    private final AuthTokenMapper authTokenMapper;

    @Override
    public List<AuthToken> findAllByRegisteredClientIds(final Set<UUID> registeredClientIds) {
        return authTokenJpaRepository.findAllByRegisteredClientIds(registeredClientIds)
                                     .stream()
                                     .map(authTokenMapper::authTokenEntityToAuthToken)
                                     .toList();
    }

    @Override
    @Transactional
    public boolean invalidateByIdAndRegisteredClientIds(final UUID authTokenId,
                                                        final Set<UUID> registeredClientIds,
                                                        final Instant revokedAt) {
        return authTokenJpaRepository.invalidateByIdAndRegisteredClientIds(authTokenId, registeredClientIds, revokedAt) > 0;
    }

    @Override
    @Transactional
    public int invalidateAllByRegisteredClientId(final UUID registeredClientId, final Instant revokedAt) {
        return authTokenJpaRepository.invalidateAllByRegisteredClientId(registeredClientId, revokedAt);
    }

}
