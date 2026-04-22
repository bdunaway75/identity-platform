package io.github.blakedunaway.authserver.business.model.user;

import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import jakarta.validation.ValidationException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlatformUser extends AbstractUser {

    private Set<UUID> registeredClientIds;

    private PlatformUserTier tier;

    private boolean isDemoUser;

    public Set<UUID> filterOwnedRegisteredClientIds(final Set<UUID> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            return Collections.emptySet();
        }

        final Set<UUID> ownedIds = getRegisteredClientIds();
        return requestedIds.stream()
                           .filter(ownedIds::contains)
                           .collect(Collectors.toCollection(HashSet::new));
    }

    public PlatformUser attachRegisteredClientId(final UUID registeredClientId) {
        Assert.notNull(registeredClientId, "RegisteredClientId cannot be null");

        return PlatformUser.from(this)
                           .registeredClientIds(ids -> ids.add(registeredClientId))
                           .build();
    }

    public void validateTierCompliance(final Set<RegisteredClientModel> registeredClients) {
        final PlatformUserTier resolvedTier = getTier();
        if (resolvedTier == null) {
            throw new ValidationException("Platform user tier is required");
        }

        final Set<RegisteredClientModel> resolvedRegisteredClients = registeredClients == null
                                                                     ? Collections.emptySet()
                                                                     : registeredClients;
        final int registeredClientCount = resolvedRegisteredClients.size();
        final int totalScopes = resolvedRegisteredClients.stream()
                                                         .mapToInt(registeredClient -> registeredClient.getScopes().size())
                                                         .sum();
        final int totalAuthorities = resolvedRegisteredClients.stream()
                                                              .mapToInt(registeredClient -> registeredClient.getAuthorities().size())
                                                              .sum();

        final List<String> errors = new java.util.ArrayList<>();
        if (registeredClientCount > resolvedTier.getAllowedNumberOfRegisteredClients()) {
            errors.add("registered clients exceed tier allowance");
        }
        if (totalScopes > resolvedTier.getAllowedNumberOfGlobalScopes()) {
            errors.add("scopes exceed tier allowance");
        }
        if (totalAuthorities > resolvedTier.getAllowedNumberOfGlobalAuthorities()) {
            errors.add("authorities exceed tier allowance");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Platform user tier validation failed: " + String.join(", ", errors));
        }
    }

    public static PlatformUserBuilder from(final PlatformUser platformUser) {
        return new PlatformUserBuilder().from(platformUser);
    }

    public static PlatformUserBuilder from(final UUID id) {
        return new PlatformUserBuilder().from(id);
    }

    public static PlatformUserBuilder from(final String email) {
        return new PlatformUserBuilder().from(email);
    }

    @Getter
    public static class PlatformUserBuilder extends AbstractUser.AbstractUserBuilder<PlatformUserBuilder> {

        private PlatformUserTier tier;

        private boolean isDemoUser;

        @Override
        protected PlatformUserBuilder self() {
            return this;
        }

        public PlatformUserBuilder from(final PlatformUser user) {
            super.from(user);
            this.registeredClientIds = user.getRegisteredClientIds() == null
                                       ? new HashSet<>()
                                       : new HashSet<>(user.getRegisteredClientIds());
            this.tier = user.getTier();
            this.isDemoUser = user.isDemoUser();
            return this;
        }

        @Override
        public PlatformUserBuilder from(final UUID id) {
            super.from(id);
            return this;
        }

        @Override
        public PlatformUserBuilder from(final String email) {
            super.from(email);
            return this;
        }

        public PlatformUserBuilder tier(final PlatformUserTier tier) {
            this.tier = tier;
            return this;
        }

        public PlatformUserBuilder isDemoUser(final boolean isDemoUser) {
            this.isDemoUser = isDemoUser;
            return this;
        }

        public PlatformUserBuilder registeredClientIds(final java.util.function.Consumer<Set<UUID>> registeredClientIdsConsumer) {
            if (this.registeredClientIds == null) {
                this.registeredClientIds = new HashSet<>();
            }
            registeredClientIdsConsumer.accept(this.registeredClientIds);
            return this;
        }

        @Override
        public PlatformUser build() {
            final PlatformUser platformUser = new PlatformUser();
            copyTo(platformUser);
            platformUser.registeredClientIds = this.registeredClientIds == null
                                               ? Collections.emptySet()
                                               : Set.copyOf(this.registeredClientIds);
            platformUser.tier = this.tier;
            platformUser.isDemoUser = this.isDemoUser;
            return platformUser;
        }

    }

}
