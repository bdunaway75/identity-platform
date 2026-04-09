package io.github.blakedunaway.authserver.business.api.dto;

import io.github.blakedunaway.authserver.business.model.user.PlatformUserTier;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class PlatformUserTierResponse {

    private final UUID id;

    private final String stripePriceId;

    private final String name;

    private final int price;

    private final String description;

    private final int tierOrder;

    private final int allowedNumberOfRegisteredClients;

    private final int allowedNumberOfGlobalUsers;

    private final int allowedNumberOfGlobalScopes;

    private final int allowedNumberOfGlobalAuthorities;

    public static PlatformUserTierResponse fromModel(final PlatformUserTier model) {
        if (model == null) {
            return null;
        }

        return PlatformUserTierResponse.builder()
                                       .id(model.getId())
                                       .stripePriceId(model.getStripePriceId())
                                       .name(model.getName())
                                       .price(model.getPrice())
                                       .description(model.getDescription())
                                       .tierOrder(model.getTierOrder())
                                       .allowedNumberOfRegisteredClients(model.getAllowedNumberOfRegisteredClients())
                                       .allowedNumberOfGlobalUsers(model.getAllowedNumberOfGlobalUsers())
                                       .allowedNumberOfGlobalScopes(model.getAllowedNumberOfGlobalScopes())
                                       .allowedNumberOfGlobalAuthorities(model.getAllowedNumberOfGlobalAuthorities())
                                       .build();
    }

}
