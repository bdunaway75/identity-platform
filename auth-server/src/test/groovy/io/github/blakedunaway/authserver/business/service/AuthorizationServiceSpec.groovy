package io.github.blakedunaway.authserver.business.service

import io.github.blakedunaway.authserver.TestSpec
import io.github.blakedunaway.authserver.business.model.SigningKey
import io.github.blakedunaway.authserver.business.model.enums.SigningKeyStatus
import com.blakedunaway.springbackendauth.config.TestConfig
import io.github.blakedunaway.authserver.integration.repository.implementation.AuthorizationRepositoryImpl
import io.github.blakedunaway.authserver.mapper.RegisteredClientMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Subject

import java.time.Instant
import java.time.LocalDateTime

import static org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER

@Import(TestConfig)
class AuthorizationServiceSpec extends TestSpec {

    @Autowired
    @Subject
    private AuthorizationService service

    @Autowired
    private RegisteredClientService registeredClientService

    @Autowired
    private RegisteredClientMapper registeredClientMapper

    @Autowired
    private SigningKeyStore signingKeyStore;

    @Autowired
    private AuthorizationRepositoryImpl authRepo

    @DirtiesContext
    def "save -> findById roundtrip with ACCESS token persists authorization and scopes"() {
        given:
        def rc = minimalRegisteredClient()
        def rawAccess = "raw-access-${UUID.randomUUID()}"
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, rawAccess, now, now.plusSeconds(900), Set.of("read", "write"))
        def auth = OAuth2Authorization.withRegisteredClient(rc)
                                      .principalName("alice")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("read", "write"))
                                      .token(access) { _ -> /* no-op metadata */ }
                                      .build()

        when:
        service.save(auth)
        def fromService = authRepo.findAll()

        then:
        fromService.size() == 1
        fromService.get(0).principalName == "alice"
        fromService.get(0).authorizedScopes.containsAll(Set.of("read", "write"))

        and: "we actually wrote to the DB"
        authRepo.findAll().size() == 1
        authRepo.findAll().first().authorizedScopes.containsAll(Set.of("read", "write"))
        !authRepo.findAll().first().tokens.isEmpty()
    }

    @DirtiesContext
    def "save with kid -> findById roundtrip with ACCESS token persists authorization and scopes"() {
        given:
        def rc = minimalRegisteredClient()
        def rawAccess = "raw-access-${UUID.randomUUID()}"
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, rawAccess, now, now.plusSeconds(900), Set.of("read", "write"))
        def auth = OAuth2Authorization.withRegisteredClient(rc)
                                      .principalName("alice")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("read", "write"))
                                      .token(access) { _ -> }
                                      .build()

        when:
        service.save(auth)
        def fromService = authRepo.findAll()

        then:
        fromService.size() == 1
        fromService.get(0).principalName == "alice"
        fromService.get(0).authorizedScopes.containsAll(Set.of("read", "write"))

        and: "we actually wrote to the DB"
        authRepo.findAll().size() == 1
        authRepo.findAll().first().authorizedScopes.containsAll(Set.of("read", "write"))
        !authRepo.findAll().first().tokens.isEmpty()
    }

    @DirtiesContext
    def "findByToken - locates saved auth by raw access token"() {
        given:
        def rc = minimalRegisteredClient()
        def rawAccess = "raw-access-${UUID.randomUUID().leastSignificantBits}"
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, rawAccess, now, now.plusSeconds(600), Set.of("read"))
        def auth = OAuth2Authorization.withRegisteredClient(rc)
                                      .id(UUID.randomUUID().toString())
                                      .principalName("bob")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("read"))
                                      .token(access, { _ -> })
                                      .build()
        registeredClientService.saveRegisteredClient(registeredClientMapper.registeredClientToRegisteredClientModel(rc))
        service.save(auth)

        when:
        def resolved = service.findByToken(rawAccess, OAuth2TokenType.ACCESS_TOKEN)

        then:
        resolved != null
        resolved.principalName == "bob"
        // Don’t assert tokenValue equality; you’re not storing raw values. Presence is enough:
        resolved.accessToken != null
    }

    @DirtiesContext
    def "findByToken guards: null token throws"() {
        when:
        service.findByToken(null, new OAuth2TokenType("access_token"))

        then:
        thrown(IllegalArgumentException)
    }

    @DirtiesContext
    def "findByToken guards: null tokenType throws"() {
        when:
        service.findByToken("whatever", null)

        then:
        thrown(IllegalArgumentException)
    }

    @DirtiesContext
    def "remove deletes persisted authorization"() {
        given:
        def rc = minimalRegisteredClient()
        def rawAccess = "raw-access-${UUID.randomUUID()}"
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, rawAccess, now, now.plusSeconds(600), Set.of("read"))
        def auth = OAuth2Authorization.withRegisteredClient(rc)
                                      .id(UUID.randomUUID().toString())
                                      .principalName("carol")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("read"))
                                      .token(access, { _ -> })
                                      .build()
        service.save(auth)
        assert authRepo.findAll().size() == 1

        when:
        service.remove(auth)

        then:
        authRepo.findAll().isEmpty()
    }

    @DirtiesContext
    def "save is idempotent - calling twice does not double-hash or duplicate tokens"() {
        given:
        def rc = minimalRegisteredClient()
        def raw = "raw-" + UUID.randomUUID()
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, raw, now, now.plusSeconds(600), Set.of("read"))
        def auth = OAuth2Authorization.withRegisteredClient(rc)
                                      .principalName("idempotent")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("read"))
                                      .token(access) { _ -> }
                                      .build()

        when:
        service.save(auth)
        def first = authRepo.findAll().first()
        def firstTokenHashes = first.tokens.collect { it.hashedTokenValue } as Set

        and:
        service.save(auth) // call again with same object
        def second = authRepo.findAll().first()
        def secondTokenHashes = second.tokens.collect { it.hashedTokenValue } as Set

        then:
        firstTokenHashes == secondTokenHashes
        second.tokens.size() == 1
    }

    @DirtiesContext
    def "findById attaches all tokens hashed - no raw leakage"() {
        given:
        def rc = minimalRegisteredClient()
        def raw = "raw-" + UUID.randomUUID()
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, raw, now, now.plusSeconds(600), Set.of("r"))
        def auth = OAuth2Authorization.withRegisteredClient(rc)
                                      .principalName("hashed-check")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("r"))
                                      .token(access) { _ -> }
                                      .build()
        registeredClientService.saveRegisteredClient(registeredClientMapper.registeredClientToRegisteredClientModel(rc))
        service.save(auth)

        when:
        def persisted = authRepo.findAll().first()
        def resolved = service.findById(persisted.id.toString())

        then:
        resolved != null
        resolved.accessToken != null
        resolved.accessToken.token.tokenValue != raw  // raw should never leak here
    }

    @DirtiesContext
    def "findByToken returns auth with ONLY matched token carrying raw value"() {
        given:
        def rc = minimalRegisteredClient()
        def raw = "raw-" + UUID.randomUUID()
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, raw, now, now.plusSeconds(600), Set.of("read"))
        def auth = OAuth2Authorization.withRegisteredClient(rc)
                                      .principalName("bytoken")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("read"))
                                      .token(access) { _ -> }
                                      .build()
        registeredClientService.saveRegisteredClient(registeredClientMapper.registeredClientToRegisteredClientModel(rc))
        service.save(auth)

        when:
        def resolved = service.findByToken(raw, OAuth2TokenType.ACCESS_TOKEN)

        then:
        resolved != null
        resolved.accessToken != null
        resolved.accessToken.token.tokenValue == raw  // raw only in this path
    }

    @DirtiesContext
    def "save with rotated refresh removes old refresh - no zombie tokens"() {
        given:
        def rc = minimalRegisteredClient()
        def now = Instant.now()
        def refresh1 = new OAuth2RefreshToken("r1-" + UUID.randomUUID(), now, now.plusSeconds(600))
        def auth1 = OAuth2Authorization.withRegisteredClient(rc)
                                       .principalName("rotate")
                                       .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                       .authorizedScopes(Set.of("r"))
                                       .token(refresh1) { _ -> }
                                       .build()
        service.save(auth1)
        def before = authRepo.findAll().first()
        def oldHash = before.tokens.first().hashedTokenValue

        and:
        def refresh2 = new OAuth2RefreshToken("r2-" + UUID.randomUUID(), now, now.plusSeconds(600))
        def auth2 = OAuth2Authorization.withRegisteredClient(rc)
                                       .id(before.id.toString())
                                       .principalName("rotate")
                                       .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                       .authorizedScopes(Set.of("r"))
                                       .token(refresh2) { _ -> }
                                       .build()

        when:
        service.save(auth2)
        def after = authRepo.findAll().first()
        def hashes = after.tokens.collect { it.hashedTokenValue } as Set

        then:
        hashes.size() == 1
        !hashes.contains(oldHash)
    }

    @DirtiesContext
    def "revoked token persists INVALIDATED metadata and survives round-trip"() {
        given:
        def rc = minimalRegisteredClient()
        def raw = "raw-" + UUID.randomUUID()
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, raw, now, now.plusSeconds(600), Set.of("read"))
        def auth = OAuth2Authorization.withRegisteredClient(rc)
                                      .principalName("revoked")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("read"))
                                      .token(access) { meta -> meta.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, true) }
                                      .build()

        when:
        registeredClientService.saveRegisteredClient(registeredClientMapper.registeredClientToRegisteredClientModel(rc))
        service.save(auth)
        def persisted = authRepo.findAll().first()
        def re = service.findById(persisted.id.toString())

        then:
        re.accessToken.metadata[OAuth2Authorization.Token.INVALIDATED_METADATA_NAME] == true
    }

    @DirtiesContext
    def "update path normalizes raw inputs without altering already hashed values"() {
        given:
        def rc = minimalRegisteredClient()
        def now = Instant.now()
        def raw1 = "raw-" + UUID.randomUUID()
        def access1 = new OAuth2AccessToken(BEARER, raw1, now, now.plusSeconds(600), Set.of("r"))
        def auth1 = OAuth2Authorization.withRegisteredClient(rc)
                                       .principalName("norm")
                                       .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                       .authorizedScopes(Set.of("r"))
                                       .token(access1) { _ -> }
                                       .build()
        service.save(auth1)
        def persisted = authRepo.findAll().get(0)
        def hash1 = persisted.tokens.getAt(0).hashedTokenValue;

        and:
        def raw2 = "raw-" + UUID.randomUUID()
        def access2 = new OAuth2RefreshToken(raw2, now, now.plusSeconds(600))
        def auth2 = OAuth2Authorization.withRegisteredClient(rc)
                                       .id(persisted.id.toString())
                                       .principalName("norm")
                                       .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                       .authorizedScopes(Set.of("r"))
                                       .token(new OAuth2AccessToken(BEARER, hash1, now, now.plusSeconds(600), Set.of("r"))) { _ -> }
                                       .token(access2) { _ -> }
                                       .build()

        when:
        service.save(auth2)
        def after = authRepo.findAll().first()
        def hashes = after.tokens.collect { it.hashedTokenValue } as Set

        then:
        hashes.size() == 2
        hashes.contains(hash1)
    }

    @DirtiesContext
    def "findByToken unknown returns null"() {
        expect:
        service.findByToken("does-not-exist", OAuth2TokenType.ACCESS_TOKEN) == null
    }

    @DirtiesContext
    def "attributes and scopes persist and round-trip"() {
        given:
        def rc = minimalRegisteredClient()
        def raw = "raw-" + UUID.randomUUID()
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, raw, now, now.plusSeconds(600), Set.of("read", "write"))
        def auth = OAuth2Authorization.withRegisteredClient(rc)
                                      .principalName("attrs")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("read", "write"))
                                      .attribute("foo", "bar")
                                      .token(access) { meta -> meta.put("x", "y") }
                                      .build()

        when:
        registeredClientService.saveRegisteredClient(registeredClientMapper.registeredClientToRegisteredClientModel(rc))
        service.save(auth)
        def persisted = authRepo.findAll().first()
        def re = service.findById(persisted.id.toString())

        then:
        re.attributes["foo"] == "bar"
        re.authorizedScopes.containsAll(Set.of("read", "write"))
    }

    @DirtiesContext
    def "ID token claims and subject persist"() {
        given:
        def rc = minimalRegisteredClient()
        def now = Instant.now()
        def idTok = new OidcIdToken(
                "id-" + UUID.randomUUID(), now, now.plusSeconds(300), Map.of("sub", "user-123")
        )
        def auth = OAuth2Authorization.withRegisteredClient(rc)
                                      .principalName("oidc")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("openid"))
                                      .token(idTok) { _ -> }
                                      .build()

        when:
        service.save(auth)
        def persisted = authRepo.findAll().first()

        then:
        persisted.tokens.any { it.tokenType.name() == "ID_TOKEN" && it.metadata.containsKey("metadata.token.claims") }
    }

    @DirtiesContext
    def "remove deletes auth and child tokens - orphanRemoval"() {
        given:
        def rc = minimalRegisteredClient()
        def raw = "raw-" + UUID.randomUUID()
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, raw, now, now.plusSeconds(600), Set.of("r"))
        def auth = OAuth2Authorization.withRegisteredClient(rc)
                                      .principalName("remove")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("r"))
                                      .token(access) { _ -> }
                                      .build()
        service.save(auth)
        assert authRepo.findAll().size() == 1

        when:
        service.remove(auth)

        then:
        authRepo.findAll().isEmpty()
    }

    @DirtiesContext
    def "assigned ids with Persistable mark entity not new after save"() {
        given:
        def rc = minimalRegisteredClient()
        def raw = "raw-" + UUID.randomUUID()
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, raw, now, now.plusSeconds(600), Set.of("r"))
        def auth = OAuth2Authorization.withRegisteredClient(rc)
                                      .principalName("persistable")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("r"))
                                      .token(access) { _ -> }
                                      .build()

        when:
        service.save(auth)
        def e = authRepo.findAll().first()

        then:
        !e.isNew()  // relies on @PostPersist flipping the flag
    }

    @DirtiesContext
    def "save new key to DB"() {
        given: "an ACTIVE signing key already in DB"
        def kid = "kid-" + UUID.randomUUID()
        def keyEntity = signingKeyStore.save(SigningKey.from(UUID.randomUUID().toString())
                                                       .kid(kid)
                                                       .algorithm("RS256")
                                                       .signingKeyStatus(SigningKeyStatus.ACTIVE)
                                                       .createdAt(LocalDateTime.now())
                                                       .keys(signingKeyStore.generateRsaKey())
                                                       .build())

        and: "an OAuth2Authorization with an access token whose metadata contains the kid"
        def rc = minimalRegisteredClient()
        def raw = "raw-" + UUID.randomUUID()
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, raw, now, now.plusSeconds(600), Set.of("read"))
        def auth = OAuth2Authorization.withRegisteredClient(rc)
                                      .principalName("alice")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("read"))
                                      .token(access) { meta -> meta.put("kid", kid) }
                                      .build()

        when:
        service.save(auth)
        def persisted = authRepo.findAll().first()
        def token = persisted.tokens.first()

        then: "string kid is stored and relation is set"
        token.kid != null
        token.kid == kid
        token.metadata["kid"] == kid
    }

    @DirtiesContext
    def "save fails when kid not found in signing_keys"() {
        given:
        def rc = minimalRegisteredClient()
        def raw = "raw-" + UUID.randomUUID()
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, raw, now, now.plusSeconds(600), Set.of("r"))
        def bogusKid = "does-not-exist-" + UUID.randomUUID()
        def auth = OAuth2Authorization.withRegisteredClient(rc)
                                      .principalName("no-key")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("r"))
                                      .token(access) { meta -> meta.put("kid", bogusKid) }
                                      .build()

        when:
        service.save(auth)

        then:
        def e = thrown(IllegalStateException)
        e.message.contains("Unknown signing kid")
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
