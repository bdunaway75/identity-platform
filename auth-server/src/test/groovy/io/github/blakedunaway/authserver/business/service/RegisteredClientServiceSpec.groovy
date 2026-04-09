package io.github.blakedunaway.authserver.business.service

import io.github.blakedunaway.authserver.TestSpec
import io.github.blakedunaway.authserver.business.model.RegisteredClientModel
import io.github.blakedunaway.authserver.config.redis.RedisStore
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.test.annotation.DirtiesContext

import java.time.Duration
import java.time.LocalDateTime

@Import(RegisteredClientService)
class RegisteredClientServiceSpec extends TestSpec {

    @Autowired
    private RegisteredClientService service

    @SpringBean
    RedisStore redisStore = Mock()

    private static RegisteredClientModel minimalRegisteredClient(final String clientName) {
        return RegisteredClientModel.builder()
                                    .id(null)
                                    .clientId(null)
                                    .clientName(clientName)
                                    .clientSecret("secret")
                                    .clientAuthenticationMethods(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC))
                                    .authorizationGrantTypes(Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS))
                                    .tokenSettings(TokenSettings.builder().build())
                                    .clientSettings(ClientSettings.builder().build())
                                    .clientIdIssuedAt(LocalDateTime.now())
                                    .clientSecretExpiresAt(LocalDateTime.now())
                                    .postLogoutRedirectUris(Set.of("https://test.com/logout"))
                                    .scopes(Set.of("read"))
                                    .redirectUris(Set.of("https://test.com/callback"))
                                    .authorities(Set.of())
                                    .roles(Set.of())
                                    .build()
    }

    @DirtiesContext
    def "saveRegisteredClient persists requested authorities and roles through the entity relationship"() {
        given:
        def request = RegisteredClientModel.builder()
                                           .id(null)
                                           .clientId(null)
                                           .clientName("saved-with-access")
                                           .clientSecret("secret")
                                           .clientAuthenticationMethods(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC))
                                           .authorizationGrantTypes(Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS))
                                           .tokenSettings(TokenSettings.builder().build())
                                           .clientSettings(ClientSettings.builder().build())
                                           .clientIdIssuedAt(LocalDateTime.now())
                                           .clientSecretExpiresAt(LocalDateTime.now())
                                           .postLogoutRedirectUris(Set.of("https://test.com/logout"))
                                           .scopes(Set.of("read"))
                                           .redirectUris(Set.of("https://test.com/callback"))
                                           .authorities(Set.of("perm_read"))
                                           .roles(Set.of("role_support"))
                                           .build()

        when:
        def saved = service.saveRegisteredClient(request)

        then:
        saved != null
        saved.getAuthorities() == ["PERM_READ"] as Set
        saved.getRoles() == ["ROLE_SUPPORT"] as Set
    }

    @DirtiesContext
    def "updateRegisteredClientAuthorities persists normalized authorities and roles"() {
        given:
        def saved = service.saveRegisteredClient(minimalRegisteredClient("authority-client"))

        when:
        def updated = service.updateRegisteredClientAuthorities(
                saved.getId(),
                ["perm_read", "ROLE_SUPPORT", "role_admin"] as Set
        )

        then:
        updated != null
        updated.getAuthorities() == ["PERM_READ"] as Set
        updated.getRoles() == ["ROLE_SUPPORT", "ROLE_ADMIN"] as Set
    }

    @DirtiesContext
    def "updateRegisteredClient preserves existing security settings while updating client fields and access"() {
        given:
        def saved = service.saveRegisteredClient(minimalRegisteredClient("updatable-client"))
        def persistedBeforeUpdate = service.findRegisteredClientById(saved.getId())

        when:
        def updated = service.updateRegisteredClient(
                persistedBeforeUpdate,
                RegisteredClientModel.builder()
                                     .clientName("updated-client-name")
                                     .redirectUris(Set.of("https://updated.example.com/callback"))
                                     .postLogoutRedirectUris(Set.of("https://updated.example.com/logout"))
                                     .scopes(Set.of("read", "write"))
                                     .authorities(Set.of("perm_manage"))
                                     .roles(Set.of("role_support"))
                                     .build()
        )

        then:
        updated != null
        updated.getClientId() == persistedBeforeUpdate.getClientId()
        updated.getClientAuthenticationMethods() == persistedBeforeUpdate.getClientAuthenticationMethods()
        updated.getAuthorizationGrantTypes() == persistedBeforeUpdate.getAuthorizationGrantTypes()
        updated.getClientSecret() == persistedBeforeUpdate.getClientSecret()
        updated.getClientName() == "updated-client-name"
        updated.getRedirectUris() == ["https://updated.example.com/callback"] as Set
        updated.getPostLogoutRedirectUris() == ["https://updated.example.com/logout"] as Set
        updated.getScopes() == ["read", "write"] as Set
        updated.getAuthorities() == ["PERM_MANAGE"] as Set
        updated.getRoles() == ["ROLE_SUPPORT"] as Set
    }

    @DirtiesContext
    def "updateRegisteredClientAuthorities replaces removed authorities"() {
        given:
        def saved = service.saveRegisteredClient(minimalRegisteredClient("replace-authority-client"))
        service.updateRegisteredClientAuthorities(saved.getId(), ["PERM_READ", "ROLE_SUPPORT"] as Set)

        when:
        def updated = service.updateRegisteredClientAuthorities(saved.getId(), ["ROLE_ADMIN"] as Set)

        then:
        updated != null
        updated.getAuthorities().isEmpty()
        updated.getRoles() == ["ROLE_ADMIN"] as Set
    }

    @DirtiesContext
    def "saveRegisteredClient round-trips frontend token settings values through the mapper"() {
        given:
        def frontendTokenSettings = [
                accessTokenTimeToLive      : 'PT30M',
                refreshTokenTimeToLive    : 'PT43200M',
                authorizationCodeTimeToLive: 'PT5M',
                reuseRefreshTokens                : true,
        ]
        def request = RegisteredClientModel.builder()
                                           .id(null)
                                           .clientId(null)
                                           .clientName("token-settings-client")
                                           .clientSecret("secret")
                                           .clientAuthenticationMethods(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC))
                                           .authorizationGrantTypes(Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS))
                                           .tokenSettings(TokenSettings.withSettings(frontendTokenSettings).build())
                                           .clientSettings(ClientSettings.builder().build())
                                           .clientIdIssuedAt(LocalDateTime.now())
                                           .clientSecretExpiresAt(LocalDateTime.now())
                                           .postLogoutRedirectUris(Set.of("https://test.com/logout"))
                                           .scopes(Set.of("read"))
                                           .redirectUris(Set.of("https://test.com/callback"))
                                           .authorities(Set.of())
                                           .roles(Set.of())
                                           .build()

        when:
        def saved = service.saveRegisteredClient(request)
        def resolved = service.findRegisteredClientById(saved.getId())
        def resolvedTokenSettings = resolved.toOAuth2RegisteredClient().getTokenSettings()

        then:
        resolvedTokenSettings.getAccessTokenTimeToLive() == Duration.ofMinutes(30)
        resolvedTokenSettings.getRefreshTokenTimeToLive() == Duration.ofMinutes(43200)
        resolvedTokenSettings.getAuthorizationCodeTimeToLive() == Duration.ofMinutes(5)
        resolvedTokenSettings.isReuseRefreshTokens()
    }
}
