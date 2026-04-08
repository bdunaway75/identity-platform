package io.github.blakedunaway.authserver.business.service;

import io.github.blakedunaway.authserver.business.api.dto.ClientUserUpdateRequest;
import io.github.blakedunaway.authserver.business.model.Authorities;
import io.github.blakedunaway.authserver.business.model.user.ClientRegisterDto;
import io.github.blakedunaway.authserver.business.model.user.ClientUser;
import io.github.blakedunaway.authserver.business.model.user.PlatformRegisterDto;
import io.github.blakedunaway.authserver.business.model.user.PlatformUser;
import io.github.blakedunaway.authserver.integration.repository.gateway.PlatformUserRepository;
import io.github.blakedunaway.authserver.integration.repository.gateway.UserRepository;
import io.github.blakedunaway.authserver.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.LinkedHashSet;
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
        Assert.notNull(clientUser.getEmail(), "Email cannot be null");
        Assert.notNull(clientUser.getPasswordHash(), "Password hash cannot be null");
        Assert.isTrue(clientUser.getPasswordHash().startsWith("$argon2"), "Password has not been hashed");
        return userRepository.save(clientUser);
    }

    public PlatformUser savePlatformUser(final PlatformUser platformUser) {
        Assert.notNull(platformUser, "ClientUser cannot be null");
        Assert.notNull(platformUser.getEmail(), "Email cannot be null");
        Assert.notNull(platformUser.getPasswordHash(), "Password hash cannot be null");
        Assert.isTrue(platformUser.getPasswordHash().startsWith("$argon2"), "Password has not been hashed");
        return platformUserRepository.save(platformUser);
    }

    public UserDetails loadUserDetailsByEmailAndClientId(final String clientId, final String email) throws UsernameNotFoundException {
        Assert.notNull(email, "Username cannot be null");
        Assert.notNull(clientId, "ClientId cannot be null");
        return userRepository.findByClient_IdAndEmail(clientId, email).map(ClientUser::toSpring).orElse(null);
    }

    public PlatformUser loadPlatformUserByEmail(final String email) throws UsernameNotFoundException {
        Assert.notNull(email, "Email cannot be null");
        return platformUserRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    public Set<UUID> filterOwnedRegisteredClientIds(final PlatformUser platformUser, final Set<UUID> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            return Collections.emptySet();
        }

        final Set<UUID> ownedIds = platformUser.getRegisteredClientIds() == null
                                   ? Collections.emptySet()
                                   : platformUser.getRegisteredClientIds();
        return requestedIds.stream()
                           .filter(ownedIds::contains)
                           .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean ownsRegisteredClient(final PlatformUser platformUser, final UUID registeredClientId) {
        return platformUser.getRegisteredClientIds() != null
               && platformUser.getRegisteredClientIds().contains(registeredClientId);
    }

    public PlatformUser attachRegisteredClientToPlatformUser(final String email, final UUID registeredClientId) {
        Assert.notNull(email, "Username cannot be null");
        Assert.notNull(registeredClientId, "RegisteredClientId cannot be null");

        final PlatformUser existingPlatformUser = loadPlatformUserByEmail(email);
        if (existingPlatformUser == null) {
            throw new UsernameNotFoundException("Platform user not found for email " + email);
        }

        final PlatformUser updatedClientUser = PlatformUser.from(existingPlatformUser)
                                                           .registeredClientIds(ids -> {
                                                               ids.clear();
                                                               ids.addAll(existingPlatformUser.getRegisteredClientIds());
                                                               ids.add(registeredClientId);
                                                           })
                                                           .build();

        return savePlatformUser(updatedClientUser);
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
                                       final ClientUserUpdateRequest request) {
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

                                                           resolvedAuthorities.addAll(request.getAuthorities()
                                                                                             .stream()
                                                                                             .filter(authority -> authority != null && !authority.isBlank())
                                                                                             .map(String::toUpperCase)
                                                                                             .map(Authorities::from)
                                                                                             .collect(Collectors.toSet()));
                                                       })
                                                       .build();

        return saveUser(updatedClientUser);
    }

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        throw new UnsupportedOperationException("loading a user by username is not supported");
    }

}
