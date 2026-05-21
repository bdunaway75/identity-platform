package io.github.blakedunaway.authserver.business.model.user;

import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import io.github.blakedunaway.authserver.business.model.Authority;
import io.github.blakedunaway.authserver.util.AuthorityUtility;
import jakarta.validation.ValidationException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlatformUser extends AbstractUser {

    private Set<UUID> registeredClientIds;

    private PlatformUserTier tier;

    private boolean isDemoUser;

    public static PlatformUserBuilder from(final PlatformUser platformUser) {
        return new PlatformUserBuilder().from(platformUser);
    }

    public static PlatformUserBuilder from(final UUID id) {
        return new PlatformUserBuilder().from(id);
    }

    public static PlatformUserBuilder from(final String email) {
        return new PlatformUserBuilder().from(email);
    }

    public static PlatformUserBuilder verified(final String email, final String passwordHash) {
        return PlatformUser.from(email)
                           .email(email)
                           .registeredClientIds(Set::clear)
                           .authorities(authorities -> authorities.add(Authority.from(AuthorityUtility.ROLE_PLATFORM_USER)))
                           .expired(false)
                           .credentialsExpired(false)
                           .verified(true)
                           .userAttributes(Collections.emptyMap())
                           .passwordHash(passwordHash)
                           .isDemoUser(false);
    }

    public Set<UUID> registeredClientIdsOrEmpty() {
        return getRegisteredClientIds() == null ? Collections.emptySet() : getRegisteredClientIds();
    }

    public Set<UUID> filterOwnedRegisteredClientIds(final Set<UUID> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            return Collections.emptySet();
        }

        final Set<UUID> ownedIds = registeredClientIdsOrEmpty();
        return requestedIds.stream()
                           .filter(ownedIds::contains)
                           .collect(Collectors.toCollection(HashSet::new));
    }

    public boolean ownsRegisteredClientId(final UUID registeredClientId) {
        return registeredClientId != null && registeredClientIdsOrEmpty().contains(registeredClientId);
    }

    public boolean ownsAllRegisteredClientIds(final Set<UUID> registeredClientIds) {
        return registeredClientIds != null
               && !registeredClientIds.isEmpty()
               && filterOwnedRegisteredClientIds(registeredClientIds).size() == registeredClientIds.size();
    }

    public boolean ownsAnyClientIds(final Set<RegisteredClientModel> registeredClients,
                                    final Set<String> requestedClientIds) {
        if (registeredClients == null || registeredClients.isEmpty() || requestedClientIds == null || requestedClientIds.isEmpty()) {
            return false;
        }

        return registeredClients.stream()
                                .map(RegisteredClientModel::getClientId)
                                .anyMatch(requestedClientIds::contains);
    }

    public RegisteredClientModel findOwnedRegisteredClientById(final Set<RegisteredClientModel> registeredClients,
                                                               final UUID registeredClientId) {
        if (!ownsRegisteredClientId(registeredClientId) || registeredClients == null || registeredClients.isEmpty()) {
            return null;
        }

        return registeredClients.stream()
                                .filter(client -> client != null && client.hasId(registeredClientId))
                                .findFirst()
                                .orElse(null);
    }

    public Set<RegisteredClientModel> replaceRegisteredClientAndValidateTier(final Set<RegisteredClientModel> registeredClients,
                                                                             final RegisteredClientModel updatedRegisteredClient) {
        final Set<RegisteredClientModel> resolvedRegisteredClients = registeredClients == null
                                                                     ? new HashSet<>()
                                                                     : new HashSet<>(registeredClients);
        if (updatedRegisteredClient == null) {
            return resolvedRegisteredClients;
        }

        resolvedRegisteredClients.removeIf(client -> client != null && client.hasId(updatedRegisteredClient.getId()));
        resolvedRegisteredClients.add(updatedRegisteredClient);
        validateTierCompliance(resolvedRegisteredClients);
        return Set.copyOf(resolvedRegisteredClients);
    }

    public PlatformUser attachRegisteredClientId(final UUID registeredClientId) {
        Assert.notNull(registeredClientId, "RegisteredClientId cannot be null");

        return PlatformUser.from(this)
                           .registeredClientIds(ids -> ids.add(registeredClientId))
                           .build();
    }

    public PlatformUser withDefaultTierIfMissing() {
        if (getTier() != null) {
            return this;
        }

        return PlatformUser.from(this)
                           .tier(PlatformUserTier.builder()
                                                 .name("FREE")
                                                 .build())
                           .build();
    }

    public PlatformUser resetPassword(final String passwordHash) {
        return PlatformUser.from(this)
                           .passwordHash(passwordHash)
                           .expired(false)
                           .credentialsExpired(false)
                           .build();
    }

    public boolean isPlatformAdmin() {
        return getAuthorities() != null
               && getAuthorities().stream()
                                  .map(Authority::getName)
                                  .collect(Collectors.toSet())
                                  .containsAll(Set.of("ROLE_PLATFORM_ADMIN", "PLATFORM_ADMIN_ACCESS"));
    }

    public boolean canUpgradeTo(final PlatformUserTier requestedTier) {
        return requestedTier != null && getTier() != null && requestedTier.isHigherThan(getTier());
    }

    public boolean canDowngradeTo(final PlatformUserTier requestedTier) {
        return requestedTier != null && getTier() != null && requestedTier.isLowerThan(getTier());
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

        final List<String> errors = new ArrayList<>();
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

        public PlatformUserBuilder registeredClientIds(final Consumer<Set<UUID>> registeredClientIdsConsumer) {
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
