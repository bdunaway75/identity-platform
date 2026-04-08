package io.github.blakedunaway.authserver.mapper;

import io.github.blakedunaway.authserver.business.model.AuthToken;
import io.github.blakedunaway.authserver.business.model.Authorization;
import io.github.blakedunaway.authserver.business.model.enums.AuthorizationGrantTypeInternal;
import io.github.blakedunaway.authserver.business.model.enums.TokenType;
import io.github.blakedunaway.authserver.config.redis.RedisStore;
import io.github.blakedunaway.authserver.integration.entity.AuthTokenEntity;
import io.github.blakedunaway.authserver.integration.entity.AuthorizationEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class AuthorizationMapper {

    private static final String REDIS_HANDLE = "auth:attrs:";

    private final AuthTokenMapper authTokenMapper;

    private final RegisteredClientMapper registeredClientMapper;

    private final RedisStore redisStore;

    public AuthorizationEntity authorizationToAuthorizationEntity(final Authorization authorization) {

        final Set<AuthTokenEntity> tokenEntities = Optional.ofNullable(authorization.getTokens())
                                                           .orElseGet(Set::of)
                                                           .stream()
                                                           .map(authTokenMapper::authTokenToAuthTokenEntity)
                                                           .collect(Collectors.toSet());

        return AuthorizationEntity.create(
                authorization.getId(),
                RegisteredClientEntity.createFromId(authorization.getRegisteredClientModel().getId()),
                authorization.getPrincipalName(),
                authorization.getAuthorizationGrantTypeInternal().getWireName(),
                authorization.getAuthorizedScopes(),
                tokenEntities
        );
    }

    public Authorization authorizationEntityToAuthorization(final AuthorizationEntity entity) {
        final Set<AuthToken> tokens = Optional.ofNullable(entity.getTokens())
                                              .orElseGet(Set::of)
                                              .stream()
                                              .map(authTokenMapper::authTokenEntityToAuthToken)
                                              .collect(Collectors.toSet());

        return Authorization.builder()
                            .id(entity.getAuthId())
                            .registeredClient(registeredClientMapper.registeredClientEntityToRegisteredClientModel(
                                    entity.getRegisteredClient()))
                            .principalName(entity.getPrincipalName())
                            .authorizationGrantType(AuthorizationGrantTypeInternal.findByName(entity.getAuthorizationGrantType()))
                            .scopes(entity.getAuthorizedScopes())
                            .tokens(tokens)
                            .build();
    }

    public AuthorizationEntity oAuth2AuthorizationToAuthorizationEntity(final OAuth2Authorization authorization,
                                                                        final RegisteredClientEntity clientEntity,
                                                                        final boolean isNew) {
        final Set<AuthTokenEntity> tokenEntities = Optional.of(TokenType.retrieveFromSpring(authorization))
                                                           .orElseGet(Set::of)
                                                           .stream()
                                                           .map(authTokenMapper::authTokenToAuthTokenEntity)
                                                           .collect(Collectors.toSet());
        return AuthorizationEntity.create(
                isNew ? null : UUID.fromString(authorization.getId()),
                clientEntity,
                authorization.getPrincipalName(),
                authorization.getAuthorizationGrantType().getValue(),
                authorization.getAuthorizedScopes(),
                tokenEntities
        );

    }

    public OAuth2Authorization authorizationToOAuth2Authorization(final Authorization authorization) {
        OAuth2Authorization.Builder builder =
                OAuth2Authorization
                        .withRegisteredClient(registeredClientMapper.registeredClientModelToRegisteredClient(authorization.getRegisteredClientModel()))
                        .id(String.valueOf(authorization.getId()))
                        .principalName(authorization.getPrincipalName())
                        .authorizationGrantType(authorization.getAuthorizationGrantTypeInternal()
                                                             .getAuthorizationGrantType())
                        .authorizedScopes(authorization.getAuthorizedScopes())
                        .attributes(attrs -> attrs.putAll(redisStore.get(REDIS_HANDLE + authorization.getId())));

        authorization.getTokens().forEach(token -> builder.token(token.toOAuth2Token()));
        return builder.build();
    }

}

