package io.github.blakedunaway.authserver.mapper;

import io.github.blakedunaway.authserver.business.model.AuthToken;
import io.github.blakedunaway.authserver.business.model.Authorization;
import io.github.blakedunaway.authserver.business.model.enums.AuthorizationGrantTypeInternal;
import io.github.blakedunaway.authserver.integration.entity.AuthTokenEntity;
import io.github.blakedunaway.authserver.integration.entity.AuthorizationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class AuthorizationMapper {

    private final AuthTokenMapper authTokenMapper;

    public AuthorizationEntity authorizationToAuthorizationEntity(final Authorization authorization) {
        UUID id = null;
        if (authorization.getId() != null) {
            try {
                id = UUID.fromString(authorization.getId());
            } catch (Exception e) {
                throw new IllegalArgumentException("Authorization.id must be a valid UUID: " + authorization.getId(), e);
            }
        }

        final Set<AuthTokenEntity> tokenEntities = Optional.ofNullable(authorization.getTokens())
                                                           .orElseGet(Set::of)
                                                           .stream()
                                                           .map(authTokenMapper::authTokenToAuthTokenEntity)
                                                           .collect(Collectors.toSet());

        return AuthorizationEntity.create(
                id,
                authorization.isNew(),
                authorization.getRegisteredClientId(),
                authorization.getPrincipalName(),
                authorization.getAuthorizationGrantTypeInternal()
                             .getWireName(),
                authorization.getAuthorizedScopes(),
                authorization.getAttributes(),
                tokenEntities
        );
    }

    public Authorization authorizationEntityToAuthorization(final AuthorizationEntity entity) {
        final Set<AuthToken> tokens = Optional.ofNullable(entity.getTokens())
                                              .orElseGet(Set::of)
                                              .stream()
                                              .map(authTokenMapper::authTokenEntityToAuthToken)
                                              .collect(Collectors.toSet());

        final Set<String> scopes = Optional.ofNullable(entity.getAuthorizedScopes())
                                           .orElseGet(Set::of);

        final Map<String, Object> attributes = Optional.ofNullable(entity.getAttributes())
                                                       .orElseGet(Map::of);

        return Authorization.fromId(entity.getAuthId().toString())
                            .registeredClientId(entity.getRegisteredClientId())
                            .principalName(entity.getPrincipalName())
                            .authorizationGrantType(AuthorizationGrantTypeInternal.findByName(entity.getAuthorizationGrantType()))
                            .scopes(scopesMutator -> scopesMutator.addAll(scopes))
                            .attrs(attributes)
                            .tokens(tokensMutator ->
                                            tokensMutator.addAll(
                                                    tokens.stream()
                                                          .map(AuthToken::toBuilder)
                                                          .collect(Collectors.toSet()))
                            )
                            .isNew(entity.isNew())
                            .build();
    }

}

