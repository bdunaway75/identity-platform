package io.github.blakedunaway.authserver.business.api.dto.response;

import io.github.blakedunaway.authserver.business.model.user.PlatformUserTier;
import lombok.Builder;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Builder
@Getter
public class PlatformUserDetailsReponse {
    private final Set<RegisteredClientResponse> registeredClientResponses;

    private final Set<String> clientIds;

    private final int totalUsers;

    private final int totalRegisteredClients;

    private final int totalScopes;

    private final int totalAuthorities;

    private final int totalRoles;

    private final PlatformUserTier tier;

    private final boolean isDemoUser;

    private final boolean isAdmin;

    public static PlatformUserDetailsReponseBuilder from(final Set<RegisteredClientResponse> registeredClients,
                                                         final PlatformUserTier tier) {
        final Set<RegisteredClientResponse> registeredClientResponses = new HashSet<>();
        final Set<String> clientIds = new HashSet<>();
        int totalScopes = 0;
        int totalAuthorities = 0;
        int totalRoles = 0;
        int totalRegisteredClients = 0;
        if (registeredClients != null) {
            totalRegisteredClients = registeredClients.size();
            for (final RegisteredClientResponse registeredClient : registeredClients) {
                registeredClientResponses.add(registeredClient);
                clientIds.add(registeredClient.getClientId());
                if (registeredClient.getScopes() != null) {
                    totalScopes += registeredClient.getScopes().size();
                }
                if (registeredClient.getAuthorities() != null) {
                    totalAuthorities += registeredClient.getAuthorities().size();
                }
                if (registeredClient.getRoles() != null) {
                    totalRoles += registeredClient.getRoles().size();
                }
            }
        }
        return PlatformUserDetailsReponse.builder()
                                         .clientIds(clientIds)
                                         .registeredClientResponses(registeredClientResponses)
                                         .totalScopes(totalScopes)
                                         .totalAuthorities(totalAuthorities)
                                         .totalRoles(totalRoles)
                                         .totalRegisteredClients(totalRegisteredClients)
                                         .tier(tier);
    }
}
