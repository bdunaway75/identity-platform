package io.github.blakedunaway.authserver.mapper;

import io.github.blakedunaway.authserver.business.api.dto.request.RegisteredClientRequest;
import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import io.github.blakedunaway.authserver.business.model.enums.AuthorizationGrantTypeInternal;
import io.github.blakedunaway.authserver.business.model.enums.ClientAuthenticationMethodResolver;
import io.github.blakedunaway.authserver.business.validation.RegisteredClientSettingsValidator;
import io.github.blakedunaway.authserver.integration.entity.AuthorityEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientAuthMethodEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientGrantTypeEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientPostLogoutRedirectUriEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientRedirectUriEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientScopeEntity;
import io.github.blakedunaway.authserver.util.AuthenticationUtility;
import io.github.blakedunaway.authserver.util.AuthorityUtility;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper
public abstract class RegisteredClientMapper {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenSettingsMapper tokenSettingsMapper;

    private static Set<String> nullResolver(final Set<String> strings) {
        if (strings == null) {
            return Set.of();
        }
        return strings.stream()
                      .filter(s -> s != null && !s.isBlank())
                      .collect(Collectors.toCollection(HashSet::new));
    }

    private static Set<String> mergeAuthorityNames(final RegisteredClientModel registeredClientModel) {
        if (registeredClientModel == null) {
            return Set.of();
        }

        final Set<String> mergedAuthorityNames = new HashSet<>(AuthorityUtility.normalizeAuthorities(registeredClientModel.getAuthorities()));
        mergedAuthorityNames.addAll(AuthorityUtility.normalizeRoles(registeredClientModel.getRoles()));
        return mergedAuthorityNames;
    }

    public RegisteredClientModel registeredClientToRegisteredClientModel(final RegisteredClient registeredClient) {
        if (registeredClient == null) {
            return null;
        }

        return RegisteredClientModel.builder()
                                    .id(registeredClient.getId() == null ? null : UUID.fromString(registeredClient.getId()))
                                    .clientId(registeredClient.getClientId())
                                    .clientIdIssuedAt(instantToLocalDateTime(registeredClient.getClientIdIssuedAt()))
                                    .clientSecret(registeredClient.getClientSecret())
                                    .clientSecretExpiresAt(instantToLocalDateTime(registeredClient.getClientSecretExpiresAt()))
                                    .clientName(registeredClient.getClientName())
                                    .clientAuthenticationMethods(new HashSet<>(registeredClient.getClientAuthenticationMethods()))
                                    .authorizationGrantTypes(new HashSet<>(registeredClient.getAuthorizationGrantTypes()))
                                    .redirectUris(new HashSet<>(registeredClient.getRedirectUris()))
                                    .postLogoutRedirectUris(new HashSet<>(registeredClient.getPostLogoutRedirectUris()))
                                    .scopes(new HashSet<>(registeredClient.getScopes()))
                                    .clientSettings(registeredClient.getClientSettings())
                                    .tokenSettings(registeredClient.getTokenSettings())
                                    .build();
    }

    @Named("instantToLocalDateTime")
    public LocalDateTime instantToLocalDateTime(final Instant value) {
        if (value == null) {
            return null;
        }
        return LocalDateTime.ofInstant(value, ZoneId.systemDefault());
    }

    @Named("localDateTimeToInstant")
    public Instant localDateTimeToInstant(final LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(ZoneId.systemDefault()).toInstant();
    }

