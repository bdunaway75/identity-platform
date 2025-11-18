package io.github.blakedunaway.authserver.mapper;

import io.github.blakedunaway.authserver.business.model.Authorities;
import io.github.blakedunaway.authserver.business.model.RegisterDto;
import io.github.blakedunaway.authserver.business.model.User;
import io.github.blakedunaway.authserver.integration.entity.AuthoritiesEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientEntity;
import io.github.blakedunaway.authserver.integration.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Mapper(uses = {AuthoritiesMapper.class})
public abstract class UserMapper {

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User userEntityToUser(final UserEntity userEntity) {
        return User.fromId(userEntity.getUserId())
                   .registeredClientId(userEntity.getRegisteredClient().getRegisteredClientId())
                   .expired(userEntity.isExpired())
                   .credentialsExpired(userEntity.isCredentialsExpired())
                   .authorities(authorities -> authorities.addAll(userEntity.getAuthorities()
                                                                            .stream()
                                                                            .map(AuthoritiesEntity::getName)
                                                                            .map(Authorities::from)
                                                                            .collect(Collectors.toSet())))
                   .isNew(userEntity.isNew())
                   .locked(userEntity.isLocked())
                   .verified(userEntity.isVerified())
                   .plan(userEntity.getPlan())
                   .email(userEntity.getEmail())
                   .passwordHash(userEntity.getPasswordHash())
                   .createdAt(userEntity.getCreatedAt())
                   .updatedAt(userEntity.getUpdatedAt())
                   .build();
    }

    @Mapping(target = "registeredClient", source = "registeredClientId")
    public abstract UserEntity userToUserEntity(final User user);

    RegisteredClientEntity registeredClientIdToRegisteredClientEntity(final String registeredClientId) {
        if (registeredClientId == null) {
            return null;
        }
        return RegisteredClientEntity.createFromId(registeredClientId);
    }

    public User registerDtoToUser(final RegisterDto registerDto) {
        return User.fromEmail(registerDto.getEmail())
                   .isNew(true)
                   .email(registerDto.getEmail())
                   .registeredClientId(registerDto.getRegisteredClientId())
                   .expired(false)
                   .credentialsExpired(false)
                   .verified(false)
                   .plan("Basic")
                   .updatedAt(LocalDateTime.now())
                   .createdAt(LocalDateTime.now())
                   .passwordHash(passwordEncoder.encode(registerDto.getPassword()))
                   .build();
    }

}
