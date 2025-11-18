package io.github.blakedunaway.authserver.integration.repository.implementation;

import io.github.blakedunaway.authserver.business.model.Authorization;
import io.github.blakedunaway.authserver.integration.entity.AuthorizationEntity;
import io.github.blakedunaway.authserver.integration.repository.gateway.AuthorizationRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.AuthorizationJpaRepository;
import io.github.blakedunaway.authserver.mapper.AuthorizationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationRepositoryImpl implements AuthorizationRepository {

    private final ObjectMapper objectMapper;

    private final AuthorizationJpaRepository authorizationJpaRepository;

    private final AuthorizationMapper authorizationMapper;

    @Override
    @Transactional
    public Authorization save(final Authorization authorization) {
        final AuthorizationEntity authorizationEntity = authorizationMapper.authorizationToAuthorizationEntity(authorization);
        return authorizationMapper.authorizationEntityToAuthorization(authorizationJpaRepository.save(authorizationEntity));
    }

    @Override
    @Transactional
    public void remove(final String id) {
        if (authorizationJpaRepository.findById(UUID.fromString(id)).isEmpty()) {
            throw new EntityNotFoundException("Not entity found with id " + id);
        }
        authorizationJpaRepository.deleteById(UUID.fromString(id));
    }

    @Override
    public Authorization findById(final String id) {
        if (id == null) {
            return null;
        }
        final AuthorizationEntity authorizationEntity = authorizationJpaRepository.findById(UUID.fromString(id)).orElse(null);
        if (authorizationEntity == null) {
            return null;
        }
        authorizationEntity.setNew(false);
        return authorizationMapper.authorizationEntityToAuthorization(authorizationEntity);
    }

    @Override
    public Authorization findByToken(final String token, final String tokenType) {
        final AuthorizationEntity authorizationEntity = authorizationJpaRepository.findByTokens_TokenValueHash(token).orElse(null);
        if (authorizationEntity == null) {
            return null;
        }
        authorizationEntity.setNew(false);
        return authorizationMapper.authorizationEntityToAuthorization(authorizationEntity);
    }

    @Override
    public Authorization findByTokenAttribute(final String attributeKey, final String attributeValue) {
        Assert.notNull(attributeKey, "attributeKey must not be null");
        Assert.notNull(attributeValue, "attributeValue must not be null");
        try {
            final AuthorizationEntity authorizationEntity =
                    authorizationJpaRepository.findByAttribute(objectMapper.writeValueAsString(Map.of(attributeKey, attributeValue)));
            if (authorizationEntity == null) {
                return null;
            } else {
                authorizationEntity.setNew(false);
                return authorizationMapper.authorizationEntityToAuthorization(authorizationEntity);
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<Authorization> findAll() {
        final List<AuthorizationEntity> authorizationEntities = authorizationJpaRepository.findAll();
        if (authorizationEntities.isEmpty()) {
            return new ArrayList<>();
        }
        return authorizationEntities.stream().map(authorizationMapper::authorizationEntityToAuthorization).collect(Collectors.toList());
    }

}
