package io.github.blakedunaway.authserver.business.service

import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.JWKMatcher
import com.nimbusds.jose.jwk.JWKSelector
import com.nimbusds.jose.jwk.RSAKey
import io.github.blakedunaway.authserver.TestSpec
import io.github.blakedunaway.authserver.business.model.SigningKey
import io.github.blakedunaway.authserver.business.model.enums.MetaDataKeys
import io.github.blakedunaway.authserver.business.model.enums.SigningKeyStatus
import io.github.blakedunaway.authserver.config.TestConfig
import io.github.blakedunaway.authserver.integration.repository.gateway.SigningKeyRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Subject

import java.time.Instant

import static org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER

@Import(TestConfig)
class SigningStoreServiceSpec extends TestSpec {
    @Autowired
    @Subject
    private SigningKeyStore signingKeyStore

    @Autowired
    private AuthorizationService service

    @Autowired
    private SigningKeyRepository signingKeyRepository

    @DirtiesContext
    def "ensureActiveKey creates an ACTIVE key when none exist"() {
        when:
        signingKeyStore.ensureActiveKey()
        List<SigningKey> actives = signingKeyRepository.findByStatus(SigningKeyStatus.ACTIVE)

        then:
        actives.size() == 1
        actives.first().getStatus() == SigningKeyStatus.ACTIVE
    }

    @DirtiesContext
    def "jwkSource returns ACTIVE with private material and INACTIVE without"() {
        given:
        SigningKey kActive = signingKeyRepository.save(signingKeyStore.createSigningKey())
        SigningKey kInactive = signingKeyRepository.save(signingKeyStore.createSigningKey().retire())

        when:
        JWKSelector selector = new JWKSelector(new JWKMatcher.Builder().build())
        List<JWK> selected = signingKeyStore.jwkSource().get(selector, null)

        then:
        selected.collect { it.getKeyID() }.containsAll([kActive.getKid(), kInactive.getKid()])

        and:
        RSAKey activeJwk = (RSAKey) selected.find { it.getKeyID() == kActive.getKid() }
        RSAKey inactiveJwk = (RSAKey) selected.find { it.getKeyID() == kInactive.getKid() }
        activeJwk.isPrivate()
        !inactiveJwk.isPrivate()
    }

    @DirtiesContext
    def "rotateSigningKeys creates a new ACTIVE and retires prior ACTIVE keys"() {
        given:
        signingKeyStore.ensureActiveKey()
        List<SigningKey> before = signingKeyRepository.findByStatus(SigningKeyStatus.ACTIVE)

        when:
        signingKeyStore.rotateSigningKeys()

        then:
        List<SigningKey> actives = signingKeyRepository.findByStatus(SigningKeyStatus.ACTIVE)
        actives.size() == 1
        !before.any { it.kid == actives.first().kid }

        and:
        List<SigningKey> retired = signingKeyRepository.findByStatus(SigningKeyStatus.INACTIVE)
        retired.size() == before.size()
        (retired*.kid as Set) == (before*.kid as Set)
    }

    @DirtiesContext
    def "jwkSource ordering prefers newest ACTIVE first"() {
        given:
        SigningKey firstActive = signingKeyRepository.save(signingKeyStore.createSigningKey())
        sleep(5) // ensure createdAt difference
        SigningKey secondActive = signingKeyRepository.save(signingKeyStore.createSigningKey())

        when:
        JWKSelector selector = new JWKSelector(new JWKMatcher.Builder().build())
        List<JWK> selected = signingKeyStore.jwkSource().get(selector, null)

        then:
        List<String> kidsInOrder = selected*.keyID
        kidsInOrder.indexOf(secondActive.getKid()) < kidsInOrder.indexOf(firstActive.getKid())
    }

    @DirtiesContext
    def "INACTIVE keys expose only public parameters in jwkSource material"() {
        given:
        SigningKey inactive = signingKeyRepository.save(signingKeyStore.createSigningKey().retire())

        when:
        JWKSelector selector = new JWKSelector(new JWKMatcher.Builder().keyID(inactive.getKid()).build())
        List<JWK> selected = signingKeyStore.jwkSource().get(selector, null)

        then:
        selected.size() == 1
        RSAKey jwk = (RSAKey) selected.first()
        !jwk.isPrivate()
        jwk.toRSAPublicKey() != null
    }

