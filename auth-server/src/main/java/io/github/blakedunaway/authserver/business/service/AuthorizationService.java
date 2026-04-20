package io.github.blakedunaway.authserver.business.service;

import io.github.blakedunaway.authserver.business.model.Authorization;
import io.github.blakedunaway.authserver.business.model.AuthToken;
import io.github.blakedunaway.authserver.business.model.enums.TokenType;
import io.github.blakedunaway.authserver.config.redis.RedisStore;
import io.github.blakedunaway.authserver.integration.repository.gateway.AuthorizationRepository;
import io.github.blakedunaway.authserver.mapper.AuthorizationMapper;
import io.github.blakedunaway.authserver.util.RedisUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AuthorizationService implements OAuth2AuthorizationService {


    private final AuthorizationRepository authorizationRepository;

    private final AuthorizationMapper authorizationMapper;

    private final RedisStore redisStore;

    @Override
    public void save(final OAuth2Authorization src) {
        final Authorization authorization = authorizationRepository.save(src);
        redisStore.put(
                RedisUtility.AUTHORIZATION_ATTRIBUTES + authorization.getId().toString(),
                src.getAttributes(),
                resolveAuthorizationAttributesTtl(authorization)
        );
    }

    @Override
    public void remove(final OAuth2Authorization authorization) {
        authorizationRepository.remove(authorization.getId());
        redisStore.consume(RedisUtility.AUTHORIZATION_ATTRIBUTES + authorization.getId());
    }

    @Override
    public OAuth2Authorization findById(final String id) {
        Assert.notNull(id, "id must not be null");
        return authorizationMapper.authorizationToOAuth2Authorization(authorizationRepository.findById(UUID.fromString(id)));
    }

    @Override
    public OAuth2Authorization findByToken(final String rawToken, final OAuth2TokenType tokenType) {
        Assert.hasText(rawToken, "token cannot be empty");
        final String tokenTypeValue = tokenType == null ? null : tokenType.getValue();
        final Authorization authorization = authorizationRepository.findByToken(rawToken, tokenTypeValue);
        return authorization == null ? null : authorizationMapper.authorizationToOAuth2Authorization(authorization);
    }

    private Duration resolveAuthorizationAttributesTtl(final Authorization authorization) {
        final Instant now = Instant.now();

        return authorization.getTokens()
                            .stream()
                            .filter(Objects::nonNull)
                            .filter(token -> TokenType.ID_TOKEN.equals(token.getTokenType()))
                            .map(AuthToken::getExpiresAt)
                            .filter(Objects::nonNull)
                            .max(Comparator.naturalOrder())
                            .map(expiresAt -> Duration.between(now, expiresAt).plusMinutes(5))
                            .filter(duration -> !duration.isNegative() && !duration.isZero())
                            .orElse(Duration.ofHours(1));
    }

}
