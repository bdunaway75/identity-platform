package io.github.blakedunaway.authserver.business.service;

import io.github.blakedunaway.authserver.business.model.AuthToken;
import io.github.blakedunaway.authserver.business.model.Authorization;
import io.github.blakedunaway.authserver.business.model.enums.TokenType;
import io.github.blakedunaway.authserver.integration.repository.gateway.AuthorizationRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.AuthTokenJpaRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AuthorizationService implements OAuth2AuthorizationService {

    private final AuthorizationRepository authorizationRepository;

    private final RegisteredClientRepository registeredClientRepository;

    private final AuthTokenJpaRepository authTokenJpaRepository;

    public void save(final OAuth2Authorization src) {
        final Authorization.Builder authBuilder = Authorization.fromSpring(src);

        final Authorization persisted = authorizationRepository.findById(authBuilder.getId());

        final Set<AuthToken.Builder> normalized = new java.util.HashSet<>();
        final Set<AuthToken.Builder> loopableTokens = authBuilder.getTokens() == null
                                                      ? new HashSet<>()
                                                      : authBuilder.getTokens()
                                                                   .stream()
                                                                   .map(token -> TokenHasher.isHmacSha256Base64Url(token.getHashedTokenValue())
                                                                                 ? token
                                                                                 : token.hashedTokenValue(TokenHasher.hmacCurrent(token.getHashedTokenValue())))
                                                                   .collect(Collectors.toSet());

        final Map<String, UUID> allExistingIds = authTokenJpaRepository.findIdsByTokenValueHashes(loopableTokens.stream()
                                                                                                                .map(AuthToken.Builder::getHashedTokenValue)
                                                                                                                .collect(Collectors.toSet()))
                                                                       .stream()
                                                                       .map(obj -> new TokenMetaData(obj, authBuilder.getId()))
                                                                       .collect(Collectors.toMap(TokenMetaData::getHashedTokenValue,
                                                                                                 TokenMetaData::getTokenId));
        for (final AuthToken.Builder authToken : loopableTokens) {
            if (allExistingIds.containsKey(authToken.getHashedTokenValue())) {
                normalized.add(authToken.exists(allExistingIds.get(authToken.getHashedTokenValue())));
            } else {
                normalized.add(authToken.isNew(true));
            }
        }

        authBuilder.isNew(persisted == null)
                   .replaceTokens(normalized);

        authorizationRepository.save(authBuilder.build());
    }

    @Override
    public void remove(final OAuth2Authorization authorization) {
        authorizationRepository.remove(authorization.getId());
    }

    @Override
    public OAuth2Authorization findById(final String id) {
        Assert.notNull(id, "id must not be null");

        final Authorization authorization = authorizationRepository.findById(id);
        Assert.notNull(authorization, "authorization must not be null");
        Assert.notNull(authorization.getRegisteredClientId(), "registeredClientId must not be null");

        final RegisteredClient registeredClient = registeredClientRepository.findById(authorization.getRegisteredClientId());
        Assert.notNull(registeredClient, "registered client must not be null");

        return authorizationRepository.findById(id)
                                      .toSpringAuthorization(registeredClient)
                                      .build();
    }

    @Override
    public OAuth2Authorization findByToken(final String rawToken, final OAuth2TokenType tokenType) {
        Assert.notNull(rawToken, "token must not be null");
        Assert.notNull(tokenType, "tokenType must not be null");

        final String hashedToken = TokenHasher.hmacCurrent(rawToken);
        Assert.notNull(hashedToken, "hashedToken must not be null");
        final Authorization authorization;
        if (TokenType.getTokenTypeByWireName(tokenType.getValue()).isEmpty()) {
            //find by attribute
            authorization = authorizationRepository.findByTokenAttribute(tokenType.getValue(), rawToken);
            if (authorization != null) {
                Assert.notNull(authorization.getRegisteredClientId(), "registered client id must not be null");
                final RegisteredClient registeredClient = registeredClientRepository.findById(authorization.getRegisteredClientId());
                Assert.notNull(registeredClient, "registered client must not be null");
                return authorization.toSpringAuthorization(registeredClient).build();
            }
        } else {
            authorization = authorizationRepository.findByToken(hashedToken, tokenType.getValue());
            if (authorization != null) {
                Assert.notNull(authorization.getRegisteredClientId(), "registered client id must not be null");
                final RegisteredClient registeredClient = registeredClientRepository.findById(authorization.getRegisteredClientId());
                Assert.notNull(registeredClient, "registered client must not be null");
                return authorization.toSpringAuthorizationWithToken(registeredClient, rawToken)
                                    .build();
            }
        }
        return null;
    }

    @Getter
    private static class TokenMetaData {

        private final String hashedTokenValue;

        private final UUID tokenId;

        TokenMetaData(final Object[] objects, final String authId) {
            if (objects == null || objects[0] == null || objects[1] == null) {
                throw new IllegalArgumentException("Retrieved invalid results fetching token with Authorization id : " + authId);
            }
            this.hashedTokenValue = (String) objects[0];
            this.tokenId = UUID.fromString(objects[1].toString());

        }

    }

}
