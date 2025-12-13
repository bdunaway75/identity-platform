package io.github.blakedunaway.authserver.mapper;

import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import io.github.blakedunaway.authserver.business.model.enums.TokenSettingsTypes;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientGrantTypeEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientRedirectUriEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientScopeEntity;
import io.github.blakedunaway.authserver.util.AuthenticationUtility;
import io.github.blakedunaway.authserviceclient.dto.RegisteredClientDto;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Mapper
public abstract class RegisteredClientMapper {

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static Set<String> nullResolver(final Set<String> strings) {
        if (strings == null) {
            return Set.of();
        }
        return strings.stream()
                      .filter(s -> s != null && !s.isBlank())
                      .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public abstract RegisteredClientModel registeredClientToRegisteredClientModel(final RegisteredClient registeredClient);

    public RegisteredClientModel registeredClientDtoToRegisteredClientModel(final RegisteredClientDto dto) {
        if (dto == null) {
            return null;
        }

        final Set<String> authMethods = dto.getClientAuthenticationMethods() != null ? dto.getClientAuthenticationMethods() : Set.of();
        final Set<String> grantTypes = dto.getAuthorizationGrantTypes() != null ? dto.getAuthorizationGrantTypes() : Set.of();
        final Set<String> scopes = dto.getScopes() != null ? dto.getScopes() : Set.of();
        final Set<String> redirectUris = dto.getRedirectUris() != null ? new LinkedHashSet<>(dto.getRedirectUris()) : new LinkedHashSet<>();
        final Set<String> postLogoutRedirectUris = new HashSet<>();

        // TODO DEFAULTS
        final ClientSettings clientSettings = dto.getClientSettings() != null
                ? ClientSettings.withSettings(dto.getClientSettings())
                                .build()
                : ClientSettings.builder()
                                .build();

        final TokenSettings tokenSettings = dto.getTokenSettings() != null
                ? TokenSettings.withSettings(TokenSettingsTypes.normalize(dto.getTokenSettings()))
                               .build()
                : TokenSettings.builder()
                               .build();

        final Set<ClientAuthenticationMethod> camSet = authMethods.stream()
                                                                  .map(this::resolveAuthMethod)
                                                                  .collect(Collectors.toCollection(LinkedHashSet::new));

        final Set<AuthorizationGrantType> agtSet = grantTypes.stream()
                                                             .map(AuthorizationGrantType::new)
                                                             .collect(Collectors.toCollection(LinkedHashSet::new));

        return RegisteredClientModel.builder()
                                    .id(UUID.randomUUID()
                                            .toString())
                                    .clientId(dto.getClientId())
                                    .clientIdIssuedAt(dto.getClientIdIssuedAt()
                                                         .atZone(ZoneId.systemDefault())
                                                         .toInstant())
                                    .clientSecret(AuthenticationUtility.isArgon2Hash(dto.getClientSecret())
                                                          ? dto.getClientSecret()
                                                          : passwordEncoder.encode(dto.getClientSecret()))
                                    .clientSecretExpiresAt(dto.getClientSecretExpiresAt()
                                                              .atZone(ZoneId.systemDefault())
                                                              .toInstant())
                                    .clientName(dto.getClientName())
                                    .clientSettings(clientSettings)
                                    .tokenSettings(tokenSettings)
                                    .clientAuthenticationMethods(camSet)
                                    .authorizationGrantTypes(agtSet)
                                    .redirectUris(redirectUris)
                                    .postLogoutRedirectUris(postLogoutRedirectUris)
                                    .scopes(new LinkedHashSet<>(scopes))
                                    .build();
    }


    public RegisteredClientModel registeredClientEntityToRegisteredClientModel(final RegisteredClientEntity entity) {
        if (entity == null) {
            return null;
        }

        final Set<ClientAuthenticationMethod> authMethods = entity.getClientAuthenticationMethods() != null
                ? entity.getClientAuthenticationMethods()
                        .stream()
                        .map(method -> resolveAuthMethod(method.getClientAuthMethod()))
                        .collect(Collectors.toSet())
                : Set.of();

        final Set<AuthorizationGrantType> grantTypes = entity.getAuthorizationGrantTypes() != null
                ? entity.getAuthorizationGrantTypes()
                        .stream()
                        .map(RegisteredClientGrantTypeEntity::getAuthorizationGrantType)
                        .map(AuthorizationGrantType::new)
                        .collect(Collectors.toSet()) : Set.of();

        final Set<String> scopes = entity.getScopes() != null ? entity.getScopes()
                                                                      .stream()
                                                                      .map(RegisteredClientScopeEntity::getScope)
                                                                      .collect(Collectors.toSet()) : Set.of();

        final Set<String> redirectUris = entity.getRedirectUris() != null ? entity.getRedirectUris()
                                                                                  .stream()
                                                                                  .map(RegisteredClientRedirectUriEntity::getRedirectUri)
                                                                                  .collect(Collectors.toSet()) : new LinkedHashSet<>();
        final Set<String> postLogoutRedirectUris = new HashSet<>();

        // TODO DEFAULTS
        final ClientSettings clientSettings = entity.getClientSettings() != null
                ? ClientSettings.withSettings(entity.getClientSettings())
                                .build()
                : ClientSettings.builder()
                                .build();

        final TokenSettings tokenSettings = entity.getTokenSettings() != null
                ? TokenSettings.withSettings(TokenSettingsTypes.normalize(entity.getTokenSettings()))
                               .build()
                : TokenSettings.builder()
                               .build();


        return RegisteredClientModel.builder()
                                    .id(entity.getId())
                                    .clientId(entity.getClientId())
                                    .clientIdIssuedAt(entity.getClientIdIssuedAt()
                                                            .atZone(ZoneId.systemDefault())
                                                            .toInstant())
                                    .clientSecret(entity.getClientSecret() == null ? null :
                                                          AuthenticationUtility.isArgon2Hash(entity.getClientSecret())
                                                          ? entity.getClientSecret()
                                                          : passwordEncoder.encode(entity.getClientSecret()))
                                    .clientSecretExpiresAt(entity.getClientSecretExpiresAt()
                                                                 .atZone(ZoneId.systemDefault())
                                                                 .toInstant())
                                    .clientName(entity.getClientName())
                                    .clientSettings(clientSettings)
                                    .tokenSettings(tokenSettings)
                                    .clientAuthenticationMethods(authMethods)
                                    .authorizationGrantTypes(grantTypes)
                                    .redirectUris(redirectUris)
                                    .postLogoutRedirectUris(postLogoutRedirectUris)
                                    .scopes(new LinkedHashSet<>(scopes))
                                    .build();
    }


    public RegisteredClientEntity registeredModelClientToRegisteredClientEntity(final RegisteredClientModel src) {
        if (src == null) {
            return null;
        }
        final Map<String, Object> clientSettings = src.getClientSettings() != null ? src.getClientSettings() : Map.of();
        final Map<String, Object> tokenSettings = src.getTokenSettings() != null ? src.getTokenSettings() : Map.of();

        final RegisteredClientEntity clientEntity = RegisteredClientEntity.create(
                src.getId(),
                src.getClientId(),
                src.getClientIdIssuedAt(),
                src.getClientSecret() == null ? null : AuthenticationUtility.isArgon2Hash(src.getClientSecret()) ? src.getClientSecret() : passwordEncoder.encode(src.getClientSecret()),
                src.getClientSecretExpiresAt(),
                src.getClientName(),
                clientSettings,
                tokenSettings
        );

        // collections via parent helpers (keeps orphanRemoval working)
        src.getClientAuthenticationMethods().forEach(clientEntity::addClientAuthenticationMethod);
        src.getAuthorizationGrantTypes().forEach(clientEntity::addAuthorizationGrantType);
        nullResolver(src.getRedirectUris()).forEach(clientEntity::addRedirectUri);
        nullResolver(src.getPostLogoutRedirectUris()).forEach(clientEntity::addPostLogoutRedirectUri);
        nullResolver(src.getScopes()).forEach(clientEntity::addScope);

        return clientEntity;
    }

    /**
     * update existing JPA entity
     */
    public RegisteredClientEntity updateEntity(final RegisteredClientModel src, final RegisteredClientEntity target) {
        if (src == null || target == null) {
            throw new IllegalArgumentException();
        }

        final Map<String, Object> clientSettings = src.getClientSettings() != null ? src.getClientSettings() : new HashMap<>();
        final Map<String, Object> tokenSettings = src.getTokenSettings() != null ? src.getTokenSettings() : new HashMap<>();

        target.overwriteBasics(
                src.getClientId(),
               src.getClientIdIssuedAt(),
                src.getClientSecret(),
                src.getClientSecretExpiresAt(),
                src.getClientName(),
                clientSettings,
                tokenSettings
        );

        target.replaceClientAuthenticationMethods(src.getClientAuthenticationMethods());
        target.replaceAuthorizationGrantTypes(src.getAuthorizationGrantTypes());
        target.replaceRedirectUris(nullResolver(src.getRedirectUris()));
        target.replacePostLogoutRedirectUris(nullResolver(src.getPostLogoutRedirectUris()));
        target.replaceScopes(nullResolver(src.getScopes()));
        return target;
    }

    private ClientAuthenticationMethod resolveAuthMethod(final String authMethod) {
        if (authMethod == null) {
            return null;
        }
        return switch (authMethod) {
            case "client_secret_basic" -> ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
            case "client_secret_post" -> ClientAuthenticationMethod.CLIENT_SECRET_POST;
            case "client_secret_jwt" -> ClientAuthenticationMethod.CLIENT_SECRET_JWT;
            case "private_key_jwt" -> ClientAuthenticationMethod.PRIVATE_KEY_JWT;
            case "tls_client_auth" -> ClientAuthenticationMethod.TLS_CLIENT_AUTH;
            case "self_signed_tls_client_auth" -> ClientAuthenticationMethod.SELF_SIGNED_TLS_CLIENT_AUTH;
            default -> new ClientAuthenticationMethod(authMethod);
        };
    }

}
