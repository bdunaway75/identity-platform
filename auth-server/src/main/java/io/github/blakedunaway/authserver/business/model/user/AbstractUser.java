package io.github.blakedunaway.authserver.business.model.user;

import io.github.blakedunaway.authserver.business.model.Authorities;
import io.github.blakedunaway.authserver.business.validation.ValidEmail;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AbstractUser {

    protected UUID id;

    @ValidEmail
    private String email;

    private String passwordHash;

    private boolean verified;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Map<String, Object> userAttributes;

    private Set<Authorities> authorities;

    private boolean locked;

    private boolean expired;

    private boolean credentialsExpired;

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
    public abstract static class AbstractUserBuilder<T extends AbstractUserBuilder<T>> {

        protected UUID id;

        protected String email;

        protected String passwordHash;

        protected boolean verified;

        protected LocalDateTime createdAt;

        protected LocalDateTime updatedAt;

        protected Map<String, Object> userAttributes = Collections.emptyMap();

        protected Set<UUID> registeredClientIds = new HashSet<>();

        protected Set<Authorities> authorities = new HashSet<>();

        protected boolean locked;

        protected boolean expired;

        protected boolean credentialsExpired;

        public T email(final String email) {
            this.email = email;
            return this.self();
        }

        public T passwordHash(final String passwordHash) {
            this.passwordHash = passwordHash;
            return this.self();
        }

        public T verified(final boolean verified) {
            this.verified = verified;
            return this.self();
        }

        public T createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this.self();
        }

        public T updatedAt(final LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this.self();
        }

        public T userAttributes(final Map<String, Object> userAttributes) {
            this.userAttributes = userAttributes == null ? Collections.emptyMap() : userAttributes;
            return this.self();
        }

        public T registeredClientIds(final Consumer<Set<UUID>> registeredClientIdsMutator) {
            registeredClientIdsMutator.accept(registeredClientIds);
            return this.self();
        }

        public T authorities(final Consumer<Set<Authorities>> authoritiesMutator) {
            authoritiesMutator.accept(authorities);
            return this.self();
        }

        public T locked(final boolean locked) {
            this.locked = locked;
            return this.self();
        }

        public T expired(final boolean expired) {
            this.expired = expired;
            return this.self();
        }

        public T credentialsExpired(final boolean credentialsExpired) {
            this.credentialsExpired = credentialsExpired;
            return this.self();
        }

        protected abstract T self();

        public abstract AbstractUser build();

        protected void copyTo(final AbstractUser user) {
            Assert.hasText(this.email, "Email address must not be empty");
            Assert.hasText(this.passwordHash, "Password hash must not be empty");
            Assert.isTrue(this.passwordHash.startsWith("$argon2"), "Password has not been hashed");
            Assert.notNull(this.authorities, "Authorities must not be null");
            user.id = this.id;
            user.email = this.email;
            user.passwordHash = this.passwordHash;
            user.verified = this.verified;
            user.createdAt = this.createdAt;
            user.updatedAt = this.updatedAt;
            user.userAttributes = this.userAttributes == null ? Collections.emptyMap() : Map.copyOf(this.userAttributes);
            user.authorities = Set.copyOf(this.authorities);
            user.locked = this.locked;
            user.expired = this.expired;
            user.credentialsExpired = this.credentialsExpired;
        }

        public T from(final AbstractUser user) {
            Assert.notNull(user, "User must not be null");
            this.id = user.getId();
            this.email = user.getEmail();
            this.passwordHash = user.getPasswordHash();
            this.verified = user.isVerified();
            this.createdAt = user.getCreatedAt();
            this.updatedAt = user.getUpdatedAt();
            this.userAttributes = user.getUserAttributes();
            this.authorities = Set.copyOf(user.getAuthorities());
            this.locked = user.isLocked();
            this.expired = user.isExpired();
            this.credentialsExpired = user.isCredentialsExpired();
            return this.self();
        }

        public T from(final UUID id) {
            Assert.notNull(id, "id must not be null");
            this.id = id;
            return this.self();
        }

        public T from(final String email) {
            Assert.notNull(email, "Email address must not be null");
            this.email = email;
            return this.self();
        }
    }
}
