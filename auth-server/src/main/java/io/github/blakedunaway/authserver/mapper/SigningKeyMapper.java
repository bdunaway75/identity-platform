package io.github.blakedunaway.authserver.mapper;

import io.github.blakedunaway.authserver.business.model.SigningKey;
import io.github.blakedunaway.authserver.integration.entity.AuthTokenEntity;
import io.github.blakedunaway.authserver.integration.entity.SigningKeyEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(uses = AuthTokenMapper.class)
public abstract class SigningKeyMapper {

    @Autowired
    AuthTokenMapper authTokenMapper;

    @Mapping(target = "tokens", qualifiedByName = "authTokenToAuthTokenEntity")
    public abstract SigningKeyEntity toEntity(final SigningKey signingKey);

    public SigningKey toSigningKey(final SigningKeyEntity signingKeyEntity) {
        return SigningKey.from(signingKeyEntity.getId())
                         .keys(signingKeyEntity.getPrivateKey(), signingKeyEntity.getPublicKey())
                         .signingKeyStatus(signingKeyEntity.getStatus())
                         .algorithm(signingKeyEntity.getAlgorithm())
                         .createdAt(signingKeyEntity.getCreatedAt())
                         .authTokens(authTokenMapper.authTokenEntitySetToAuthToken(signingKeyEntity.getTokens()))
                         .kid(signingKeyEntity.getKid())
                         .build();
    }

    public List<SigningKey> toSigningKeyList(final List<SigningKeyEntity> signingKeyEntityList) {
        return signingKeyEntityList.stream()
                                   .map(sk -> SigningKey.from(sk.getId())
                                                        .keys(sk.getPrivateKey(), sk.getPublicKey())
                                                        .algorithm(sk.getAlgorithm())
                                                        .kid(sk.getKid())
                                                        .signingKeyStatus(sk.getStatus())
                                                        .createdAt(sk.getCreatedAt())
                                                        .authTokens(sk.getTokens() == null
                                                                    ? null
                                                                    : authTokenMapper.authTokenEntitySetToAuthToken(sk.getTokens()))
                                                        .build())
                                   .collect(Collectors.toList());
    }

    public Set<UUID> authTokenEntitySetToAuthTokenIdSet(final Set<AuthTokenEntity> authTokenEntityList) {
        return authTokenEntityList == null
               ? null
               : authTokenEntityList.stream()
                                    .map(AuthTokenEntity::getTokenId)
                                    .collect(Collectors.toSet());
    }

}
