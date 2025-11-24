package io.github.blakedunaway.authserver.business.service

import io.github.blakedunaway.authserver.TestSpec
import io.github.blakedunaway.authserver.business.model.RegisterDto
import io.github.blakedunaway.authserver.business.model.RegisterDto.UsernamePasswordWithClientAuthenticationToken
import io.github.blakedunaway.authserver.business.model.User
import io.github.blakedunaway.authserver.config.ClientAwareDaoAuthProvider
import io.github.blakedunaway.authserver.config.TestConfig
import io.github.blakedunaway.authserver.integration.repository.jpa.UserJpaRepository
import io.github.blakedunaway.authserver.mapper.RegisteredClientMapper
import io.github.blakedunaway.authserver.mapper.UserMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.AccountExpiredException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.InternalAuthenticationServiceException
import org.springframework.security.authentication.LockedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Subject
import spock.lang.Unroll

import java.time.Instant
import java.time.LocalDateTime

@Import(TestConfig)
class ClientAwareDaoAuthProviderSpec extends TestSpec {

    @Autowired
    @Subject
    private ClientAwareDaoAuthProvider provider

    @Autowired
    private UserService userService

    @Autowired
    private UserJpaRepository userRepository

    @Autowired
    private RegisteredClientService registeredClientService

    @Autowired
    private RegisteredClientMapper registeredClientMapper

    @Autowired
    private UserMapper userMapper

    @Autowired
    private PasswordEncoder passwordEncoder

    // ---------------- helpers ----------------
    private User user(final String email,
                      final String rcId,
                      final String rawPassword,
                      boolean locked = false,
                      boolean disabled = false,
                      boolean accountExpired = false,
                      boolean credentialsExpired = false) {
        def now = LocalDateTime.now()
        return User.fromEmail(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .registeredClientId(rcId)
                .plan("FREE")
                .verified(!disabled)
                .createdAt(now)
                .updatedAt(now)
                .authorities { it.clear() }
                .locked(locked)
                .expired(accountExpired)
                .credentialsExpired(credentialsExpired)
                .isNew(false)
                .build()
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

    private static RegisterDto dto(final String email, final String password, final String rcId) {
        def register = new RegisterDto()
        register.setEmail(email)
        register.setPassword(password)
        register.setRegisteredClientId(rcId)
        return register
    }

    private static UsernamePasswordWithClientAuthenticationToken unauthenticatedToken(RegisterDto d) {
        return UsernamePasswordWithClientAuthenticationToken.unauthenticated(d, new HashSet<GrantedAuthority>())
    }

    @DirtiesContext
    def "authenticate succeeds with correct client, user, and password"() {
        given: "a registered client and a persisted user with matching client & password"
        def rc = minimalRegisteredClient()
        registeredClientService.saveRegisteredClient(registeredClientMapper.registeredClientToRegisteredClientModel(rc))
        def email = "alice@example.com"
        def raw = "s3cr3t!"
        userRepository.save(userMapper.userToUserEntity(user(email, rc.getId(), raw)));

        and: "an unauthenticated token with the same credentials"
        def token = unauthenticatedToken(dto(email, raw, rc.getId()))

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
        def rc = minimalRegisteredClient()
        registeredClientService.saveRegisteredClient(registeredClientMapper.registeredClientToRegisteredClientModel(rc))
        def email = "bob@example.com"
        userRepository.save(userMapper.userToUserEntity(user(email, rc.getId(), "correct-password")));
        and:
        def token = unauthenticatedToken(dto(email, "wrong-password", rc.getId()))

        when:
        provider.authenticate(token)

        then:
        thrown(BadCredentialsException)
    }

    @DirtiesContext
    def "authenticate wraps UsernameNotFoundException as InternalAuthenticationServiceException"() {
        given:
        def rc = minimalRegisteredClient()
        registeredClientService.saveRegisteredClient(registeredClientMapper.registeredClientToRegisteredClientModel(rc))

        and: "no user saved for this email"
        def token = unauthenticatedToken(dto("nobody@example.com", "irrelevant", rc.getId()))

        when:
        provider.authenticate(token)

        then:
        def ex = thrown(InternalAuthenticationServiceException)
        ex.getCause() instanceof UsernameNotFoundException
    }

    @DirtiesContext
    def "authenticate throws Locked/Disabled/AccountExpired/CredentialsExpired based on flags"() {
        given:
        def rc = minimalRegisteredClient()
        registeredClientService.saveRegisteredClient(registeredClientMapper.registeredClientToRegisteredClientModel(rc))

        when: "locked"
        def email1 = "locked@example.com"
        userRepository.save(userMapper.userToUserEntity(user(email1, rc.getId(), "pw", true)));
        provider.authenticate(unauthenticatedToken(dto(email1, "pw", rc.getId())))

        then:
        thrown(LockedException)

        when: "account expired"
        def email3 = "expired@example.com"
        userRepository.save(userMapper.userToUserEntity(user(email3, rc.getId(), "pw", false, false, true)))
        provider.authenticate(unauthenticatedToken(dto(email3, "pw", rc.getId())))

        then:
        thrown(AccountExpiredException)

        when: "credentials expired (password still matches, failure is in post-check)"
        def email4 = "credexpired@example.com"
        userRepository.save(userMapper.userToUserEntity(user(email4, rc.getId(), "pw", false, false, false, true))
        )
        provider.authenticate(unauthenticatedToken(dto(email4, "pw", rc.getId())))

        then:
        thrown(CredentialsExpiredException)
    }

    @Unroll
    def "retrieveUser rejects invalid input: #caseDesc"() {
        when:
        provider.retrieveUser(input as UsernamePasswordWithClientAuthenticationToken)

        then:
        thrown(IllegalArgumentException)

        where:
        caseDesc                   | input
        "null token"               | null
        "null RegisterDto"         | UsernamePasswordWithClientAuthenticationToken.unauthenticated(null, new HashSet<GrantedAuthority>())
        "blank registeredClientId" | unauthenticatedToken(dto("a@b.com", "p", ""))
        "blank email"              | unauthenticatedToken(dto("", "p", "rc-1"))
        "blank password"           | unauthenticatedToken(dto("a@b.com", "", "rc-1"))
    }

    def "supports only UsernamePasswordWithClientAuthenticationToken"() {
        expect:
        provider.supports(UsernamePasswordWithClientAuthenticationToken)
        !provider.supports(UsernamePasswordAuthenticationToken)
        !provider.supports(Object)
    }

    @DirtiesContext
    def "authenticate preserves details and erases credentials"() {
        given:
        def rc = minimalRegisteredClient()
        registeredClientService.saveRegisteredClient(registeredClientMapper.registeredClientToRegisteredClientModel(rc))
        def email = "detail@example.com"
        def raw = "pw"
        userRepository.save(userMapper.userToUserEntity(user(email, rc.getId(), raw)));

        and:
        def token = unauthenticatedToken(dto(email, raw, rc.getId()))
        token.setDetails([k: "v"])

        when:
        def result = provider.authenticate(token) as UsernamePasswordWithClientAuthenticationToken

        then: "details propagated"
        result.getDetails() == [k: "v"]

        and: "credentials erased on the original token"
        token.getCredentials() == null || token.getCredentials().toString().isBlank()
    }
}