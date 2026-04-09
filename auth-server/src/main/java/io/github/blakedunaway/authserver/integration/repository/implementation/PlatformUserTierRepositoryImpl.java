package io.github.blakedunaway.authserver.integration.repository.implementation;

import io.github.blakedunaway.authserver.business.model.user.PlatformUserTier;
import io.github.blakedunaway.authserver.integration.repository.gateway.PlatformUserTierRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.PlatformUserTierJpaRepository;
import io.github.blakedunaway.authserver.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformUserTierRepositoryImpl implements PlatformUserTierRepository {

    private final PlatformUserTierJpaRepository platformUserTierJpaRepository;

    private final UserMapper userMapper;

    @Override
    public List<PlatformUserTier> findAll() {
        return platformUserTierJpaRepository.findAll()
                                            .stream()
                                            .map(userMapper::platformUserTierEntityToPlatformUserTier)
                                            .sorted(Comparator.comparingInt(PlatformUserTier::getPrice)
                                                              .thenComparing(PlatformUserTier::getName,
                                                                             String.CASE_INSENSITIVE_ORDER))
                                            .toList();
    }

    @Override
    public PlatformUserTier findById(String id) {
        return platformUserTierJpaRepository.findById(UUID.fromString(id)).map(userMapper::platformUserTierEntityToPlatformUserTier).orElse(null);
    }

    @Override
    public PlatformUserTier findByStripePriceId(final String stripePriceId) {
        return platformUserTierJpaRepository.findByStripPriceId(stripePriceId)
                                            .map(userMapper::platformUserTierEntityToPlatformUserTier)
                                            .orElse(null);
    }

}
