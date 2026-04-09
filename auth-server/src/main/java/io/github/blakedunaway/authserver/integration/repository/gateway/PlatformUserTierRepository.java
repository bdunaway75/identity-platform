package io.github.blakedunaway.authserver.integration.repository.gateway;

import io.github.blakedunaway.authserver.business.model.user.PlatformUserTier;

import java.util.List;

public interface PlatformUserTierRepository {

    List<PlatformUserTier> findAll();

    PlatformUserTier findById(final String id);

    PlatformUserTier findByStripePriceId(final String stripePriceId);

}
