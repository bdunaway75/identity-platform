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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
            final SigningKey updated = SigningKey.from(signingKeyEntity.get()
                                                                       .getId())
                                                 .encoded(signingKey.isEncoded())
                                                 .keys(signingKey.getPrivateKey(), signingKey.getPublicKey())
                                                 .signingKeyStatus(signingKey.getStatus())
                                                 .authTokens(signingKey.getTokens())
                                                 .algorithm(signingKey.getAlgorithm())
                                                 .createdAt(signingKey.getCreatedAt())
                                                 .kid(signingKey.getKid())
                                                 .build();
            signingKeyJpaRepository.save(signingKeyMapper.toEntity(updated));
            return updated;
        }
        return signingKeyMapper.toSigningKey(signingKeyJpaRepository.save(signingKeyMapper.toEntity(signingKey)));
    }

    @Override
    public List<SigningKey> findByStatus(final SigningKeyStatus status) {
        return signingKeyMapper.toSigningKeyList(signingKeyJpaRepository.findByStatus(status));
    }

    @Transactional
    @Override
    public List<SigningKey> purgeInactiveKeys() {
        final Instant now = Instant.now();
        final List<SigningKeyEntity> inactive = signingKeyJpaRepository.findByStatus(SigningKeyStatus.INACTIVE);

        final List<SigningKeyEntity> toPurge = inactive.stream()
                                                       .filter(sk -> sk.getTokens() == null ||
                                                               sk.getTokens()
                                                                 .stream()
                                                                 .noneMatch(t -> t.getRevokedAt() == null &&
                                                                         t.getExpiresAt()
                                                                          .isAfter(now)))
                                                       .toList();

        //deleting for now, maybe keep for auditing
        if (!toPurge.isEmpty()) {
            signingKeyJpaRepository.deleteAllInBatch(toPurge);
        }
        return signingKeyMapper.toSigningKeyList(toPurge);
    }

    @Override
    public Optional<SigningKey> findByKid(final String kid) {
        return signingKeyJpaRepository.findByKid(kid)
                                      .map(signingKeyMapper::toSigningKey);
    }

    @Override
    public boolean existsByKids(Set<String> kids) {
        return !signingKeyJpaRepository.findSigningKeyEntitiesByKidIn(kids).isEmpty();
    }

    @Override
    public List<SigningKey> findAllByStatusIn(final List<SigningKeyStatus> status) {
        return signingKeyMapper.toSigningKeyList(signingKeyJpaRepository.findAllByStatusIn(status));
    }

}
