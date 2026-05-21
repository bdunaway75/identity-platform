package io.github.blakedunaway.authserver.business.service;

import io.github.blakedunaway.authserver.business.api.dto.request.ClientUserRequest;
import io.github.blakedunaway.authserver.business.model.Authority;
import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import io.github.blakedunaway.authserver.business.model.user.ClientUser;
import io.github.blakedunaway.authserver.business.model.user.PlatformUser;
import io.github.blakedunaway.authserver.business.model.user.PlatformUserTier;
import io.github.blakedunaway.authserver.integration.repository.gateway.PlatformUserRepository;
import io.github.blakedunaway.authserver.integration.repository.gateway.UserRepository;
import io.github.blakedunaway.authserver.util.AuthorityUtility;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    private final PlatformUserRepository platformUserRepository;

    private final RegisteredClientService registeredClientService;

    private final PasswordEncoder passwordEncoder;

    public ClientUser saveVerifiedClientUser(final String email,
                                             final String passwordHash,
                                             final String clientId) {
        Assert.hasText(email, "Email is required");
        Assert.hasText(passwordHash, "Password hash is required");
        Assert.hasText(clientId, "Client id is required");

        return saveUser(ClientUser.verified(email, passwordHash, clientId)
                                  .updatedAt(LocalDateTime.now())
                                  .createdAt(LocalDateTime.now())
                                  .build());
    }

    public PlatformUser saveVerifiedPlatformUser(final String email,
                                                 final String passwordHash) {
        Assert.hasText(email, "Email is required");
        Assert.hasText(passwordHash, "Password hash is required");

        return savePlatformUser(PlatformUser.verified(email, passwordHash)
                                            .updatedAt(LocalDateTime.now())
                                            .createdAt(LocalDateTime.now())
                                            .build());
    }

    public ClientUser saveUser(final ClientUser clientUser) {
        Assert.notNull(clientUser, "ClientUser cannot be null");
        Assert.notNull(clientUser.getPasswordHash(), "Password hash cannot be null");
        Assert.isTrue(clientUser.getPasswordHash().startsWith("$argon2"), "Password has not been hashed");
        return userRepository.save(clientUser);
    }

    public PlatformUser savePlatformUser(final PlatformUser platformUser) {
        Assert.notNull(platformUser, "PlatformUser cannot be null");
        Assert.notNull(platformUser.getPasswordHash(), "Password hash cannot be null");
        Assert.isTrue(platformUser.getPasswordHash().startsWith("$argon2"), "Password has not been hashed");
        final PlatformUser resolvedPlatformUser = platformUser.withDefaultTierIfMissing();
        resolvedPlatformUser.validateTierCompliance(registeredClientService.findRegisteredClientsByIds(platformUser.getRegisteredClientIds()));
        return platformUserRepository.save(resolvedPlatformUser);
    }

    public UserDetails loadUserDetailsByEmailAndClientId(final String clientId, final String email) throws UsernameNotFoundException {
        Assert.notNull(email, "Email cannot be null");
        Assert.notNull(clientId, "ClientId cannot be null");
        return userRepository.findByClient_IdAndEmail(clientId, email).map(ClientUser::toSpring).orElse(null);
    }

    public boolean existsClientIdAndEmail(final String clientId, final String email) {
        Assert.notNull(email, "Email cannot be null");
        Assert.notNull(clientId, "ClientId cannot be null");
        return userRepository.existsByClientIdAndEmail(clientId, email);
    }

    public PlatformUser loadPlatformUserByEmail(final String email) throws UsernameNotFoundException {
        Assert.notNull(email, "Email cannot be null");
        return platformUserRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    public boolean updateExpiredPlatformUserPassword(final String email,
                                                     final String currentPassword,
                                                     final String newPassword) {
        if (StringUtils.isBlank(email) || StringUtils.isBlank(currentPassword) || StringUtils.isBlank(newPassword)) {
            return false;
        }

        final PlatformUser existingPlatformUser = loadPlatformUserByEmail(email);
        if (existingPlatformUser == null || !passwordEncoder.matches(currentPassword, existingPlatformUser.getPasswordHash())) {
            return false;
        }

        final PlatformUser updatedPlatformUser = existingPlatformUser.resetPassword(passwordEncoder.encode(newPassword));
        savePlatformUser(updatedPlatformUser);
        return true;
    }

    public boolean updateExpiredClientUserPassword(final String clientId,
                                                   final String email,
                                                   final String currentPassword,
                                                   final String newPassword) {
        if (StringUtils.isBlank(email) || StringUtils.isBlank(currentPassword) || StringUtils.isBlank(newPassword)) {
            return false;
        }

        final ClientUser existingClientUser = userRepository.findByClient_IdAndEmail(clientId, email).orElse(null);
        if (existingClientUser == null || !passwordEncoder.matches(currentPassword, existingClientUser.getPasswordHash())) {
            return false;
        }

        final ClientUser updatedClientUser = existingClientUser.resetPassword(passwordEncoder.encode(newPassword));
        saveUser(updatedClientUser);
        return true;
    }

    public UserDetails loadPlatformUserDetailsByEmail(final String email) throws UsernameNotFoundException {
        Assert.notNull(email, "Email cannot be null");
        return platformUserRepository.findByEmailIgnoreCase(email).map(PlatformUser::toSpring).orElse(null);
    }

    public Set<UUID> filterOwnedRegisteredClientIds(final PlatformUser platformUser, final Set<UUID> requestedIds) {
        Assert.notNull(platformUser, "PlatformUser cannot be null");
        return platformUser.filterOwnedRegisteredClientIds(requestedIds);
    }

    public PlatformUser attachRegisteredClientToPlatformUser(final String email, final UUID registeredClientId) {
        Assert.notNull(email, "Username cannot be null");
        Assert.notNull(registeredClientId, "RegisteredClientId cannot be null");

        final PlatformUser existingPlatformUser = loadPlatformUserByEmail(email);
        if (existingPlatformUser == null) {
            throw new UsernameNotFoundException("Platform user not found for email " + email);
        }

        return savePlatformUser(existingPlatformUser.attachRegisteredClientId(registeredClientId));
    }

    public int getTotalUserCount(final String email) {
        Assert.notNull(email, "Email cannot be null");
        return platformUserRepository.getTotalUserCount(email);
    }

    public int getTotalClientCount(final String email) {
        Assert.notNull(email, "Email cannot be null");
        return platformUserRepository.getTotalClientCount(email);
    }

    public List<ClientUser> findClientUsersByRegisteredClientIds(final Set<UUID> registeredClientIds) {
        if (registeredClientIds == null || registeredClientIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllByRegisteredClientIds(registeredClientIds);
    }

    public ClientUser updateClientUser(final UUID clientUserId,
                                       final Set<UUID> registeredClientIds,
                                       final ClientUserRequest request) {
        final ClientUser existingClientUser = userRepository.findByIdAndRegisteredClientIds(clientUserId, registeredClientIds)
                                                            .orElse(null);
        if (existingClientUser == null) {
            return null;
        }

        return saveUser(existingClientUser.applyRequest(request));
    }

    public void removeRemovedRegisteredClientAuthorities(final RegisteredClientModel existingRegisteredClient,
                                                         final RegisteredClientModel updatedRegisteredClient) {
        if (existingRegisteredClient == null || updatedRegisteredClient == null || existingRegisteredClient.getId() == null) {
            return;
        }

        final Set<String> removedAuthorityNames = existingRegisteredClient.removedAuthorityAndRoleNamesComparedTo(updatedRegisteredClient);
        if (removedAuthorityNames.isEmpty()) {
            return;
        }

        for (final ClientUser clientUser : findClientUsersByRegisteredClientIds(Set.of(existingRegisteredClient.getId()))) {
            final ClientUser updatedClientUser = clientUser.removeAuthoritiesNamed(removedAuthorityNames);
            if (updatedClientUser == clientUser) {
                continue;
            }

            saveUser(updatedClientUser);
        }
    }

    public PlatformUser loadPlatformUserById(final UUID userId) {
        Assert.notNull(userId, "UserId cannot be null");
        return platformUserRepository.loadPlatformUserById(userId);
    }

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        throw new UnsupportedOperationException("loading a user by username is not supported");
    }

}
