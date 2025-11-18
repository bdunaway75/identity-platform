package io.github.blakedunaway.authserver.mapper;

import io.github.blakedunaway.authserver.business.model.AuthToken;
import io.github.blakedunaway.authserver.integration.entity.AuthTokenEntity;
import org.springframework.stereotype.Component;


@Component
public class AuthTokenMapper {

    AuthToken authTokenEntityToAuthToken(final AuthTokenEntity authTokenEntity) {
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

    AuthTokenEntity authTokenToAuthTokenEntity(final AuthToken authToken) {
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

}

