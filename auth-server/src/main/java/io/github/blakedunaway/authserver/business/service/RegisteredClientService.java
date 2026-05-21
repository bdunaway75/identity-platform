package io.github.blakedunaway.authserver.business.service;

import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import io.github.blakedunaway.authserver.business.validation.RegisteredClientValidator;
import io.github.blakedunaway.authserver.integration.repository.gateway.RegisteredClientInternalRepository;
import io.github.blakedunaway.authserver.util.AuthenticationUtility;
import io.github.blakedunaway.authserver.util.AuthorityUtility;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RegisteredClientService {

    private final RegisteredClientInternalRepository registeredClientInternalRepository;

    public RegisteredClientModel saveRegisteredClient(final RegisteredClientModel registeredClientModel) {
        validateRegisteredClient(registeredClientModel);
        if (AuthenticationUtility.declaredConfidential(registeredClientModel.getClientAuthenticationMethods())) {
            final String rawClientSecret = UUID.randomUUID().toString();
            final RegisteredClientModel savedClient =
                    registeredClientInternalRepository.save(registeredClientModel.withClientSecret(rawClientSecret)
                                                                                 .withClientSecretExpiresAt(LocalDateTime.now().plusDays(30)));
            return findRegisteredClientById(savedClient.getId()).withClientSecret(rawClientSecret);
        }

        final RegisteredClientModel savedClient = registeredClientInternalRepository.save(registeredClientModel);
        return findRegisteredClientById(savedClient.getId());
    }

    public RegisteredClientModel findRegisteredClientById(final UUID id) {
        if (id == null) {
            return null;
        }
        return registeredClientInternalRepository.findById(id.toString());
    }

    public boolean existsByClientId(final String clientId) {
        return registeredClientInternalRepository.existsByClientId(clientId);
    }

    public Set<RegisteredClientModel> findRegisteredClientsByIds(final Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        return registeredClientInternalRepository.findAllByIds(ids);
    }

    public boolean validateRegisteredClient(@Valid final RegisteredClientModel registeredClientModel) {
        final Map<String, List<String>> validatedClient = RegisteredClientValidator.isValid(registeredClientModel);
        if (!validatedClient.isEmpty()) {
            throw new ValidationException("Client validation failed with errors: " + Map.of("errors", Map.of("validatedClient", validatedClient)));
        }
        return true;
    }

    public RegisteredClientModel updateRegisteredClient(final RegisteredClientModel existingRegisteredClient,
                                                        final RegisteredClientModel registeredClientModel) {
        final RegisteredClientModel resolvedRegisteredClient = previewUpdatedRegisteredClient(existingRegisteredClient,
                                                                                              registeredClientModel);
        if (resolvedRegisteredClient == null) {
            return null;
        }

        validateRegisteredClient(resolvedRegisteredClient);
        registeredClientInternalRepository.update(resolvedRegisteredClient);
        return findRegisteredClientById(existingRegisteredClient.getId());
    }

    public RegisteredClientModel updateRegisteredClientAuthorities(final UUID id,
                                                                   final Set<String> authoritiesAndRoles) {
        final RegisteredClientModel existingRegisteredClient = findRegisteredClientById(id);
        if (existingRegisteredClient == null) {
            return null;
        }

        return updateRegisteredClient(
                existingRegisteredClient,
                existingRegisteredClient.withAuthoritiesAndRoles(authoritiesAndRoles)
        );
    }

    public RegisteredClientModel previewUpdatedRegisteredClient(final RegisteredClientModel existingRegisteredClient,
                                                                final RegisteredClientModel registeredClientModel) {
        if (existingRegisteredClient == null || registeredClientModel == null) {
            return null;
        }
        return existingRegisteredClient.merge(registeredClientModel);
    }

}
