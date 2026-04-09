package io.github.blakedunaway.authserver.business.service;

import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import io.github.blakedunaway.authserver.business.validation.RegisteredClientValidator;
import io.github.blakedunaway.authserver.integration.repository.gateway.RegisteredClientInternalRepository;
import io.github.blakedunaway.authserver.util.AuthenticationUtility;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public Set<RegisteredClientModel> findRegisteredClientsByIds(final Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
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

    public RegisteredClientModel previewUpdatedRegisteredClient(final RegisteredClientModel existingRegisteredClient,
                                                                final RegisteredClientModel registeredClientModel) {
        if (existingRegisteredClient == null || registeredClientModel == null) {
            return null;
        }
        final ClientSettings clientSettings = registeredClientModel.getClientSettings().isEmpty()
                                              ? ClientSettings.withSettings(existingRegisteredClient.getClientSettings()).build()
                                              : ClientSettings.withSettings(registeredClientModel.getClientSettings()).build();
        final TokenSettings tokenSettings = registeredClientModel.getTokenSettings().isEmpty()
                                            ? TokenSettings.withSettings(existingRegisteredClient.getTokenSettings()).build()
                                            : TokenSettings.withSettings(registeredClientModel.getTokenSettings()).build();

        return RegisteredClientModel.builder()
                                    .clientId(existingRegisteredClient.getClientId())
                                    .clientIdIssuedAt(existingRegisteredClient.getClientIdIssuedAt())
                                    .clientSecret(existingRegisteredClient.getClientSecret())
                                    .clientSecretExpiresAt(existingRegisteredClient.getClientSecretExpiresAt())
                                    .clientName(registeredClientModel.getClientName() == null || registeredClientModel.getClientName().isBlank()
                                                ? existingRegisteredClient.getClientName()
                                                : registeredClientModel.getClientName())
                                    .clientAuthenticationMethods(existingRegisteredClient.getClientAuthenticationMethods()
                                                                                         .stream()
                                                                                         .map(ClientAuthenticationMethod::new)
                                                                                         .collect(Collectors.toSet()))
                                    .authorizationGrantTypes(existingRegisteredClient.getAuthorizationGrantTypes()
                                                                                     .stream()
                                                                                     .map(AuthorizationGrantType::new)
                                                                                     .collect(Collectors.toSet()))
                                    .redirectUris(registeredClientModel.getRedirectUris())
                                    .postLogoutRedirectUris(registeredClientModel.getPostLogoutRedirectUris())
                                    .scopes(registeredClientModel.getScopes())
                                    .authorities(registeredClientModel.getAuthorities())
                                    .roles(registeredClientModel.getRoles())
                                    .clientSettings(clientSettings)
                                    .tokenSettings(tokenSettings)
                                    .build();
    }

}
