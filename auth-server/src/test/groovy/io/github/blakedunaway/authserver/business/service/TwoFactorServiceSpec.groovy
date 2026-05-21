package io.github.blakedunaway.authserver.business.service

import io.github.blakedunaway.authserver.business.model.user.ClientRegisterDto
import io.github.blakedunaway.authserver.business.model.user.ClientUser
import io.github.blakedunaway.authserver.business.model.user.PendingTwoFactorSignUp
import io.github.blakedunaway.authserver.business.model.user.PlatformRegisterDto
import io.github.blakedunaway.authserver.business.model.user.PlatformUser
import io.github.blakedunaway.authserver.config.redis.RedisStore
import spock.lang.Specification

import java.time.Duration

class TwoFactorServiceSpec extends Specification {

    def redisStore = Mock(RedisStore)
    def emailService = Mock(EmailService)
    def eventStreamService = Mock(EventStreamService)
    def userService = Mock(UserService)
    def passwordEncoder = Mock(org.springframework.security.crypto.password.PasswordEncoder)
    def service = new TwoFactorService(redisStore, emailService, eventStreamService, userService, passwordEncoder)

    def setup() {
        service.@issuer = "https://auth.example.com"
    }

    def "client sign up stores pending verification in redis and sends email"() {
        given:
        def dto = new ClientRegisterDto("user@example.com", "password123", "client-123")

        when:
        def result = service.sendClientSignUpVerification(dto)

        then:
        1 * passwordEncoder.encode("password123") >> "\$argon2-client"
        1 * eventStreamService.createSessionId() >> "session-123"
        1 * redisStore.put({ it.startsWith("two-factor:sign-up:") }, {
            it instanceof PendingTwoFactorSignUp &&
                    it.email == "user@example.com" &&
                    it.passwordHash == "\$argon2-client" &&
                    it.clientId == "client-123" &&
                    it.verificationSessionId == "session-123" &&
                    !it.platformFlow
        }, Duration.ofMinutes(5))
        1 * emailService.sendTemplateEmail("user@example.com", "Verify your email", "verify-sign-up.ftlh", {
            it.emailAddress == "user@example.com" &&
                    it.verificationUrl.startsWith("https://auth.example.com/two-factor/verify?code=") &&
                    it.logoContentId == "platformLogo" &&
                    it.platformFlow == false
        }, {
            it.platformLogo.filename == "ip-logo.png"
        })
        result.sessionId() == "session-123"
        result.emailAddress() == "user@example.com"
        result.clientId() == "client-123"
        !result.platformFlow()
    }

    def "platform verification creates verified platform user"() {
        given:
        redisStore.consume("two-factor:sign-up:code-123") >> PendingTwoFactorSignUp.builder()
                                                                                    .email("owner@example.com")
                                                                                    .passwordHash("\$argon2-platform")
                                                                                    .verificationSessionId("session-platform")
                                                                                    .platformFlow(true)
                                                                                    .build()

        when:
        def result = service.verifySignUpCode("code-123", true, null)

        then:
        1 * userService.saveVerifiedPlatformUser("owner@example.com", "\$argon2-platform") >> Mock(PlatformUser)
        1 * eventStreamService.storeAndSend("two-factor:verification-status:session-platform",
                                            "/platform/login?message=Email%20verified.%20You%20can%20sign%20in%20now.",
                                            Duration.ofMinutes(5),
                                            "verified")
        result.success()
        result.redirectUrl() == "/platform/login?message=Email%20verified.%20You%20can%20sign%20in%20now."
    }

    def "client verification creates verified client user"() {
        given:
        redisStore.consume("two-factor:sign-up:code-456") >> PendingTwoFactorSignUp.builder()
                                                                                    .email("user@example.com")
                                                                                    .passwordHash("\$argon2-client")
                                                                                    .clientId("client-456")
                                                                                    .verificationSessionId("session-client")
                                                                                    .platformFlow(false)
                                                                                    .build()

        when:
        def result = service.verifySignUpCode("code-456", false, "client-456")

        then:
        1 * userService.saveVerifiedClientUser("user@example.com", "\$argon2-client", "client-456") >> Mock(ClientUser)
        1 * eventStreamService.storeAndSend("two-factor:verification-status:session-client",
                                            {
                                                it.contains("/login") &&
                                                        it.contains("client_id=client-456")
                                            },
                                            Duration.ofMinutes(5),
                                            "verified")
        result.success()
        result.redirectUrl().contains("/login")
        result.redirectUrl().contains("client_id=client-456")
    }

    def "invalid client verification returns login redirect with message"() {
        when:
        def result = service.verifySignUpCode("missing-code", false, "client-456")

        then:
        1 * redisStore.consume("two-factor:sign-up:missing-code") >> null
        !result.success()
        result.redirectUrl().contains("/login")
        result.redirectUrl().contains("client_id=client-456")
        result.redirectUrl().contains("message=")
    }

    def "missing client id returns login redirect with message"() {
        when:
        def result = service.verifySignUpCode("missing-code", false, null)

        then:
        0 * redisStore.consume(_)
        !result.success()
        result.redirectUrl() == "/login?message=This%20client%20verification%20link%20is%20missing%20the%20required%20client%20id.%20Start%20sign%20up%20again%20for%20a%20new%20email."
    }
}
