package io.github.blakedunaway.authserver.mapper;

import io.github.blakedunaway.authserver.business.model.Authorities;
import io.github.blakedunaway.authserver.business.model.user.ClientRegisterDto;
import io.github.blakedunaway.authserver.business.model.user.ClientUser;
import io.github.blakedunaway.authserver.business.model.user.PlatformRegisterDto;
import io.github.blakedunaway.authserver.business.model.user.PlatformUser;
import io.github.blakedunaway.authserver.integration.entity.AuthorityEntity;
import io.github.blakedunaway.authserver.integration.entity.ClientUserEntity;
import io.github.blakedunaway.authserver.integration.entity.PlatformUserEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    @Autowired
    private PasswordEncoder passwordEncoder;

    public ClientUser userEntityToUser(final ClientUserEntity clientUserEntity) {
        return ClientUser.from(clientUserEntity.getUserId())
                         .clientId(clientUserEntity.getClientId())
                         .expired(clientUserEntity.isExpired())
                         .credentialsExpired(clientUserEntity.isCredentialsExpired())
                         .authorities(authorities -> authorities.addAll(clientUserEntity.getAuthorities()
                                                                                        .stream()
                                                                                        .map(AuthorityEntity::getName)
                                                                                        .filter(name -> name != null && !name.isBlank())
                                                                                        .map(Authorities::from)
                                                                                        .collect(Collectors.toSet())))
                         .locked(clientUserEntity.isLocked())
                         .verified(clientUserEntity.isVerified())
                         .userAttributes(clientUserEntity.getUserAttributes())
                         .registeredClientIds(ids -> ids.clear())
                         .email(clientUserEntity.getEmail())
                         .passwordHash(clientUserEntity.getPasswordHash())
                         .createdAt(clientUserEntity.getCreatedAt())
                         .updatedAt(clientUserEntity.getUpdatedAt())
                         .build();
    }

    public PlatformUser platformUserEntityToPlatformUser(final PlatformUserEntity platformUserEntity) {
        return PlatformUser.from(platformUserEntity.getEmail())
                           .expired(platformUserEntity.isExpired())
                           .credentialsExpired(platformUserEntity.isCredentialsExpired())
                           .authorities(authorities -> authorities.addAll(platformUserEntity.getAuthorities()
                                                                                            .stream()
                                                                                            .map(AuthorityEntity::getName)
                                                                                            .filter(name -> name != null && !name.isBlank())
                                                                                            .map(Authorities::from)
                                                                                            .collect(Collectors.toSet())))
                           .registeredClientIds(ids -> ids.addAll(platformUserEntity.getRegisteredClients() == null
                                                                  ? Collections.emptySet()
                                                                  : platformUserEntity.getRegisteredClients()
                                                                                      .stream()
                                                                                      .map(RegisteredClientEntity::getRegisteredClientId)
                                                                                      .collect(Collectors.toSet())))
                           .locked(platformUserEntity.isLocked())
                           .verified(platformUserEntity.isVerified())
                           .userAttributes(platformUserEntity.getUserAttributes())
                           .email(platformUserEntity.getEmail())
                           .passwordHash(platformUserEntity.getPasswordHash())
                           .createdAt(platformUserEntity.getCreatedAt())
                           .updatedAt(platformUserEntity.getUpdatedAt())
                           .build();
    }

    public ClientUserEntity userToUserEntity(final ClientUser clientUser) {
        if (clientUser == null) {
            return null;
        }
        return new ClientUserEntity(null,
                                    clientUser.getClientId(),
                                    clientUser.getEmail(),
                                    clientUser.getPasswordHash(),
                                    clientUser.isVerified(),
                                    clientUser.getCreatedAt(),
                                    clientUser.getUpdatedAt(),
                                    clientUser.getUserAttributes() == null ? Map.of() : clientUser.getUserAttributes(),
                                    new LinkedHashSet<>(),
                                    clientUser.isLocked(),
                                    clientUser.isExpired(),
                                    clientUser.isCredentialsExpired());
    }

    public PlatformUserEntity userToPlatformUserEntity(final PlatformUser platformUser) {
        if (platformUser == null) {
            return null;
        }

        final Set<RegisteredClientEntity> registeredClients = platformUser.getRegisteredClientIds() == null
                                                              ? Collections.emptySet()
                                                              : platformUser.getRegisteredClientIds()
                                                                            .stream()
                                                                            .map(this::registeredClientIdToRegisteredClientEntity)
                                                                            .collect(Collectors.toSet());

        return new PlatformUserEntity(null,
                                      registeredClients,
                                      platformUser.getEmail(),
                                      platformUser.getPasswordHash(),
                                      platformUser.isVerified(),
                                      platformUser.getCreatedAt(),
                                      platformUser.getUpdatedAt(),
                                      platformUser.getUserAttributes() == null ? Map.of() : platformUser.getUserAttributes(),
                                      new LinkedHashSet<>(),
                                      platformUser.isLocked(),
                                      platformUser.isExpired(),
                                      platformUser.isCredentialsExpired());
    }

    RegisteredClientEntity registeredClientIdToRegisteredClientEntity(final UUID registeredClientId) {
        if (registeredClientId == null) {
            return null;
        }
        return RegisteredClientEntity.createFromId(registeredClientId);
    }

    public ClientUser clientRegisterDtoToUser(final ClientRegisterDto clientRegisterDto) {
        return ClientUser.from(clientRegisterDto.getEmail())
                         .email(clientRegisterDto.getEmail())
                         .clientId(clientRegisterDto.getClientId())
                         .expired(false)
                         .credentialsExpired(false)
                         .verified(false)
                         .userAttributes(Map.of())
                         .updatedAt(LocalDateTime.now())
                         .createdAt(LocalDateTime.now())
                         .passwordHash(passwordEncoder.encode(clientRegisterDto.getPassword()))
                         .build();
    }

    public PlatformUser platformRegisterDtoToPlatformUser(final PlatformRegisterDto platformRegisterDto) {
        return PlatformUser.from(platformRegisterDto.getEmail())
                           .email(platformRegisterDto.getEmail())
                           .registeredClientIds(ids -> ids.clear())
                           .authorities(authorities -> authorities.add(Authorities.from("ROLE_PLATFORM_USER")))
                           .expired(false)
                           .credentialsExpired(false)
                           .verified(false)
                           .userAttributes(Map.of())
                           .updatedAt(LocalDateTime.now())
                           .createdAt(LocalDateTime.now())
                           .passwordHash(passwordEncoder.encode(platformRegisterDto.getPassword()))
                           .build();
    }

}