    public RegisteredClientModel registeredClientRequestToRegisteredClientModel(final RegisteredClientRequest request) {
        if (request == null) {
            return null;
        }

        final Set<String> authMethods = request.getClientAuthenticationMethods() != null ?
                                        request.getClientAuthenticationMethods() : Set.of();
        final Set<String> grantTypes = request.getAuthorizationGrantTypes() != null ? request.getAuthorizationGrantTypes() :
                                       Set.of();
        final Set<String> scopes = request.getScopes() != null ? request.getScopes() : Set.of();
        final Set<String> authorities = request.getAuthorities() != null ? request.getAuthorities() : Set.of();
        final Set<String> roles = request.getRoles() != null ? request.getRoles() : Set.of();
        final Set<String> redirectUris = request.getRedirectUris() != null ? new HashSet<>(request.getRedirectUris()) :
                                         new HashSet<>();
        final Set<String> postLogoutRedirectUris = request.getPostLogoutRedirectUris() != null
                                                   ? new HashSet<>(request.getPostLogoutRedirectUris())
                                                   : new HashSet<>();

        RegisteredClientSettingsValidator.validateClientSettings(request.getClientSettings());
        RegisteredClientSettingsValidator.validateTokenSettings(request.getTokenSettings());

        final ClientSettings clientSettings = ClientSettings.withSettings(request.getClientSettings()).build();
        final TokenSettings tokenSettings = TokenSettings.withSettings(request.getTokenSettings()).build();

        final Set<ClientAuthenticationMethod> camSet = authMethods.stream()
                                                                  .map(ClientAuthenticationMethodResolver::resolve)
                                                                  .collect(Collectors.toCollection(HashSet::new));

        final Set<AuthorizationGrantType> agtSet = grantTypes.stream()
                                                             .map(AuthorizationGrantType::new)
                                                             .collect(Collectors.toCollection(HashSet::new));

        final String clientSecret = request.getClientSecret() == null
                                    ? null
                                    : AuthenticationUtility.isArgon2Hash(request.getClientSecret())
                                      ? request.getClientSecret()
                                      : passwordEncoder.encode(request.getClientSecret());


        return RegisteredClientModel.builder()
                                    .clientId(request.getClientId())
                                    .clientIdIssuedAt(request.getClientIdIssuedAt())
                                    .clientSecret(clientSecret)
                                    .clientSecretExpiresAt(request.getClientSecretExpiresAt())
                                    .clientName(request.getClientName())
                                    .clientSettings(clientSettings)
                                    .tokenSettings(tokenSettings)
                                    .clientAuthenticationMethods(camSet)
                                    .authorizationGrantTypes(agtSet)
                                    .redirectUris(redirectUris)
                                    .postLogoutRedirectUris(postLogoutRedirectUris)
                                    .scopes(new HashSet<>(scopes))
                                    .authorities(new HashSet<>(authorities))
                                    .roles(new HashSet<>(roles))
                                    .build();
    }


    public RegisteredClientModel registeredClientEntityToRegisteredClientModel(final RegisteredClientEntity entity) {
        if (entity == null) {
            return null;
        }

        final Set<ClientAuthenticationMethod> authMethods = entity.getClientAuthenticationMethods() != null
                                                            ? entity.getClientAuthenticationMethods()
                                                                    .stream()
                                                                    .map(RegisteredClientAuthMethodEntity::getClientAuthMethod)
                                                                    .map(ClientAuthenticationMethodResolver::resolve)
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
                                                                                  .collect(Collectors.toSet()) :
                                         new HashSet<>();

        final Set<String> postLogoutRedirectUris = entity.getPostLogoutRedirectUris() == null
                                                   ? null : entity.getPostLogoutRedirectUris()
                                                                  .stream()
                                                                  .map(RegisteredClientPostLogoutRedirectUriEntity::getPostLogoutRedirectUri)
                                                                  .collect(Collectors.toSet());

        final ClientSettings clientSettings = ClientSettings.withSettings(entity.getClientSettings()).build();

        final TokenSettings tokenSettings = tokenSettingsMapper.tokenSettingsJsonToOAuthTokenSettings(entity.getTokenSettings());

        final Set<String> authorities = entity.getAuthorities() == null
                                        ? new HashSet<>()
                                        : entity.getAuthorities().stream().map(AuthorityEntity::getName).collect(Collectors.toSet());

        return RegisteredClientModel.builder()
                                    .id(entity.getRegisteredClientId())
                                    .clientId(entity.getClientId())
                                    .clientIdIssuedAt(entity.getClientIdIssuedAt())
                                    .clientSecret(entity.getClientSecret() == null ? null :
                                                  AuthenticationUtility.isArgon2Hash(entity.getClientSecret())
                                                  ? entity.getClientSecret()
                                                  : passwordEncoder.encode(entity.getClientSecret()))
                                    .clientSecretExpiresAt(entity.getClientSecretExpiresAt())
                                    .clientName(entity.getClientName())
                                    .clientSettings(clientSettings)
                                    .tokenSettings(tokenSettings)
                                    .clientAuthenticationMethods(authMethods)
                                    .authorizationGrantTypes(grantTypes)
                                    .redirectUris(redirectUris)
                                    .postLogoutRedirectUris(postLogoutRedirectUris)
                                    .scopes(new HashSet<>(scopes))
                                    .authorities(AuthorityUtility.extractAuthorities(authorities))
                                    .roles(AuthorityUtility.extractRoles(authorities))
                                    .build();
    }

