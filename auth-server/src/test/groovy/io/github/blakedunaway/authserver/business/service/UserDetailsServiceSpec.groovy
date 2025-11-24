package io.github.blakedunaway.authserver.business.service

import io.github.blakedunaway.authserver.TestSpec
import io.github.blakedunaway.authserver.business.model.User
import io.github.blakedunaway.authserver.config.TestConfig
import io.github.blakedunaway.authserver.integration.repository.gateway.UserRepository
import io.github.blakedunaway.authserver.mapper.RegisteredClientMapper
import io.github.blakedunaway.authserver.mapper.UserMapper
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

@Import(TestConfig)
class UserDetailsServiceSpec extends TestSpec {

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

    private static User user(String email = "alice@example.com",
                             String rcId = "rc-1",
                             String hash = HASH,
                             boolean isNew = true) {
        def now = LocalDateTime.now()
        return User.fromEmail(email)
                .passwordHash(hash)
                .registeredClientId(rcId)
                .plan("FREE")
                .verified(true)
                .createdAt(now)
                .updatedAt(now)
                .authorities { it.clear() }
                .locked(false)
                .expired(false)
                .credentialsExpired(false)
                .isNew(isNew)
                .build()
    }

    @DirtiesContext
    def "saveUser returns existing - isNew flipped false - when duplicate exists"() {
        given:
        def rc = minimalRegisteredClient()
        registeredClientService.saveRegisteredClient(registeredClientMapper.registeredClientToRegisteredClientModel(rc))
        def incoming = user("dup@example.com", rc.getId(), HASH, true)

        when:
        service.saveUser(incoming)

        then:
        def persistedUser = userRepository.findByEmail(incoming.getEmail())
        persistedUser != null
        persistedUser.getPasswordHash() == HASH
        !persistedUser.isNew()
        persistedUser.getRegisteredClientId() == rc.getId()
    }

    @DirtiesContext
    def "loadUserByUsername returns UserDetails for existing user"() {
        given:
        def rc = minimalRegisteredClient()
        registeredClientService.saveRegisteredClient(registeredClientMapper.registeredClientToRegisteredClientModel(rc))
        def u = user("alice@example.com", rc.getId(), HASH, false)
        userRepository.save(u)

        when:
        UserDetails details = service.loadUserByUsername("alice@example.com")

        then:
        details != null
        details.username == "alice@example.com"
        details.password == HASH
        details.accountNonExpired
        details.accountNonLocked
        details.credentialsNonExpired
        details.enabled
    }

    private static RegisteredClient minimalRegisteredClient() {
        RegisteredClient.withId("rc-" + UUID.randomUUID().toString())
                .clientId("client-" + UUID.randomUUID().toString())
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenSettings(TokenSettings.builder().build())
                .clientSettings(ClientSettings.builder().requireProofKey(false).build())
                .clientIdIssuedAt(Instant.now())
                .clientSecretExpiresAt(Instant.now())
                .build()
    }
}