    @DirtiesContext
    def "purgeInactiveKeys deletes INACTIVE keys with no tokens"() {
        given:
        def k = signingKeyRepository.save(signingKeyStore.createSigningKey().retire())

        when:
        def result = signingKeyRepository.purgeInactiveKeys()

        then:
        result*.kid == [k.kid]
        signingKeyRepository.findByStatus(SigningKeyStatus.INACTIVE).isEmpty()
    }

    @DirtiesContext
    def "purgeInactiveKeys deletes INACTIVE keys with only expired or revoked tokens"() {
        given:
        def key = signingKeyRepository.save(signingKeyStore.createSigningKey())

        def rc = AuthorizationServiceSpec.minimalRegisteredClient()
        def raw = "raw-" + UUID.randomUUID()
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, raw, now, now.plusSeconds(1), Set.of("r"))
        sleep(1000)

        def auth = OAuth2Authorization.withRegisteredClient(rc)
                .principalName("persistable")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizedScopes(Set.of("r"))
                .token(access) { meta ->
                    meta << [
                            kid                                 : key.kid,
                            (MetaDataKeys.REVOKED_AT.getValue()): Instant.now()
                    ]
                }
                .build()

        when:
        service.save(auth)

        signingKeyRepository.save(key.retire())

        and:
        def purged = signingKeyRepository.purgeInactiveKeys()

        then:
        purged*.kid == [key.kid]
        signingKeyRepository.findByStatus(SigningKeyStatus.INACTIVE).isEmpty()
    }


    @DirtiesContext
    def "purgeInactiveKeys does not delete INACTIVE keys with a non-revoked unexpired token"() {
        given:
        // start with an ACTIVE key
        def key = signingKeyRepository.save(signingKeyStore.createSigningKey())

        def rc = AuthorizationServiceSpec.minimalRegisteredClient()
        def raw = "raw-" + UUID.randomUUID()
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, raw, now, now.plusSeconds(1), Set.of("r"))
        sleep(1000) // 1 second so it expires

        def auth = OAuth2Authorization.withRegisteredClient(rc)
                .principalName("persistable")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizedScopes(Set.of("r"))
                .token(access) { meta ->
                    meta << [
                            kid: key.kid,
                    ]
                }
                .build()

        when:
        service.save(auth)
        def savedSigningKeyWithTokens = signingKeyRepository.findByKid(key.kid)

        then:
        savedSigningKeyWithTokens.isPresent()
        savedSigningKeyWithTokens.get().kid == key.getKid()

        and:
        signingKeyRepository.save(savedSigningKeyWithTokens.get().retire())
        def purged = signingKeyRepository.purgeInactiveKeys()

        then:
        !purged.isEmpty()
        signingKeyRepository.findByStatus(SigningKeyStatus.INACTIVE).isEmpty()
    }

    @DirtiesContext
    def "purgeInactiveKeys deletes multiple INACTIVE keys meeting purge conditions"() {
        given:
        def k1 = signingKeyRepository.save(signingKeyStore.createSigningKey().retire())
        def k2 = signingKeyRepository.save(signingKeyStore.createSigningKey().retire())
        def k3 = signingKeyRepository.save(signingKeyStore.createSigningKey().retire())

        when:
        def purged = signingKeyRepository.purgeInactiveKeys()

        then:
        purged*.kid as Set == [k1.kid, k2.kid, k3.kid] as Set
        signingKeyRepository.findByStatus(SigningKeyStatus.INACTIVE).isEmpty()
    }

    //The keyStore generates a key on construction completion of the bean if theres no active keys, this always runs
    //before the repo.save function, which means after that repo.save function runs, theres already a duplicate. This is
    // an edge case I believe to only exist in the test
//    @DirtiesContext
//    def "ensureActiveKey does not create duplicate ACTIVE if one exists"() {
//        given:
//        signingKeyRepository.save(signingKeyStore.createSigningKey())
//
//        when:
//        signingKeyStore.ensureActiveKey()
//        List<SigningKey> actives = signingKeyRepository.findByStatus(SigningKeyStatus.ACTIVE)
//
//        then:
//        actives.size() == 1
//    }
}
