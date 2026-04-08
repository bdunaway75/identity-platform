package io.github.blakedunaway.authserver.business.model.user;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientUser extends AbstractUser {

    private String clientId;

    public static ClientUserBuilder from(final ClientUser clientUser) {
        return new ClientUserBuilder().from(clientUser);
    }

    public static ClientUserBuilder from(final UUID id) {
        return new ClientUserBuilder().from(id);
    }

    public static ClientUserBuilder from(final String email) {
        return new ClientUserBuilder().from(email);
    }

    @Getter

    public static class ClientUserBuilder extends AbstractUser.AbstractUserBuilder<ClientUserBuilder> {

        private String clientId;

        public ClientUserBuilder clientId(final String clientId) {
            this.clientId = clientId;
            return this;
        }

        @Override
        protected ClientUserBuilder self() {
            return this;
        }

        public ClientUserBuilder from(final ClientUser user) {
            super.from(user);
            this.clientId = user.getClientId();
            return this;
        }

        public ClientUserBuilder from(final UUID id) {
            super.from(id);
            return this;
        }

        public ClientUserBuilder from(final String email) {
            super.from(email);
            return this;
        }

        @Override
        public ClientUser build() {
            final ClientUser clientUser = new ClientUser();
            copyTo(clientUser);
            clientUser.clientId = this.clientId;
            return clientUser;
        }

    }

}