    public RegisteredClientEntity registeredModelClientToRegisteredClientEntity(final RegisteredClientModel src) {
        if (src == null) {
            return null;
        }

        final RegisteredClientEntity clientEntity = RegisteredClientEntity.create(
                src.getId(),
                src.getClientId(),
                src.getClientIdIssuedAt(),
                src.getClientSecret() == null
                ? null
                : AuthenticationUtility.isArgon2Hash(src.getClientSecret()) ? src.getClientSecret() :
                  passwordEncoder.encode(
                          src.getClientSecret()),
                src.getClientSecretExpiresAt(),
                src.getClientName(),
                src.getClientSettings(),
                tokenSettingsMapper.mapToTokenSettingsJson(src.getTokenSettings())
        );

        src.getClientAuthenticationMethods().forEach(clientEntity::addClientAuthenticationMethod);
        src.getAuthorizationGrantTypes().forEach(clientEntity::addAuthorizationGrantType);
        mergeAuthorityNames(src).forEach(clientEntity::addAuthority);
        nullResolver(src.getRedirectUris()).forEach(clientEntity::addRedirectUri);
        nullResolver(src.getPostLogoutRedirectUris()).forEach(clientEntity::addPostLogoutRedirectUri);

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

        target.overwriteBasics(
                src.getClientId(),
                src.getClientIdIssuedAt(),
                src.getClientSecret(),
                src.getClientSecretExpiresAt(),
                src.getClientName(),
                clientSettings,
                tokenSettingsMapper.mapToTokenSettingsJson(src.getTokenSettings())
        );

        target.replaceClientAuthenticationMethods(src.getClientAuthenticationMethods());
        target.replaceAuthorizationGrantTypes(src.getAuthorizationGrantTypes());
        target.replaceRedirectUris(nullResolver(src.getRedirectUris()));
        target.replacePostLogoutRedirectUris(nullResolver(src.getPostLogoutRedirectUris()));
        target.replaceAuthorities(mergeAuthorityNames(src));
        return target;
    }

    public RegisteredClient registeredClientModelToRegisteredClient(final RegisteredClientModel src) {
        final Instant issuedAt = src.getClientIdIssuedAt() == null ? null
                                                                   : src.getClientIdIssuedAt().atZone(ZoneId.systemDefault()).toInstant();

        final Instant secretExpiresAt = src.getClientSecretExpiresAt() == null ? null
                                                                               : src.getClientSecretExpiresAt().atZone(ZoneId.systemDefault()).toInstant();

        return RegisteredClient.withId(String.valueOf(src.getId()))
                               .clientId(src.getClientId())
                               .clientIdIssuedAt(issuedAt)
                               .clientSecret(src.getClientSecret())
                               .clientSecretExpiresAt(secretExpiresAt)
                               .clientName(src.getClientName())
                               .clientSettings(ClientSettings.withSettings(src.getClientSettings()).build())
                               .tokenSettings(TokenSettings.withSettings(src.getTokenSettings()).build())
                               .clientAuthenticationMethods(set -> set.addAll(src.getClientAuthenticationMethods()
                                                                                 .stream()
                                                                                 .map(ClientAuthenticationMethod::valueOf)
                                                                                 .toList()))
                               .authorizationGrantTypes(set ->
                                                                set.addAll(src.getAuthorizationGrantTypes()
                                                                              .stream()
                                                                              .map(AuthorizationGrantTypeInternal::findByName)
                                                                              .map(AuthorizationGrantTypeInternal::getAuthorizationGrantType)
                                                                              .toList()))
                               .redirectUris(set -> set.addAll(src.getRedirectUris()))
                               .postLogoutRedirectUris(set -> set.addAll(src.getPostLogoutRedirectUris()))
                               .scopes(set -> set.addAll(src.getScopes()))
                               .build();
    }

}
