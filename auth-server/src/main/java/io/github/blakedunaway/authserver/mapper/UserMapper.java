package io.github.blakedunaway.authserver.mapper;

import io.github.blakedunaway.authserver.business.model.Authority;
import io.github.blakedunaway.authserver.business.model.user.ClientRegisterDto;
import io.github.blakedunaway.authserver.business.model.user.ClientUser;
import io.github.blakedunaway.authserver.business.model.user.PlatformRegisterDto;
import io.github.blakedunaway.authserver.business.model.user.PlatformUser;
import io.github.blakedunaway.authserver.business.model.user.PlatformUserTier;
import io.github.blakedunaway.authserver.integration.entity.AuthorityEntity;
import io.github.blakedunaway.authserver.integration.entity.ClientUserEntity;
import io.github.blakedunaway.authserver.integration.entity.PlatformUserEntity;
import io.github.blakedunaway.authserver.integration.entity.PlatformUserTierEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
                                                                                        .map(Authority::from)
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
        return PlatformUser.from(platformUserEntity.getUserId())
                           .expired(platformUserEntity.isExpired())
                           .credentialsExpired(platformUserEntity.isCredentialsExpired())
                           .authorities(authorities -> authorities.addAll(platformUserEntity.getAuthorities()
                                                                                            .stream()
                                                                                            .map(AuthorityEntity::getName)
                                                                                            .filter(name -> name != null && !name.isBlank())
                                                                                            .map(Authority::from)
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
                           .tier(platformUserTierEntityToPlatformUserTier(platformUserEntity.getTier()))
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
        return new ClientUserEntity(clientUser.getId(),
                                    clientUser.getClientId(),
                                    clientUser.getEmail(),
                                    clientUser.getPasswordHash(),
                                    clientUser.isVerified(),
                                    clientUser.getCreatedAt(),
                                    clientUser.getUpdatedAt(),
                                    clientUser.getUserAttributes() == null
                                    ? new LinkedHashMap<>()
                                    : new LinkedHashMap<>(clientUser.getUserAttributes()),
                                    new HashSet<>(),
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

        final PlatformUserEntity platformUserEntity = new PlatformUserEntity(platformUser.getId(),
                                                                             registeredClients,
                                                                             platformUser.getEmail(),
                                                                             platformUser.getPasswordHash(),
                                                                             platformUser.isVerified(),
                                                                             platformUser.getCreatedAt(),
                                                                             platformUser.getUpdatedAt(),
                                                                             platformUser.getUserAttributes() == null
                                                                             ? new LinkedHashMap<>()
                                                                             : new LinkedHashMap<>(platformUser.getUserAttributes()),
                                                                             new HashSet<>(),
                                                                             platformUser.isLocked(),
                                                                             platformUser.isExpired(),
                                                                             platformUser.isCredentialsExpired());
        platformUserEntity.setTier(platformUserTierToPlatformUserTierEntity(platformUser.getTier()));
        return platformUserEntity;
    }

    public PlatformUserTier platformUserTierEntityToPlatformUserTier(final PlatformUserTierEntity platformUserTierEntity) {
        if (platformUserTierEntity == null) {
            return null;
        }

        return PlatformUserTier.builder()
                               .stripePriceId(platformUserTierEntity.getStripPriceId())
                               .id(platformUserTierEntity.getTierId())
                               .name(platformUserTierEntity.getTierName())
                               .price(platformUserTierEntity.getTierPrice())
                               .description(platformUserTierEntity.getTierDescription())
                               .tierOrder(platformUserTierEntity.getTierOrder())
                               .allowedNumberOfRegisteredClients(platformUserTierEntity.getAllowedNumberOfRegisteredClients())
                               .allowedNumberOfGlobalUsers(platformUserTierEntity.getAllowedNumberOfGlobalUsers())
                               .allowedNumberOfGlobalScopes(platformUserTierEntity.getAllowedNumberOfGlobalScopes())
                               .allowedNumberOfGlobalAuthorities(platformUserTierEntity.getAllowedNumberOfGlobalAuthorities())
                               .build();
    }

    public PlatformUserTierEntity platformUserTierToPlatformUserTierEntity(final PlatformUserTier platformUserTier) {
        if (platformUserTier == null) {
            return null;
        }

        return new PlatformUserTierEntity(platformUserTier.getId(),
                                          platformUserTier.getStripePriceId(),
                                          platformUserTier.getName(),
                                          platformUserTier.getPrice(),
                                          platformUserTier.getDescription(),
                                          platformUserTier.getTierOrder(),
                                          platformUserTier.getAllowedNumberOfRegisteredClients(),
                                          platformUserTier.getAllowedNumberOfGlobalUsers(),
                                          platformUserTier.getAllowedNumberOfGlobalScopes(),
                                          platformUserTier.getAllowedNumberOfGlobalAuthorities());
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
                         .userAttributes(new LinkedHashMap<>())
                         .updatedAt(LocalDateTime.now())
                         .createdAt(LocalDateTime.now())
                         .passwordHash(passwordEncoder.encode(clientRegisterDto.getPassword()))
                         .build();
    }

    public PlatformUser platformRegisterDtoToPlatformUser(final PlatformRegisterDto platformRegisterDto) {
        return PlatformUser.from(platformRegisterDto.getEmail())
                           .email(platformRegisterDto.getEmail())
                           .registeredClientIds(ids -> ids.clear())
                           .authorities(authorities -> authorities.add(Authority.from("ROLE_PLATFORM_USER")))
                           .expired(false)
                           .credentialsExpired(false)
                           .verified(false)
                           .userAttributes(new LinkedHashMap<>())
                           .updatedAt(LocalDateTime.now())
                           .createdAt(LocalDateTime.now())
                           .passwordHash(passwordEncoder.encode(platformRegisterDto.getPassword()))
                           .build();
    }

}
