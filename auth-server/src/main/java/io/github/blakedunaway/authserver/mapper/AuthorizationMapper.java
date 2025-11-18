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

    static OAuth2AuthorizationRequest toAuthReq(final Map<String, Object> map) {
        final OAuth2AuthorizationRequest.Builder authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                                                                                                  .authorizationUri((String) map.get(
                                                                                                          "authorizationUri"))
                                                                                                  .clientId((String) map.get("clientId"));

        final String redirect = (String) map.get("redirectUri");
        if (redirect != null) {
            authorizationRequest.redirectUri(redirect);
        }

        final List<String> scopes = (List<String>) map.getOrDefault("scopes", java.util.List.of());
        if (!scopes.isEmpty()) {
            authorizationRequest.scopes(new java.util.LinkedHashSet<>(scopes));
        }

        final String state = (String) map.get("state");
        if (state != null) {
            authorizationRequest.state(state);
        }

        final Map<String, Object> additionalParameters = (java.util.Map<String, Object>) map.getOrDefault("additionalParameters", java.util.Map.of());
        if (!additionalParameters.isEmpty()) {
            authorizationRequest.additionalParameters(additionalParameters);
        }

        final Map<String, Object> attrs = (Map<String, Object>) map.getOrDefault("attributes", Map.of());
        if (!attrs.isEmpty()) {
            authorizationRequest.attributes(a -> a.putAll(attrs));
        }

        final String uri = (String) map.get("authorizationRequestUri");
        if (uri != null) {
            authorizationRequest.authorizationRequestUri(uri);
        }

        return authorizationRequest.build();
    }

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

        // TODO test if jackson module properly serializes OAuth2Request class after fetching from DB
//        final String key = OAuth2AuthorizationRequest.class.getName();
//        final Object clazzContents = attributes.get(key);
//        if (clazzContents != null && !(clazzContents instanceof OAuth2AuthorizationRequest)) {
//            attributes.put(key, toAuthReq((Map<String, Object>) clazzContents));
//        }

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

