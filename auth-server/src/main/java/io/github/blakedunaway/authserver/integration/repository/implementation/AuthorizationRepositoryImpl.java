package io.github.blakedunaway.authserver.integration.repository.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.blakedunaway.authserver.business.model.Authorization;
import io.github.blakedunaway.authserver.business.model.enums.TokenType;
import io.github.blakedunaway.authserver.integration.entity.AuthorizationEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientEntity;
import io.github.blakedunaway.authserver.integration.repository.gateway.AuthorizationRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.AuthorizationJpaRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.RegisterClientJpaRepository;
import io.github.blakedunaway.authserver.mapper.AuthorizationMapper;
import io.github.blakedunaway.authserver.security.token.TokenHasher;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationRepositoryImpl implements AuthorizationRepository {

    private final AuthorizationJpaRepository authorizationJpaRepository;

    private final AuthorizationMapper authorizationMapper;

    private final RegisterClientJpaRepository registerClientJpaRepository;

    @Override
    @Transactional
    public Authorization save(final OAuth2Authorization authorization) {

        final UUID authId = UUID.fromString(authorization.getId());
        final UUID rClientId = UUID.fromString(authorization.getRegisteredClientId());

        final AuthorizationEntity persisted =
                authorizationJpaRepository.findById(authId).orElse(null);

        final RegisteredClientEntity clientEntity =
                registerClientJpaRepository.findById(rClientId).orElseThrow();

        final AuthorizationEntity authorizationEntity =
                authorizationMapper.oAuth2AuthorizationToAuthorizationEntity(
                        authorization,
                        clientEntity,
                        persisted == null
                );

        if (persisted != null) {
            markExistingTokens(authorizationEntity, persisted);
        }

        return authorizationMapper.authorizationEntityToAuthorization(
                authorizationJpaRepository.save(authorizationEntity)
        );
    }

    private static void markExistingTokens(final AuthorizationEntity current, final AuthorizationEntity persisted) {
        current.getTokens().forEach(token -> {
            if (persisted.getTokens().contains(token)) {
                token.markNotNew();
            }
        });
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
    public Authorization findById(final UUID authId) {
        if (authId == null) {
            return null;
        }
        return authorizationJpaRepository.findById(authId)
                                         .map(found -> {
                                             found.setNew(false);
                                             return authorizationMapper.authorizationEntityToAuthorization(found);
                                         })
                                         .orElse(null);
    }

    @Override
    public Authorization findByToken(final String token, final String tokenType) {
        final TokenType serializedTokenType = TokenType.getTokenTypeByWireName(tokenType);
        final String hashedValue = TokenHasher.hmacCurrent(token);
        return authorizationJpaRepository.findByTokens_TokenValueHash(hashedValue)
                                         .map(entity -> {
                                             entity.setNew(false);
                                             if (serializedTokenType != null) { // introspection
                                                 entity.getTokens()
                                                       .stream()
                                                       .filter(foundToken -> foundToken.getTokenValueHash().equals(hashedValue))
                                                       .findFirst()
                                                       .ifPresent(foundToken -> {
                                                           if (foundToken.getTokenType() != serializedTokenType) {
                                                               throw new IllegalArgumentException("Token type " + serializedTokenType + " does not match token type " + foundToken.getTokenType());
                                                           }
                                                       });
                                             }
                                             return authorizationMapper.authorizationEntityToAuthorization(entity);
                                         })
                                         .orElse(null);

    }

    // TODO [bdunaway][2026-Jan-23]: This is only used for testing, remove and refactor
    @Override
    public List<Authorization> findAll() {
        final List<AuthorizationEntity> authorizationEntities = authorizationJpaRepository.findAll();
        if (authorizationEntities.isEmpty()) {
            return new ArrayList<>();
        }
        return authorizationEntities.stream()
                                    .map(authorizationMapper::authorizationEntityToAuthorization)
                                    .collect(Collectors.toList());
    }

}
