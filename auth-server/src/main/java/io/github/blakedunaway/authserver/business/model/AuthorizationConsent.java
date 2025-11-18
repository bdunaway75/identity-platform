package io.github.blakedunaway.authserver.business.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.util.Assert;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthorizationConsent {

    private final UUID consentId;

    private final boolean isNew;

    private final String registeredClientId;

    private final String principalName;

    private final Set<Authorities> authorities;

    public static Builder fromId(final UUID id) {
        return new Builder(id);
    }

    public OAuth2AuthorizationConsent toSpring() {
        return OAuth2AuthorizationConsent.withId(this.getRegisteredClientId(), this.getPrincipalName())
                                         .authorities(auth -> auth.addAll(this.getAuthorities()
                                                                              .stream()
                                                                              .map(Authorities::toSimpleGrantedAuthority)
                                                                              .collect(Collectors.toSet())))
                                         .build();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder {

        private UUID consentId;

        private boolean isNew;

        private String registeredClientId;

        private String principalName;

        private Set<Authorities> authorities = new LinkedHashSet<>();

        protected Builder(final UUID consentId) {
            this.consentId = consentId;
        }

        public static Builder fromSpring(final OAuth2AuthorizationConsent authorizationConsent) {
            final Builder builder = new Builder();
            builder.consentId = UUID.randomUUID();
            builder.isNew = true;
            builder.registeredClientId = authorizationConsent.getRegisteredClientId();
            builder.principalName = authorizationConsent.getPrincipalName();
            builder.authorities = authorizationConsent.getAuthorities()
                                                      .stream()
                                                      .filter(Objects::nonNull)
                                                      .map(Authorities::from)
                                                      .collect(Collectors.toSet());
            return builder;
        }

        public Builder consentId(final UUID consentId) {
            this.consentId = consentId;
            return this;
        }

        public Builder isNew(final boolean isNew) {
            this.isNew = isNew;
            return this;
        }

        public Builder registeredClientId(final String registeredClientId) {
            this.registeredClientId = registeredClientId;
            return this;
        }

        public Builder principalName(final String principalName) {
            this.principalName = principalName;
            return this;
        }

        public Builder authorities(final Consumer<Set<Authorities>> authoritiesMutator) {
            authoritiesMutator.accept(this.getAuthorities());
            return this;
        }

        public AuthorizationConsent build() {
            Assert.notNull(this.getConsentId(), "consentId must not be null");
            Assert.notNull(this.isNew(), "isNew must not be null");
            Assert.notNull(this.getRegisteredClientId(), "registeredClientId must not be null");
            Assert.notNull(this.getPrincipalName(), "principalName must not be null");
            Assert.notNull(this.getAuthorities(), "authorities must not be null");
            return new AuthorizationConsent(this.getConsentId(),
                                            this.isNew(),
                                            this.getRegisteredClientId(),
                                            this.getPrincipalName(),
                                            Set.copyOf(this.getAuthorities()));
        }

    }

}
