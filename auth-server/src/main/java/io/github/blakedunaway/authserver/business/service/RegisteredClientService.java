package io.github.blakedunaway.authserver.business.service;

import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import io.github.blakedunaway.authserver.integration.repository.gateway.RegisteredClientInternalRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class RegisteredClientService {

    private final RegisteredClientInternalRepository registeredClientInternalRepository;

    private final Validator validator;

    public RegisteredClientModel saveRegisteredClient(final RegisteredClientModel registeredClientModel) {
        validateRegisteredClient(registeredClientModel);
        return registeredClientInternalRepository.save(registeredClientModel);

    }

    public RegisteredClientModel updateRegisteredClient(final RegisteredClientModel registeredClientModel) {
        validateRegisteredClient(registeredClientModel);
        return registeredClientInternalRepository.update(registeredClientModel);

    }

    public boolean validateRegisteredClient(@Valid final RegisteredClientModel registeredClientModel) {
        final Set<ConstraintViolation<RegisteredClientModel>> validatedClient = validator.validate(registeredClientModel);
        if (!validatedClient.isEmpty()) {
            throw new ValidationException("Client validation failed with errors: " + Map.of("errors", Map.of("validatedClient", validatedClient)));
        }
        return true;
    }

}
