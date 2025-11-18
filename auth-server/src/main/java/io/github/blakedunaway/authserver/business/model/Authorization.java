package io.github.blakedunaway.authserver.business.model;

import io.github.blakedunaway.authserver.business.model.enums.AuthorizationGrantTypeInternal;
import io.github.blakedunaway.authserver.business.model.enums.TokenType;
import io.github.blakedunaway.authserver.business.service.TokenHasher;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Authorization {

    private final String registeredClientId;

    private final String id;

    private final boolean isNew;

    private final String principalName;

    private final AuthorizationGrantTypeInternal authorizationGrantTypeInternal;

    private final Set<String> authorizedScopes;

    private final Map<String, Object> attributes;

    private final Set<AuthToken> tokens;

    public static Builder fromId(String id) {
        return new Builder(id);
    }

    public static Authorization.Builder fromSpring(final OAuth2Authorization auth) {
        Assert.notNull(auth, "OAuth2Authorization must not be null");
        final Set<OAuth2Authorization.Token<?>> wrappedOAuth2Tokens = extractSpringTokens(auth);
        return new Builder(auth.getId()).registeredClientId(auth.getRegisteredClientId())
                                        .principalName(auth.getPrincipalName())
                                        .scopes(scopes -> scopes.addAll(auth.getAuthorizedScopes()))
                                        .attrs(auth.getAttributes())
                                        .authorizationGrantType(AuthorizationGrantTypeInternal.findByName(auth.getAuthorizationGrantType()
                                                                                                              .getValue()))
                                        .tokens(tokens -> tokens.addAll(wrappedOAuth2Tokens.stream()
                                                                                           .map(AuthToken::fromSpring)
                                                                                           .collect(Collectors.toSet())));

    }

    private static Set<OAuth2Authorization.Token<?>> extractSpringTokens(final OAuth2Authorization oAuth2Authorization) {
        Assert.notNull(oAuth2Authorization, "OAuth2Authorization must not be null");
        final Set<OAuth2Authorization.Token<?>> tokens = new HashSet<>();
        for (final TokenType tokenType : TokenType.values()) {
            final OAuth2Authorization.Token<?> tokenWrapper = oAuth2Authorization.getToken(tokenType.getAssociatedOAuth2TokenClass());
            if (tokenWrapper != null) {
                tokens.add(tokenWrapper);
            }
        }
        return tokens;
    }

    public AuthToken findTokenByRawValue(final String rawValue) {
        Assert.hasText(rawValue, "rawValue");
        String hashed = TokenHasher.hmacCurrent(rawValue);
        return tokens == null ? null :
               tokens.stream()
                     .filter(t -> t.isHashedToken(hashed))
                     .findFirst()
                     .orElse(null);
    }

    public OAuth2Authorization.Builder toSpringAuthorizationWithToken(final RegisteredClient registeredClient, final String rawToken) {
        Assert.notNull(registeredClient, "Registered client must not be null");
        Assert.hasText(rawToken, "rawToken must contain text");
        return attachTokenToSpringAuthorization(OAuth2Authorization.withRegisteredClient(registeredClient), rawToken, findTokenByRawValue(rawToken))
                .principalName(this.getPrincipalName())
                .authorizationGrantType((this.getAuthorizationGrantTypeInternal().getAuthorizationGrantType()))
                .authorizedScopes(Optional.ofNullable(this.getAuthorizedScopes()).orElse(Set.of()))
                .attributes(attrs -> attrs.putAll(Optional.ofNullable(this.getAttributes()).orElse(Map.of())));
    }

    public OAuth2Authorization.Builder toSpringAuthorization(final RegisteredClient registeredClient) {
        Assert.notNull(registeredClient, "Registered client must not be null");
        return attachTokensToSpringAuthorization(OAuth2Authorization.withRegisteredClient(registeredClient))
                .principalName(this.getPrincipalName())
                .authorizationGrantType((this.getAuthorizationGrantTypeInternal().getAuthorizationGrantType()))
                .authorizedScopes(Optional.ofNullable(this.getAuthorizedScopes()).orElse(Set.of()))
                .attributes(attrs -> attrs.putAll(Optional.ofNullable(this.getAttributes()).orElse(Map.of())));
    }

    public OAuth2Authorization.Builder attachTokenToSpringAuthorization(final OAuth2Authorization.Builder builder,
                                                                        final String rawToken,
                                                                        final AuthToken tokenToAttach) {
        Assert.notNull(tokenToAttach, "Auth token must not be null");
        final OAuth2Authorization.Builder updated = tokenToAttach.attachToAuthorization(builder, rawToken);
        return updated;
    }

    public OAuth2Authorization.Builder attachTokensToSpringAuthorization(final OAuth2Authorization.Builder builder) {
        if (this.getTokens() == null || this.getTokens().isEmpty()) {
            return builder;
        }

        this.getTokens().forEach(token -> token.attachToAuthorization(builder, token.getHashedTokenValue()));
        return builder;
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder {

        private boolean isNew;

        private String registeredClientId;

        private String id;

        private String principalName;

        private AuthorizationGrantTypeInternal grant;

        private Set<String> scopes = new LinkedHashSet<>();

        private Map<String, Object> attrs = new LinkedHashMap<>();

        private Set<AuthToken.Builder> tokens = new LinkedHashSet<>();

        protected Builder(final String id) {
            this.id = id;
        }

        public Builder isNew(final boolean isNew) {
            this.isNew = isNew;
            return this;
        }

        public Builder tokens(final Consumer<Set<AuthToken.Builder>> mutator) {
            mutator.accept(this.tokens); // mutate live set
            return this;
        }

        public Builder replaceTokens(final Set<AuthToken.Builder> tokens) {
            Assert.notNull(tokens, "tokens must not be null");
            this.tokens.clear();
            this.tokens.addAll(tokens);
            return this;
        }

        public Builder scopes(final Consumer<Set<String>> scopes) {
            scopes.accept(this.scopes);
            return this;
        }


        public Builder attrs(final Map<String, Object> attrs) {
            Assert.notNull(attrs, "attrs must not be null");
            this.attrs = attrs;
            return this;
        }

        public Builder principalName(final String principalName) {
            this.principalName = principalName;
            return this;
        }

        public Builder registeredClientId(final String registeredClientId) {
            this.registeredClientId = registeredClientId;
            return this;
        }

        public Builder authorizationGrantType(final AuthorizationGrantTypeInternal authorizationGrantType) {
            this.grant = authorizationGrantType;
            return this;
        }

        public Builder id(final String id) {
            this.id = id;
            return this;
        }

        public Authorization build() {
            Assert.notNull(this.getRegisteredClientId(), "registeredClientId must not be null");
            Assert.notNull(this.getPrincipalName(), "principalName must not be null");
            Assert.notNull(this.getGrant(), "authorizationGrantType must not be null");

            return new Authorization(
                    this.getRegisteredClientId(),
                    this.getId(),
                    this.isNew(),
                    this.getPrincipalName(),
                    this.getGrant(),
                    this.getScopes() == null ? Set.of() : Set.copyOf(this.getScopes()),
                    this.getAttrs() == null ? Map.of() : Map.copyOf(this.getAttrs()),
                    this.getTokens() == null
                    ? Set.of()
                    : Set.copyOf(this.getTokens().stream().map(AuthToken.Builder::build).collect(Collectors.toSet()))
            );
        }

    }

}
