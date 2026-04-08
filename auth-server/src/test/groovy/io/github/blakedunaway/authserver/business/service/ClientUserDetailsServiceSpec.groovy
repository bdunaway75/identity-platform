package io.github.blakedunaway.authserver.business.service

import io.github.blakedunaway.authserver.TestSpec
import io.github.blakedunaway.authserver.business.model.RegisteredClientModel
import io.github.blakedunaway.authserver.business.model.user.ClientUser
import io.github.blakedunaway.authserver.config.redis.RedisStore
import io.github.blakedunaway.authserver.integration.repository.gateway.UserRepository
import io.github.blakedunaway.authserver.mapper.RegisteredClientMapper
import io.github.blakedunaway.authserver.mapper.UserMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Subject

import java.time.Instant
import java.time.LocalDateTime

@Import([UserService, RegisteredClientService])
class ClientUserDetailsServiceSpec extends TestSpec {

    @Autowired
    @Subject
    private UserService service

    @Autowired
    private UserRepository userRepository

    @Autowired
    private RegisteredClientService registeredClientService

    @Autowired
    private RegisteredClientMapper registeredClientMapper;

    @Autowired
    private UserMapper userMapper

    private static final String HASH = "\$argon2id\$v=19\$m=65536,t=2,p=1\$deadbeef\$deadbeef"

    @SpringBean
    private RedisStore redisStore = Mock()

    private RegisteredClient registeredClient;

    private static ClientUser user(String email = "alice@example.com",
                                   String rcId = "rc-1",
                                   String hash = HASH) {
        def now = LocalDateTime.now()
        return ClientUser.fromEmail(email)
                         .passwordHash(hash)
                         .clientId(rcId)
                         .plan("FREE")
                         .verified(true)
                         .createdAt(now)
                         .updatedAt(now)
                         .authorities { it.clear() }
                         .locked(false)
                         .expired(false)
                         .credentialsExpired(false)
                .build()
    }

    def setup() {
        redisStore.get(_ as String) >> [:]
        def rc = minimalRegisteredClient()
        rc = registeredClientService.saveRegisteredClient(rc)
        registeredClient = rc.toOAuth2RegisteredClient()
    }

    @DirtiesContext
    def "saveUser returns existing - isNew flipped false - when duplicate exists"() {
        given:
        def incoming = user("dup@example.com", registeredClient.getClientId(), HASH)

        when:
        service.saveUser(incoming)

        then:
        def persistedUser = service.loadUserDetailsByEmailAndClientId(registeredClient.getClientId(), incoming.getEmail())
        persistedUser != null
        persistedUser.getPassword() == HASH
    }

    @DirtiesContext
    def "saveUser returns existing when duplicate exists"() {
        given:
        def incoming = user("dup@example.com", registeredClient.getClientId(), HASH)

        when:
        service.saveUser(incoming)

        then:
        def persistedUser = service.loadUserDetailsByEmailAndClientId(registeredClient.getClientId(), "dup@example.com")
        persistedUser != null
        persistedUser.getPassword() == HASH
    }

    @DirtiesContext
    def "loadUserByEmailAndClientId returns UserDetails for existing user"() {
        given:
        def u = user("alice@example.com", registeredClient.getClientId(), HASH)
        userRepository.save(u)

        when:
        UserDetails details = service.loadUserDetailsByEmailAndClientId(registeredClient.getClientId(), "alice@example.com")

        then:
        details != null
        details.username == "alice@example.com"
        details.password == HASH
        details.accountNonExpired
        details.accountNonLocked
        details.credentialsNonExpired
        details.enabled
    }

    private static RegisteredClientModel minimalRegisteredClient() {
        RegisteredClientModel.builder()
                             .id(null)
                             .clientId(null)
                             .clientName("pooperia")
                             .clientSecret("secret")
                             .clientAuthenticationMethods(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC))
                             .authorizationGrantTypes(Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS))
                             .tokenSettings(TokenSettings.builder().build())
                             .clientSettings(ClientSettings.builder().requireProofKey(false).build())
                             .clientIdIssuedAt(Instant.now())
                             .clientSecretExpiresAt(Instant.now())
                             .postLogoutRedirectUris(Set.of("http://test.com/*"))
                             .scopes(Set.of("read"))
                             .redirectUris(Set.of("http://test.com/*"))
                             .tokenSettings(TokenSettings.builder().build())
                             .clientSettings(ClientSettings.builder().build())
                             .build()
    }
}
