package io.github.blakedunaway.authserver.business.api.dto;

import io.github.blakedunaway.authserver.business.model.user.ClientUser;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
public class ClientUserView {

    private final UUID id;

    private final String clientId;

    private final String email;

    private final boolean verified;

    private final LocalDateTime createdAt;

    private final LocalDateTime updatedAt;

    private final Set<String> authorities;

    private final boolean locked;

    private final boolean expired;

    private final boolean credentialsExpired;

    public static ClientUserView fromModel(final ClientUser clientUser) {
        if (clientUser == null) {
            return null;
        }

        return ClientUserView.builder()
                             .id(clientUser.getId())
                             .clientId(clientUser.getClientId())
                             .email(clientUser.getEmail())
                             .verified(clientUser.isVerified())
                             .createdAt(clientUser.getCreatedAt())
                             .updatedAt(clientUser.getUpdatedAt())
                             .authorities(clientUser.getAuthorities() == null
                                          ? Set.of()
                                          : clientUser.getAuthorities()
                                                      .stream()
                                                      .map(authority -> authority.getName())
                                                      .collect(Collectors.toSet()))
                             .locked(clientUser.isLocked())
                             .expired(clientUser.isExpired())
                             .credentialsExpired(clientUser.isCredentialsExpired())
                             .build();
    }
}
