package io.github.blakedunaway.authserver.business.service

import io.github.blakedunaway.authserver.TestSpec
import io.github.blakedunaway.authserver.config.TestConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent
import org.springframework.test.annotation.DirtiesContext

import static java.util.stream.Collectors.toSet

@Import(TestConfig)
class AuthorizationConsentServiceSpec extends TestSpec {

    @Autowired
    private AuthorizationConsentService service

    private static OAuth2AuthorizationConsent consent(final String rcId,
                                                      final String principal,
                                                      final Collection<String> authorities) {
        def consentBuilder = OAuth2AuthorizationConsent.withId(rcId, principal)
        authorities.each { auths -> consentBuilder.authority(new SimpleGrantedAuthority(auths)) }
        return consentBuilder.build()
    }

    private static Set<String> authNames(final OAuth2AuthorizationConsent consent) {
        new LinkedHashSet<>(consent.getAuthorities().stream().map { it.authority }.collect(toSet()))
    }

    @DirtiesContext
    def "save -> findById persists authorities for user+client"() {
        given:
        def rcId = "rc-${UUID.randomUUID()}"
        def principal = "alice"
        def granted = ["SCOPE_READ", "SCOPE_WRITE"] as LinkedHashSet
        def c = consent(rcId, principal, granted)

        when:
        service.save(c)
        def fromService = service.findById(rcId, principal)

        then:
        fromService != null
        fromService.registeredClientId == rcId
        fromService.principalName == principal
        authNames(fromService).containsAll(granted)
    }

    @DirtiesContext
    def "save is idempotent for same user+client - no duplicate authorities"() {
        given:
        def rcId = "rc-${UUID.randomUUID()}"
        def principal = "idempotent"
        def granted = ["SCOPE_READ"] as Set
        def c = consent(rcId, principal, granted)

        when:
        service.save(c)
        def first = service.findById(rcId, principal)
        service.save(c)
        def second = service.findById(rcId, principal)

        then:
        authNames(first) == granted
        authNames(second) == granted
        authNames(second).size() == 1
    }

    @DirtiesContext
    def "save merges newly approved authorities - progressive consent"() {
        given:
        def rcId = "rc-${UUID.randomUUID()}"
        def principal = "progress"
        def initial = consent(rcId, principal, ["SCOPE_READ"])
        def later = consent(rcId, principal, ["SCOPE_READ", "SCOPE_WRITE"])

        when:
        service.save(initial)
        def afterInitial = service.findById(rcId, principal)
        service.save(later)
        def afterLater = service.findById(rcId, principal)

        then:
        authNames(afterInitial) == ["SCOPE_READ"] as Set
        authNames(afterLater) == ["SCOPE_READ", "SCOPE_WRITE"] as Set
    }

    @DirtiesContext
    def "remove deletes consent for user+client"() {
        given:
        def rcId = "rc-${UUID.randomUUID()}"
        def principal = "carol"
        def c = consent(rcId, principal, ["SCOPE_READ"])

        when:
        service.save(c)
        assert service.findById(rcId, principal) != null
        service.remove(c)
        def gone = service.findById(rcId, principal)

        then:
        gone == null
    }

    @DirtiesContext
    def "round-trip mapping: Spring -> Entity -> Spring remains equal by fields"() {
        given:
        def rcId = "rc-${UUID.randomUUID()}"
        def principal = "roundtrip"
        def scopes = ["SCOPE_EMAIL", "SCOPE_PROFILE"] as Set
        def springObj = consent(rcId, principal, scopes)

        when: "save and read back through service (exercises your adapters)"
        service.save(springObj)
        def readBack = service.findById(rcId, principal)

        then:
        readBack != null
        readBack.registeredClientId == rcId
        readBack.principalName == principal
        authNames(readBack) == scopes
    }
}
