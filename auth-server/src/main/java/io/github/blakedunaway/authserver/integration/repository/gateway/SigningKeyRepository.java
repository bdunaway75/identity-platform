package io.github.blakedunaway.authserver.integration.repository.gateway;

import io.github.blakedunaway.authserver.business.model.SigningKey;
import io.github.blakedunaway.authserver.business.model.enums.SigningKeyStatus;

import java.util.List;

public interface SigningKeyRepository {

    SigningKey save(final SigningKey signingKey);

    List<SigningKey> findAllByStatusIn(final List<SigningKeyStatus> statuses);

    List<SigningKey> findByStatus(final SigningKeyStatus status);

}
