package io.github.blakedunaway.authserver.business.service;

import io.github.blakedunaway.authserver.business.model.CreatedRegisteredClient;
import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import io.github.blakedunaway.authserver.integration.repository.gateway.RegisteredClientInternalRepository;
import io.github.blakedunaway.authserver.util.AuthenticationUtility;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RegisteredClientService {

    private final RegisteredClientInternalRepository registeredClientInternalRepository;

    private final Validator validator;

    public CreatedRegisteredClient saveRegisteredClient(final RegisteredClientModel registeredClientModel) {
        validateRegisteredClient(registeredClientModel);
        if (AuthenticationUtility.declaredConfidential(registeredClientModel.getClientAuthenticationMethods())) {
            final String rawClientSecret = UUID.randomUUID().toString();
            final RegisteredClientModel savedClient =
                    registeredClientInternalRepository.save(registeredClientModel.withClientSecret(rawClientSecret)
                                                                                .withClientSecretExpiresAt(LocalDateTime.now().plusDays(30)));
            return CreatedRegisteredClient.create(savedClient, rawClientSecret);
        }

        return CreatedRegisteredClient.create(registeredClientInternalRepository.save(registeredClientModel), null);

    }

    public RegisteredClientModel updateRegisteredClient(final RegisteredClientModel registeredClientModel) {
        validateRegisteredClient(registeredClientModel);
        return registeredClientInternalRepository.update(registeredClientModel);
    }

    public RegisteredClientModel findRegisteredClientById(final UUID id) {
        if (id == null) {
            return null;
        }
        return registeredClientInternalRepository.findById(id.toString());
    }

    public Set<RegisteredClientModel> findRegisteredClientsByIds(final Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(registeredClientInternalRepository.findAllByIds(ids));
    }

    public boolean validateRegisteredClient(@Valid final RegisteredClientModel registeredClientModel) {
        final Set<ConstraintViolation<RegisteredClientModel>> validatedClient = validator.validate(registeredClientModel);
        if (!validatedClient.isEmpty()) {
            throw new ValidationException("Client validation failed with errors: " + Map.of("errors", Map.of("validatedClient", validatedClient)));
        }
        return true;
    }

}
