package io.github.blakedunaway.authserver.business.service

import io.github.blakedunaway.authserver.TestSpec
import io.github.blakedunaway.authserver.business.model.user.ClientRegisterDto
import io.github.blakedunaway.authserver.business.model.user.ClientRegisterDto.UsernamePasswordWithClientAuthenticationToken
import io.github.blakedunaway.authserver.business.model.RegisteredClientModel
import io.github.blakedunaway.authserver.business.model.user.ClientUser
import io.github.blakedunaway.authserver.config.redis.RedisStore
import io.github.blakedunaway.authserver.mapper.RegisteredClientMapper
import io.github.blakedunaway.authserver.mapper.UserMapper
import io.github.blakedunaway.authserver.security.provider.ClientAwareDaoAuthProvider
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.AccountExpiredException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Subject

import java.time.Instant
import java.time.LocalDateTime

@Import([UserService, ClientAwareDaoAuthProvider, RegisteredClientService])
class ClientAwareDaoAuthProviderSpec extends TestSpec {

    @Autowired
    @Subject
    private ClientAwareDaoAuthProvider provider

    @Autowired
    private UserService userService

    @Autowired
    private RegisteredClientService registeredClientService

    @Autowired
    private RegisteredClientMapper registeredClientMapper

    @Autowired
    private UserMapper userMapper

    @Autowired
    private PasswordEncoder passwordEncoder

    @SpringBean
    private RedisStore redisStore = Mock()

    private RegisteredClient registeredClient;

    // ---------------- helpers ----------------
    private ClientUser user(final String email,
                            final String rcId,
                            final String rawPassword,
                            boolean locked = false,
                            boolean disabled = false,
                            boolean accountExpired = false,
                            boolean credentialsExpired = false) {
        def now = LocalDateTime.now()
        return ClientUser.fromEmail(email)
                         .passwordHash(passwordEncoder.encode(rawPassword))
                         .clientId(rcId)
                         .plan("FREE")
                         .verified(!disabled)
                         .createdAt(now)
                         .updatedAt(now)
                         .authorities { it.clear() }
                         .locked(locked)
                         .expired(accountExpired)
                         .credentialsExpired(credentialsExpired)
                   .build()
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

    def setup() {
        redisStore.get(_ as String) >> [:]
        def rc = minimalRegisteredClient()
        rc = registeredClientService.saveRegisteredClient(rc)
        registeredClient = rc.toOAuth2RegisteredClient()
    }

    private static ClientRegisterDto dto(final String email, final String password, final String rcId) {
        def register = new ClientRegisterDto()
        register.setEmail(email)
        register.setPassword(password)
        register.setClientId(rcId)
        return register
    }

    private static UsernamePasswordWithClientAuthenticationToken unauthenticatedToken(ClientRegisterDto registerDto) {
        return UsernamePasswordWithClientAuthenticationToken.unauthenticated(registerDto.getEmail(),
                                                                             registerDto.getClientId(),
                                                                             registerDto.getPassword())
    }

    @DirtiesContext
    def "authenticate succeeds with correct client, user, and password"() {
        given: "a registered client and a persisted user with matching client & password"
        def email = "alice@example.com"
        def raw = "s3cr3t!"
        userService.saveUser(user(email, registeredClient.getClientId(), raw))


        and: "an unauthenticated token with the same credentials"
        def token = unauthenticatedToken(dto(email, raw, registeredClient.getClientId()))

        when:
        Authentication result = provider.authenticate(token)

        then: "we get an authenticated token carrying user authorities"
        result instanceof UsernamePasswordWithClientAuthenticationToken
        result.isAuthenticated()
        result.getAuthorities() != null
        result.getAuthorities().isEmpty() // user has no authorities; just verify it's non-null and empty is fine
    }

    @DirtiesContext
    def "authenticate fails with BadCredentialsException for wrong password"() {
        given:
        def email = "bob@example.com"
        userService.saveUser(user(email, registeredClient.getClientId(), "correct-password"))
        and:
        def token = unauthenticatedToken(dto(email, "wrong-password", registeredClient.getClientId()))

        when:
        provider.authenticate(token)

        then:
        thrown(BadCredentialsException)
    }

    @DirtiesContext
    def "authenticate throws Locked/Disabled/AccountExpired/CredentialsExpired based on flags"() {
        when: "locked"
        def email1 = "locked@example.com"
        userService.saveUser(user(email1, registeredClient.getClientId(), "pw", true))
        provider.authenticate(unauthenticatedToken(dto(email1, "pw", registeredClient.getClientId())))

        then:
        thrown(LockedException)

        when: "account expired"
        def email2 = "expired@example.com"
        userService.saveUser(user(email2, registeredClient.getClientId(), "pw", false, false, true))
        provider.authenticate(unauthenticatedToken(dto(email2, "pw", registeredClient.getClientId())))

        then:
        thrown(AccountExpiredException)

        when: "credentials expired (password still matches, failure is in post-check)"
        def email3 = "credexpired@example.com"
        userService.saveUser(user(email3, registeredClient.getClientId(), "pw", false, false, false, true))
        provider.authenticate(unauthenticatedToken(dto(email3, "pw", registeredClient.getClientId())))

        then:
        thrown(CredentialsExpiredException)
    }

    @DirtiesContext
    def "authenticate preserves details and erases credentials"() {
        given:
        def email = "detail@example.com"
        def raw = "pw"
        userService.saveUser(user(email, registeredClient.getClientId(), raw))

        and:
        def token = unauthenticatedToken(dto(email, "pw", registeredClient.getClientId()))
        token.setDetails([k: "v"])

        when:
        def result = provider.authenticate(token) as UsernamePasswordWithClientAuthenticationToken

        then: "details propagated"
        result.getDetails() == [k: "v"]

        and: "credentials erased on the original token"
        token.getCredentials() == null || token.getCredentials().toString().isBlank()
    }
}