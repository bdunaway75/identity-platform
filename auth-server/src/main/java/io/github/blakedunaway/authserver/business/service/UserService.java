package io.github.blakedunaway.authserver.business.service;

import io.github.blakedunaway.authserver.business.api.dto.request.ClientUserRequest;
import io.github.blakedunaway.authserver.business.model.Authority;
import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import io.github.blakedunaway.authserver.business.model.user.ClientRegisterDto;
import io.github.blakedunaway.authserver.business.model.user.ClientUser;
import io.github.blakedunaway.authserver.business.model.user.PlatformRegisterDto;
import io.github.blakedunaway.authserver.business.model.user.PlatformUser;
import io.github.blakedunaway.authserver.business.model.user.PlatformUserTier;
import io.github.blakedunaway.authserver.integration.repository.gateway.PlatformUserRepository;
import io.github.blakedunaway.authserver.integration.repository.gateway.UserRepository;
import io.github.blakedunaway.authserver.mapper.UserMapper;
import io.github.blakedunaway.authserver.util.AuthorityUtility;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    private final PlatformUserRepository platformUserRepository;

    private final UserMapper userMapper;

    private final RegisteredClientService registeredClientService;

    private final PasswordEncoder passwordEncoder;

    public ClientUser signUpClientUser(final ClientRegisterDto clientRegisterDto) {
        if (clientRegisterDto.getEmail().isBlank() || clientRegisterDto.getPassword().isBlank()) {
            throw new IllegalArgumentException("Username and password are required");
        }
        return saveUser(userMapper.clientRegisterDtoToUser(clientRegisterDto));
    }

    public PlatformUser signUpPlatformUser(final PlatformRegisterDto platformRegisterDto) {
        if (platformRegisterDto.getEmail().isBlank() || platformRegisterDto.getPassword().isBlank()) {
            throw new IllegalArgumentException("Username and password are required");
        }
        return savePlatformUser(userMapper.platformRegisterDtoToPlatformUser(platformRegisterDto));
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
        final PlatformUser resolvedPlatformUser = platformUser.getTier() == null
                                                  ? PlatformUser.from(platformUser)
                                                                .tier(PlatformUserTier.builder()
                                                                                      .name("FREE")
                                                                                      .build())
                                                                .build()
                                                  : platformUser;
        validatePlatformUserTierCompliance(resolvedPlatformUser);
        return platformUserRepository.save(resolvedPlatformUser);
    }

    public UserDetails loadUserDetailsByEmailAndClientId(final String clientId, final String email) throws UsernameNotFoundException {
        Assert.notNull(email, "Email cannot be null");
        Assert.notNull(clientId, "ClientId cannot be null");
        return userRepository.findByClient_IdAndEmail(clientId, email).map(ClientUser::toSpring).orElse(null);
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

        final PlatformUser updatedPlatformUser = PlatformUser.from(existingPlatformUser)
                                                             .passwordHash(passwordEncoder.encode(newPassword))
                                                             .expired(false)
                                                             .credentialsExpired(false)
                                                             .build();
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

        final ClientUser updatedClientUser = ClientUser.from(existingClientUser)
                                                       .passwordHash(passwordEncoder.encode(newPassword))
                                                       .expired(false)
                                                       .credentialsExpired(false)
                                                       .build();
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

    public void validatePlatformUserTierCompliance(final PlatformUser platformUser) {
        Assert.notNull(platformUser, "PlatformUser cannot be null");
        validatePlatformUserTierCompliance(platformUser, registeredClientService.findRegisteredClientsByIds(platformUser.getRegisteredClientIds()));
    }

    public void validatePlatformUserTierCompliance(final PlatformUser platformUser,
                                                   final Set<RegisteredClientModel> registeredClients) {
        Assert.notNull(platformUser, "PlatformUser cannot be null");
        platformUser.validateTierCompliance(registeredClients);
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

        final ClientUser updatedClientUser = ClientUser.from(existingClientUser)
                                                       .email(request.getEmail() != null ? request.getEmail() : existingClientUser.getEmail())
                                                       .verified(request.getVerified() != null
                                                                 ? request.getVerified()
                                                                 : existingClientUser.isVerified())
                                                       .locked(request.getLocked() != null ? request.getLocked() : existingClientUser.isLocked())
                                                       .expired(request.getExpired() != null ? request.getExpired() : existingClientUser.isExpired())
                                                       .credentialsExpired(request.getCredentialsExpired() != null
                                                                           ? request.getCredentialsExpired()
                                                                           : existingClientUser.isCredentialsExpired())
                                                       .userAttributes(request.getUserAttributes() != null
                                                                       ? request.getUserAttributes()
                                                                       : existingClientUser.getUserAttributes())
                                                       .authorities(resolvedAuthorities -> {
                                                           resolvedAuthorities.clear();
                                                           if (request.getAuthorities() == null) {
                                                               resolvedAuthorities.addAll(existingClientUser.getAuthorities());
                                                               return;
                                                           }

                                                           resolvedAuthorities.addAll(
                                                                   AuthorityUtility.normalizeAuthorityAndRoleNames(request.getAuthorities())
                                                                                   .stream()
                                                                                   .map(Authority::from)
                                                                                   .collect(Collectors.toSet())
                                                           );
                                                       })
                                                       .build();

        return saveUser(updatedClientUser);
    }

    public void removeRemovedRegisteredClientAuthorities(final RegisteredClientModel existingRegisteredClient,
                                                         final RegisteredClientModel updatedRegisteredClient) {
        if (existingRegisteredClient == null || updatedRegisteredClient == null || existingRegisteredClient.getId() == null) {
            return;
        }

        final Set<String> removedAuthorityNames = new HashSet<>(AuthorityUtility.normalizeAuthorities(existingRegisteredClient.getAuthorities()));
        removedAuthorityNames.addAll(AuthorityUtility.normalizeRoles(existingRegisteredClient.getRoles()));
        removedAuthorityNames.removeAll(AuthorityUtility.normalizeAuthorities(updatedRegisteredClient.getAuthorities()));
        removedAuthorityNames.removeAll(AuthorityUtility.normalizeRoles(updatedRegisteredClient.getRoles()));
        if (removedAuthorityNames.isEmpty()) {
            return;
        }

        for (final ClientUser clientUser : findClientUsersByRegisteredClientIds(Set.of(existingRegisteredClient.getId()))) {
            final Set<Authority> retainedAuthorities = clientUser.getAuthorities() == null
                                                       ? Collections.emptySet()
                                                       : clientUser.getAuthorities()
                                                                   .stream()
                                                                   .filter(authority -> authority != null
                                                                                        && authority.getName() != null
                                                                                        && !removedAuthorityNames.contains(authority.getName()
                                                                                                                                    .toUpperCase()))
                                                                   .collect(Collectors.toSet());
            if (clientUser.getAuthorities() != null && retainedAuthorities.size() == clientUser.getAuthorities().size()) {
                continue;
            }

            final ClientUser updatedClientUser = ClientUser.from(clientUser)
                                                           .authorities(authorities -> {
                                                               authorities.clear();
                                                               authorities.addAll(retainedAuthorities);
                                                           })
                                                           .build();
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
