package io.github.blakedunaway.authserver.mapper;

import io.github.blakedunaway.authserver.business.model.AuthToken;
import io.github.blakedunaway.authserver.integration.entity.AuthTokenEntity;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;


@Mapper
public abstract class AuthTokenMapper {

    @Named("authTokenEntityToAuthToken")
    public AuthToken authTokenEntityToAuthToken(final AuthTokenEntity authTokenEntity) {
        return AuthToken.fromId(authTokenEntity.getTokenId())
                        .tokenType(authTokenEntity.getTokenType())
                        .hashedTokenValue(authTokenEntity.getTokenValueHash())
                        .subject(authTokenEntity.getSubject())
                        .metadata(metaData -> metaData.putAll(authTokenEntity.getMetadataJson()))
                        .expiresAt(authTokenEntity.getExpiresAt())
                        .issuedAt(authTokenEntity.getIssuedAt())
                        .revokedAt(authTokenEntity.getRevokedAt())
                        .kid(authTokenEntity.getKid())
                        .build();
    }

    public Set<AuthToken> authTokenEntitySetToAuthToken(final Set<AuthTokenEntity> authTokenEntities) {
        return authTokenEntities == null ? null : authTokenEntities.stream().map(this::authTokenEntityToAuthToken).collect(Collectors.toSet());
    }

    @Named("authTokenToAuthTokenEntity")
    public AuthTokenEntity authTokenToAuthTokenEntity(final AuthToken authToken) {
        return AuthTokenEntity.create(authToken.getId(),
                                      authToken.getKid(),
                                      authToken.isNew(),
                                      authToken.getHashedTokenValue(),
                                      authToken.getIssuedAt(),
                                      authToken.getExpiresAt(),
                                      authToken.getRevokedAt(),
                                      authToken.getTokenType(),
                                      authToken.getSubject(),
                                      authToken.getMetadata());
    }

    @Named("authTokenSetToAuthTokenEntitySet")
    @IterableMapping(qualifiedByName = "authTokenToAuthTokenEntity")
    abstract Set<AuthTokenEntity> authTokenSetToAuthTokenEntitySet(final Set<AuthToken> authTokens);

}

