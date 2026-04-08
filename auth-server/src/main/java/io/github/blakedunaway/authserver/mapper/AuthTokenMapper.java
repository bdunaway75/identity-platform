package io.github.blakedunaway.authserver.mapper;

import io.github.blakedunaway.authserver.business.model.AuthToken;
import io.github.blakedunaway.authserver.integration.entity.AuthTokenEntity;
import io.github.blakedunaway.authserver.security.token.TokenHasher;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

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

    // This function ensures token are hashed before turning to entities
    @Named("authTokenToAuthTokenEntity")
    public AuthTokenEntity authTokenToAuthTokenEntity(final AuthToken authToken) {
        final String hashedTokenValue = TokenHasher.isHmacSha256Base64Url(authToken.getHashedTokenValue())
                                        ? authToken.getHashedTokenValue()
                                        : TokenHasher.hmacCurrent(authToken.getHashedTokenValue());
        return AuthTokenEntity.create(authToken.getId(),
                                      authToken.getKid(),
                                      hashedTokenValue,
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

