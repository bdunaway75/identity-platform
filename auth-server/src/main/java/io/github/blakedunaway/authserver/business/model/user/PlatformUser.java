package io.github.blakedunaway.authserver.business.model.user;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlatformUser extends AbstractUser {

    private Set<UUID> registeredClientIds;

    public static PlatformUserBuilder from(final PlatformUser platformUser) {
        return new PlatformUserBuilder().from(platformUser);
    }

    public static PlatformUserBuilder from(final String email) {
        return new PlatformUserBuilder().from(email);
    }

    @Getter
    public static class PlatformUserBuilder extends AbstractUser.AbstractUserBuilder<PlatformUserBuilder> {

        @Override
        protected PlatformUserBuilder self() {
            return this;
        }

        public PlatformUserBuilder from(final PlatformUser user) {
            super.from(user);
            this.registeredClientIds = user.getRegisteredClientIds();
            return this;
        }

        public PlatformUserBuilder from(final String email) {
            super.from(email);
            return this;
        }

        @Override
        public PlatformUser build() {
            final PlatformUser platformUser = new PlatformUser();
            copyTo(platformUser);
            platformUser.registeredClientIds = this.registeredClientIds == null
                                               ? Collections.emptySet()
                                               : Set.copyOf(this.registeredClientIds);
            return platformUser;
        }

    }

}
