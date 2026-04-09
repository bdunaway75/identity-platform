package io.github.blakedunaway.authserver.business.service;

import io.github.blakedunaway.authserver.business.model.user.PlatformUserTier;
import io.github.blakedunaway.authserver.integration.repository.gateway.PlatformUserTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlatformUserTierService {

    private final PlatformUserTierRepository platformUserTierRepository;

    public List<PlatformUserTier> findAllTiers() {
        return platformUserTierRepository.findAll();
    }

    public PlatformUserTier findTierById(String tierId) {
        return platformUserTierRepository.findById(tierId);
    }

    public PlatformUserTier findTierByStripePriceId(final String stripePriceId) {
        return platformUserTierRepository.findByStripePriceId(stripePriceId);
    }

}
