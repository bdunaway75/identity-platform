package io.github.blakedunaway.authserver.integration.repository.gateway;

import io.github.blakedunaway.authserver.business.model.SigningKey;
import io.github.blakedunaway.authserver.business.model.enums.SigningKeyStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SigningKeyRepository {

    SigningKey save(final SigningKey signingKey);

    List<SigningKey> findAllByStatusIn(final List<SigningKeyStatus> statuses);

    List<SigningKey> findByStatus(final SigningKeyStatus status);

    List<SigningKey> purgeInactiveKeys();

    Optional<SigningKey> findByKid(final String kid);

    boolean existsByKids(final Set<String> kids);

}
