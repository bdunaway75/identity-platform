package io.github.blakedunaway.authserver.business.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthorizationConsent {

    private final UUID consentId;

    private final UUID registeredClientId;

    private final String principalName;

    private final Set<Authority> authorities;

    public static Builder fromId(final UUID id) {
        return new Builder(id);
    }

    public OAuth2AuthorizationConsent toSpring() {
        return OAuth2AuthorizationConsent.withId(String.valueOf(this.getRegisteredClientId()), this.getPrincipalName())
                                         .authorities(auth ->
                                                              auth.addAll(this.getAuthorities()
                                                                              .stream()
                                                                              .map(Authority::toSimpleGrantedAuthority)
                                                                              .collect(Collectors.toSet())))
                                         .build();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder {

        private UUID consentId;

        private UUID registeredClientId;

        private String principalName;

        private Set<Authority> authorities = new HashSet<>();

        protected Builder(final UUID consentId) {
            this.consentId = consentId;
        }

        public static AuthorizationConsent fromSpring(final OAuth2AuthorizationConsent authorizationConsent) {
            final Builder builder = new Builder();
            builder.registeredClientId = UUID.fromString(authorizationConsent.getRegisteredClientId());
            builder.principalName = authorizationConsent.getPrincipalName();
            builder.authorities = authorizationConsent.getAuthorities()
                                                      .stream()
                                                      .filter(Objects::nonNull)
                                                      .map(Authority::from)
                                                      .map(authority -> authority.toBuilder().isNew(true).build())
                                                      .collect(Collectors.toSet());
            return builder.build();
        }

        public Builder registeredClientId(final UUID registeredClientId) {
            this.registeredClientId = registeredClientId;
            return this;
        }

        public Builder principalName(final String principalName) {
            this.principalName = principalName;
            return this;
        }

        public Builder authorities(final Consumer<Set<Authority>> authoritiesMutator) {
            authoritiesMutator.accept(this.getAuthorities());
            return this;
        }

        public AuthorizationConsent build() {
            Assert.notNull(this.getRegisteredClientId(), "registeredClientId must not be null");
            Assert.notNull(this.getPrincipalName(), "principalName must not be null");
            Assert.notNull(this.getAuthorities(), "authorities must not be null");
            return new AuthorizationConsent(this.getConsentId(),
                                            this.getRegisteredClientId(),
                                            this.getPrincipalName(),
                                            Set.copyOf(this.getAuthorities()));
        }

    }

}
