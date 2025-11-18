package io.github.blakedunaway.authserver.integration.repository.implementation;

import io.github.blakedunaway.authserver.business.model.User;
import io.github.blakedunaway.authserver.integration.entity.AuthoritiesEntity;
import io.github.blakedunaway.authserver.integration.entity.UserEntity;
import io.github.blakedunaway.authserver.integration.repository.gateway.UserRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.AuthoritiesJpaRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.UserJpaRepository;
import io.github.blakedunaway.authserver.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpringUserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    private final UserMapper userMapper;

    private final AuthoritiesJpaRepository authoritiesJpaRepository;

    @Override
    public User findByEmail(final String email) {
        final UserEntity userEntity = userJpaRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new UsernameNotFoundException(email));
        if (userEntity == null) {
            return null;
        } else {
            userEntity.setNew(false);
        }
        return userMapper.userEntityToUser(userEntity);
    }

    @Override
    @Transactional
    public User save(final User user) {
        final UserEntity userEntity = userJpaRepository.save(userMapper.userToUserEntity(user));
        final Set<AuthoritiesEntity> authoritiesEntities = authoritiesJpaRepository.findAllByNameIn(userEntity.getAuthorities()
                                                                                                              .stream()
                                                                                                              .map(AuthoritiesEntity::getName)
                                                                                                              .map(String::toUpperCase)
                                                                                                              .collect(Collectors.toSet()));
        authoritiesEntities.addAll(userEntity.getAuthorities());
        userEntity.setAuthorities(authoritiesEntities);
        authoritiesEntities.forEach(auth -> {
            if (auth.getAuthorityId() == null) {
                authoritiesEntities.add(authoritiesJpaRepository.save(auth));
            }
        });
        return userMapper.userEntityToUser(userEntity);
    }

    @Override
    public User findByRegisteredClient_IdAndEmail(final String registeredClientId, final String email) {
        final UserEntity userEntity = userJpaRepository.findByRegisteredClient_IdAndEmail(registeredClientId, email);
        if (userEntity == null) {
            return null;
        }
        return userMapper.userEntityToUser(userEntity);
    }

}

