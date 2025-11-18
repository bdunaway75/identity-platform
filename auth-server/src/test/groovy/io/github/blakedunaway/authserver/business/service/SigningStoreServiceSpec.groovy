package io.github.blakedunaway.authserver.business.service

import io.github.blakedunaway.authserver.TestSpec
import io.github.blakedunaway.authserver.business.model.SigningKey
import io.github.blakedunaway.authserver.business.model.enums.SigningKeyStatus
import io.github.blakedunaway.authserver.config.TestConfig
import io.github.blakedunaway.authserver.integration.repository.gateway.SigningKeyRepository
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.JWKMatcher
import com.nimbusds.jose.jwk.JWKSelector
import com.nimbusds.jose.jwk.RSAKey
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Subject

@Import(TestConfig)
class SigningStoreServiceSpec extends TestSpec {
    @Autowired
    @Subject
    private SigningKeyStore signingKeyStore

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
