package io.github.blakedunaway.authserver.integration.repository.implementation;

import io.github.blakedunaway.authserver.business.model.SigningKey;
import io.github.blakedunaway.authserver.business.model.enums.SigningKeyStatus;
import io.github.blakedunaway.authserver.integration.entity.SigningKeyEntity;
import io.github.blakedunaway.authserver.integration.repository.gateway.SigningKeyRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.SigningKeyJpaRepository;
import io.github.blakedunaway.authserver.mapper.SigningKeyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SigningKeyRepositoryImpl implements SigningKeyRepository {

    private final SigningKeyJpaRepository signingKeyJpaRepository;

    private final SigningKeyMapper signingKeyMapper;

    @Transactional
    @Override
    public SigningKey save(final SigningKey signingKey) {
        final Optional<SigningKeyEntity> signingKeyEntity = signingKeyJpaRepository.findByKid(signingKey.getKid());
        if (signingKeyEntity.isPresent()) {
            final SigningKey updated = SigningKey.from(signingKeyEntity.get().getId())
                                                 .encoded(signingKey.isEncoded())
                                                 .keys(signingKey.getPrivateKey(), signingKey.getPublicKey())
                                                 .signingKeyStatus(signingKey.getStatus())
                                                 .authTokens(signingKey.getAuthTokenIds())
                                                 .algorithm(signingKey.getAlgorithm())
                                                 .createdAt(signingKey.getCreatedAt())
                                                 .kid(signingKey.getKid())
                                                 .build();
            signingKeyJpaRepository.save(signingKeyMapper.toEntity(updated));
            return updated;
        }
        final SigningKeyEntity newKey = signingKeyJpaRepository.save(signingKeyMapper.toEntity(signingKey));
        return SigningKey.from(newKey.getId())
                         .encoded(signingKey.isEncoded())
                         .keys(newKey.getPrivateKey(), newKey.getPublicKey())
                         .signingKeyStatus(newKey.getStatus())
                         .authTokens(signingKeyMapper.authTokenEntitySetToAuthTokenIdSet(newKey.getTokens()))
                         .algorithm(newKey.getAlgorithm())
                         .createdAt(newKey.getCreatedAt())
                         .kid(newKey.getKid())
                         .build();
    }

    @Override
    public List<SigningKey> findByStatus(final SigningKeyStatus status) {
        return signingKeyMapper.toSigningKeyList(signingKeyJpaRepository.findByStatus(status));
    }

    @Override
    public List<SigningKey> findAllByStatusIn(final List<SigningKeyStatus> status) {
        return signingKeyMapper.toSigningKeyList(signingKeyJpaRepository.findAllByStatusIn(status));
    }

}
