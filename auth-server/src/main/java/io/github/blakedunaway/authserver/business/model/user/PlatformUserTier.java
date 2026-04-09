package io.github.blakedunaway.authserver.business.model.user;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlatformUserTier {

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

}
