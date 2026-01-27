package io.github.blakedunaway.authserver.business.model;

import io.github.blakedunaway.authserver.business.model.enums.AuthorizationGrantTypeInternal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Authorization {

    private final UUID id;

    private final RegisteredClientModel registeredClientModel;

    private final boolean isNew;

    private final String principalName;

    private final AuthorizationGrantTypeInternal authorizationGrantTypeInternal;

    private final Set<String> authorizedScopes;

    private final Set<AuthToken> tokens;

    public static Builder builder() {
        return new Builder();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder {

        private UUID id;

        private RegisteredClientModel registeredClientModel;

        private boolean isNew;

        private String principalName;

        private AuthorizationGrantTypeInternal grant;

        private Set<String> scopes = new LinkedHashSet<>();

        private Set<AuthToken> tokens = new LinkedHashSet<>();

        protected Builder(final UUID id) {
            this.id = id;
        }

        public Builder isNew(final boolean isNew) {
            this.isNew = isNew;
            return this;
        }

        public Builder tokens(final Set<AuthToken> tokens) {
            this.tokens = tokens;
            return this;
        }

        public Builder scopes(Set<String> scopes) {
            this.scopes = scopes;
            return this;
        }

        public Builder principalName(final String principalName) {
            this.principalName = principalName;
            return this;
        }

        public Builder registeredClient(final RegisteredClientModel registeredClient) {
            this.registeredClientModel = registeredClient;
            return this;
        }

        public Builder authorizationGrantType(final AuthorizationGrantTypeInternal authorizationGrantType) {
            this.grant = authorizationGrantType;
            return this;
        }

        public Builder id(final UUID id) {
            this.id = id;
            return this;
        }

        public Authorization build() {
            Assert.notNull(this.getPrincipalName(), "principalName must not be null");
            Assert.notNull(this.getGrant(), "authorizationGrantType must not be null");

            return new Authorization(
                    this.getId(),
                    this.getRegisteredClientModel(),
                    this.isNew(),
                    this.getPrincipalName(),
                    this.getGrant(),
                    this.getScopes() == null ? Set.of() : Set.copyOf(this.getScopes()),
                    this.getTokens() == null
                    ? Set.of()
                    : this.getTokens()
            );
        }

    }

}
