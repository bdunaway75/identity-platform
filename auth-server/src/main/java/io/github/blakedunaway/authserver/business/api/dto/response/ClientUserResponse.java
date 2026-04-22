package io.github.blakedunaway.authserver.business.api.dto.response;

import io.github.blakedunaway.authserver.business.model.Authority;
import io.github.blakedunaway.authserver.business.model.user.ClientUser;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import io.github.blakedunaway.authserver.util.AuthorityUtility;

@Getter
@Builder
public class ClientUserResponse {

    private final UUID id;

    private final String clientId;

    private final String email;

    private final boolean verified;

    private final LocalDateTime createdAt;

    private final LocalDateTime updatedAt;

    private final Set<String> authorities;

    private final Set<String> roles;

    private final boolean locked;

    private final boolean expired;

    private final boolean credentialsExpired;

    public static ClientUserResponse fromModel(final ClientUser clientUser) {
        if (clientUser == null) {
            return null;
        }

        return ClientUserResponse.builder()
                                 .id(clientUser.getId())
                                 .clientId(clientUser.getClientId())
                                 .email(clientUser.getEmail())
                                 .verified(clientUser.isVerified())
                                 .createdAt(clientUser.getCreatedAt())
                                 .updatedAt(clientUser.getUpdatedAt())
                                 .authorities(AuthorityUtility.extractAuthorities(
                                         clientUser.getAuthorities() == null
                                                 ? Set.of()
                                                 : clientUser.getAuthorities()
                                                             .stream()
                                                             .map(Authority::getName)
                                                             .collect(Collectors.toSet())
                                 ))
                                 .roles(AuthorityUtility.extractRoles(
                                         clientUser.getAuthorities() == null
                                                 ? Set.of()
                                                 : clientUser.getAuthorities()
                                                             .stream()
                                                             .map(Authority::getName)
                                                             .collect(Collectors.toSet())
                                 ))
                                 .locked(clientUser.isLocked())
                                 .expired(clientUser.isExpired())
                                 .credentialsExpired(clientUser.isCredentialsExpired())
                                 .build();
    }
}
