package io.github.blakedunaway.authserver.mapper;

import io.github.blakedunaway.authserver.business.model.SigningKey;
import io.github.blakedunaway.authserver.integration.entity.AuthTokenEntity;
import io.github.blakedunaway.authserver.integration.entity.SigningKeyEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper
public abstract class SigningKeyMapper {

    @Mapping(target = "tokens", source = "authTokenIds", qualifiedByName = "idsToTokens")
    public abstract SigningKeyEntity toEntity(final SigningKey signingKey);

    public List<SigningKey> toSigningKeyList(final List<SigningKeyEntity> signingKeyEntityList) {
        return signingKeyEntityList.stream()
                                   .map(sk -> SigningKey.from(sk.getId())
                                                        .keys(sk.getPrivateKey(), sk.getPublicKey())
                                                        .algorithm(sk.getAlgorithm())
                                                        .kid(sk.getKid())
                                                        .signingKeyStatus(sk.getStatus())
                                                        .createdAt(sk.getCreatedAt())
                                                        .authTokens(sk.getTokens() == null ?
                                                                            null :
                                                                            sk.getTokens()
                                                                              .stream()
                                                                              .map(AuthTokenEntity::getTokenId)
                                                                              .collect(Collectors.toSet()))
                                                        .build())
                                   .collect(Collectors.toList());
    }

    public Set<UUID> authTokenEntitySetToAuthTokenIdSet(final Set<AuthTokenEntity> authTokenEntityList) {
        return authTokenEntityList == null ? null : authTokenEntityList.stream()
                                                                       .map(AuthTokenEntity::getTokenId)
                                                                       .collect(Collectors.toSet());
    }

    @Named("idsToTokens")
    public Set<AuthTokenEntity> authTokenIdSetToAuthTokenEntitySet(final Set<UUID> authTokenIdSet) {
        return authTokenIdSet == null ? null : authTokenIdSet.stream()
                                                             .map(AuthTokenEntity::createFromId)
                                                             .collect(Collectors.toSet());
    }

    public SigningKeyEntity kIdToSigningKeyEntity(final String kid) {
        return SigningKeyEntity.createFromKid(kid);
    }

}
