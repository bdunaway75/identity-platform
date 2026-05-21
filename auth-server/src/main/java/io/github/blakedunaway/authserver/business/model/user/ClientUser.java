package io.github.blakedunaway.authserver.business.model.user;

import io.github.blakedunaway.authserver.business.api.dto.request.ClientUserRequest;
import io.github.blakedunaway.authserver.business.model.Authority;
import io.github.blakedunaway.authserver.util.AuthorityUtility;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public static ClientUserBuilder verified(final String email,
                                             final String passwordHash,
                                             final String clientId) {
        return ClientUser.from(email)
                         .email(email)
                         .clientId(clientId)
                         .expired(false)
                         .credentialsExpired(false)
                         .verified(true)
                         .userAttributes(Collections.emptyMap())
                         .passwordHash(passwordHash);
    }

    public ClientUser applyRequest(final ClientUserRequest request) {
        if (request == null) {
            return this;
        }

        return ClientUser.from(this)
                         .email(request.getEmail() != null ? request.getEmail() : getEmail())
                         .verified(request.getVerified() != null ? request.getVerified() : isVerified())
                         .locked(request.getLocked() != null ? request.getLocked() : isLocked())
                         .expired(request.getExpired() != null ? request.getExpired() : isExpired())
                         .credentialsExpired(request.getCredentialsExpired() != null
                                             ? request.getCredentialsExpired()
                                             : isCredentialsExpired())
                         .userAttributes(request.getUserAttributes() != null ? request.getUserAttributes() : getUserAttributes())
                         .authorities(resolvedAuthorities -> {
                             resolvedAuthorities.clear();
                             if (request.getAuthorities() == null) {
                                 resolvedAuthorities.addAll(getAuthorities());
                                 return;
                             }

                             resolvedAuthorities.addAll(AuthorityUtility.normalizeAuthorityAndRoleNames(request.getAuthorities())
                                                                        .stream()
                                                                        .map(Authority::from)
                                                                        .collect(Collectors.toSet()));
                         })
                         .build();
    }

    public ClientUser resetPassword(final String passwordHash) {
        return ClientUser.from(this)
                         .passwordHash(passwordHash)
                         .expired(false)
                         .credentialsExpired(false)
                         .build();
    }

    public ClientUser removeAuthoritiesNamed(final Set<String> authorityNames) {
        if (authorityNames == null || authorityNames.isEmpty() || getAuthorities() == null || getAuthorities().isEmpty()) {
            return this;
        }

        final Set<String> normalizedAuthorityNames = AuthorityUtility.normalizeAuthorityAndRoleNames(authorityNames);
        final Set<Authority> retainedAuthorities = getAuthorities().stream()
                                                                   .filter(authority -> authority != null
                                                                                        && authority.getName() != null
                                                                                        && !normalizedAuthorityNames.contains(authority.getName().toUpperCase()))
                                                                   .collect(Collectors.toSet());
        if (retainedAuthorities.size() == getAuthorities().size()) {
            return this;
        }

        return ClientUser.from(this)
                         .authorities(authorities -> {
                             authorities.clear();
                             authorities.addAll(retainedAuthorities);
                         })
                         .build();
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

        @Override
        public ClientUserBuilder from(final UUID id) {
            super.from(id);
            return this;
        }

        @Override
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
