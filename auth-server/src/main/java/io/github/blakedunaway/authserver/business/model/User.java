package io.github.blakedunaway.authserver.business.model;

import io.github.blakedunaway.authserver.business.validation.ValidEmail;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class User {

    @ValidEmail
    private final String email;

    private final boolean isNew;

    private final String passwordHash;

    private final String registeredClientId;

    private final boolean verified;

    private final LocalDateTime createdAt;

    private final LocalDateTime updatedAt;

    private final String plan;

    private final Set<Authorities> authorities;

    private final boolean locked;

    private final boolean expired;

    private final boolean credentialsExpired;

    private final UUID id;

    public static Builder fromId(final UUID id) {
        Assert.notNull(id, "id must not be null");
        return new Builder(id);
    }

    public static Builder fromEmail(final String email) {
        Assert.notNull(email, "email must not be null");
        return new Builder(email);
    }

    public static Builder fromUser(final User user) {
        Assert.notNull(user, "user must not be null");
        return new Builder(user);
    }

    public UserDetails toSpring() {
        return org.springframework.security.core.userdetails.User.withUsername(this.getEmail())
                                                                 .password(this.getPasswordHash())
                                                                 .authorities(this.getAuthorities()
                                                                                  .stream()
                                                                                  .map(Authorities::toSimpleGrantedAuthority)
                                                                                  .collect(Collectors.toSet()))
                                                                 .accountExpired(this.isExpired())
                                                                 .accountLocked(this.isLocked())
                                                                 .credentialsExpired(this.isCredentialsExpired())
                                                                 .disabled(this.isLocked())
                                                                 .build();
    }

    @Getter
    public static class Builder {

        private UUID id;

        private String email;

        private boolean isNew;

        private String registeredClientId;

        private String passwordHash;

        private boolean verified;

        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;

        private String plan;

        private Set<Authorities> authorities = new HashSet<>();

        private boolean locked;

        private boolean expired;

        private boolean credentialsExpired;

        protected Builder(final UUID id) {
            this.id = id;
        }

        protected Builder(final String email) {
            this.email = email;
        }

        protected Builder(final User user) {
            this.id = user.getId();
            this.email = user.getEmail();
            this.isNew = user.isNew();
            this.registeredClientId = user.getRegisteredClientId();
            this.passwordHash = user.getPasswordHash();
            this.verified = user.isVerified();
            this.createdAt = user.getCreatedAt();
            this.updatedAt = user.getUpdatedAt();
            this.plan = user.getPlan();
            this.authorities = user.getAuthorities();
            this.locked = user.isLocked();
            this.expired = user.isExpired();
            this.credentialsExpired = user.isCredentialsExpired();
        }

        public Builder email(final String email) {
            this.email = email;
            return this;
        }

        public Builder passwordHash(final String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder isNew(final boolean isNew) {
            this.isNew = isNew;
            return this;
        }

        public Builder verified(final boolean verified) {
            this.verified = verified;
            return this;
        }

        public Builder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(final LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder plan(final String plan) {
            this.plan = plan;
            return this;
        }

        public Builder authorities(final Consumer<Set<Authorities>> authoritiesMutator) {
            authoritiesMutator.accept(authorities);
            return this;
        }

        public Builder locked(final boolean locked) {
            this.locked = locked;
            return this;
        }

        public Builder expired(final boolean expired) {
            this.expired = expired;
            return this;
        }

        public Builder credentialsExpired(final boolean credentialsExpired) {
            this.credentialsExpired = credentialsExpired;
            return this;
        }

        public Builder registeredClientId(final String registeredClientId) {
            this.registeredClientId = registeredClientId;
            return this;
        }

        public User build() {
            Assert.hasText(this.getEmail(), "Email address must not be empty");
            Assert.hasText(this.getPasswordHash(), "Password hash must not be empty");
            Assert.isTrue(this.getPasswordHash().startsWith("$argon2"), "Password has not been hashed");
            Assert.notNull(this.getPlan(), "Plan must not be null");
            Assert.notNull(this.getAuthorities(), "Authorities must not be null");
            Assert.notNull(this.getRegisteredClientId(), "Registered client id must not be null");
            Assert.notNull(this.isNew(), "New user must not be null");
            return new User(this.getEmail(),
                            this.isNew(),
                            this.getPasswordHash(),
                            this.getRegisteredClientId(),
                            this.isVerified(),
                            this.getCreatedAt(),
                            this.getUpdatedAt(),
                            this.getPlan(),
                            Set.copyOf(this.getAuthorities()),
                            this.isLocked(),
                            this.isExpired(),
                            this.isCredentialsExpired(),
                            this.getId());
        }
    }
}
