package io.github.blakedunaway.authserver.business.service

import io.github.blakedunaway.authserver.TestSpec
import io.github.blakedunaway.authserver.business.model.RegisteredClientModel
import io.github.blakedunaway.authserver.business.model.SigningKey
import io.github.blakedunaway.authserver.business.model.enums.SigningKeyStatus
import io.github.blakedunaway.authserver.config.redis.RedisStore
import io.github.blakedunaway.authserver.integration.repository.gateway.AuthorizationRepository
import io.github.blakedunaway.authserver.mapper.RegisteredClientMapper
import io.github.blakedunaway.authserver.security.token.TokenHasher
import org.spockframework.spring.SpringBean
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

@Import([AuthorizationService, RegisteredClientService, SigningKeyStore])
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
    private AuthorizationRepository authRepo

    @SpringBean
    private RedisStore redisStore = Mock()

    private String kid;

    private RegisteredClient registeredClient;

    def setup() {
        redisStore.get(_ as String) >> [:]
        kid = UUID.randomUUID()
        signingKeyStore.save(SigningKey.from(UUID.randomUUID().toString())
                                       .kid(kid)
                                       .algorithm("RS256")
                                       .signingKeyStatus(SigningKeyStatus.ACTIVE)
                                       .createdAt(LocalDateTime.now())
                                       .keys(signingKeyStore.generateRsaKey())
                                       .build())

        def rc = minimalRegisteredClient()
        rc = registeredClientService.saveRegisteredClient(rc)
        registeredClient = rc.toOAuth2RegisteredClient()
    }

    @DirtiesContext
    def "save -> findById with ACCESS token persists authorization and scopes"() {
        given:
        def rawAccess = UUID.randomUUID().toString()
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, rawAccess, now, now.plusSeconds(900), Set.of("read", "write"))
        def auth = OAuth2Authorization.withRegisteredClient(registeredClient)
                                      .principalName("alice")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("read", "write"))
                                      .token(access) { meta -> meta.put("kid", kid) }
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
    def "save with kid -> findById with ACCESS token persists authorization and scopes"() {
        given:
        def rawAccess = UUID.randomUUID().toString()
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, rawAccess, now, now.plusSeconds(900), Set.of("read", "write"))
        def auth = OAuth2Authorization.withRegisteredClient(registeredClient)
                                      .principalName("alice")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("read", "write"))
                                      .token(access) { meta -> meta.put("kid", kid) }
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
        def rawAccess = UUID.randomUUID().toString()
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, rawAccess, now, now.plusSeconds(600), Set.of("read"))
        def auth = OAuth2Authorization.withRegisteredClient(registeredClient)
                                      .id(UUID.randomUUID().toString())
                                      .principalName("bob")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("read"))
                                      .token(access, { meta -> meta.put("kid", kid) })
                                      .build()
        service.save(auth)

        when:
        def resolved = service.findByToken(rawAccess, OAuth2TokenType.ACCESS_TOKEN)

        then:
        resolved != null
        resolved.principalName == "bob"
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
        thrown(NoSuchElementException)
    }

    // This is the natural flow of spring. Whenever an Auth is saved, the request ends. If it needs to be fetched again, it will use the token endpoint.
    // Because spring generates its own ID for any auth it touches, if 2 saves occurred during the same request, the save would fail because the first
    // save we internally nulled the auths id, the next save we characterize the auth as new again, because its using the same ID that was generated
    // before the first save.
    @DirtiesContext
    def "save is idempotent - calling twice does not double-hash or duplicate tokens"() {
        given:
        def raw = "raw-" + UUID.randomUUID()
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, raw, now, now.plusSeconds(600), Set.of("read"))
        def auth = OAuth2Authorization.withRegisteredClient(registeredClient)
                                      .principalName("idempotent")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("read"))
                                      .token(access) { meta -> meta.put("kid", kid) }
                                      .build()

        when:
        service.save(auth)
        def first = authRepo.findAll().first()
        def firstTokenHashes = first.tokens.collect { it.hashedTokenValue } as Set

        and:
        auth = service.findByToken(raw, null)
        service.save(auth)
        def second = authRepo.findAll().first()
        def secondTokenHashes = second.tokens.collect { it.hashedTokenValue } as Set

        then:
        firstTokenHashes == secondTokenHashes
        second.tokens.size() == 1
    }

    @DirtiesContext
    def "findById attaches all tokens hashed - no raw leakage"() {
        given:
        def raw = "raw-" + UUID.randomUUID()
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, raw, now, now.plusSeconds(600), Set.of("r"))
        def auth = OAuth2Authorization.withRegisteredClient(registeredClient)
                                      .principalName("hashed-check")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("r"))
                                      .token(access) { meta -> meta.put("kid", kid) }
                                      .build()
        service.save(auth)

        when:
        def persisted = authRepo.findAll().first()
        def resolved = service.findById(persisted.id.toString())

        then:
        resolved != null
        resolved.accessToken != null
        resolved.accessToken.token.tokenValue != raw
    }

    @DirtiesContext
    def "findByToken returns auth with ONLY matched token carrying raw value"() {
        given:
        def raw = "raw-" + UUID.randomUUID()
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, raw, now, now.plusSeconds(600), Set.of("read"))
        def auth = OAuth2Authorization.withRegisteredClient(registeredClient)
                                      .principalName("bytoken")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("read"))
                                      .token(access) { meta -> meta.put("kid", kid) }
                                      .build()
        service.save(auth)

        when:
        def resolved = service.findByToken(raw, OAuth2TokenType.ACCESS_TOKEN)

        then:
        resolved != null
        resolved.accessToken != null
        resolved.accessToken.token.tokenValue == TokenHasher.hmacCurrent(raw)
    }

    @DirtiesContext
    def "save with rotated refresh removes old refresh - no zombie tokens"() {
        given:
        def now = Instant.now()
        def refresh1 = new OAuth2RefreshToken("r1-" + UUID.randomUUID(), now, now.plusSeconds(600))
        def auth1 = OAuth2Authorization.withRegisteredClient(registeredClient)
                                       .principalName("rotate")
                                       .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                       .authorizedScopes(Set.of("r"))
                                       .token(refresh1) { meta -> meta.put("kid", kid) }
                                       .build()
        service.save(auth1)
        def before = authRepo.findAll().first()
        def oldHash = before.tokens.first().hashedTokenValue

        and:
        def refresh2 = new OAuth2RefreshToken("r2-" + UUID.randomUUID(), now, now.plusSeconds(600))
        def auth2 = OAuth2Authorization.withRegisteredClient(registeredClient)
                                       .id(before.id.toString())
                                       .principalName("rotate")
                                       .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                       .authorizedScopes(Set.of("r"))
                                       .token(refresh2) { meta -> meta.put("kid", kid) }
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
    def "update path normalizes raw inputs without altering already hashed values"() {
        given:
        def now = Instant.now()
        def raw1 = "raw-" + UUID.randomUUID()
        def access1 = new OAuth2AccessToken(BEARER, raw1, now, now.plusSeconds(600), Set.of("r"))
        def auth1 = OAuth2Authorization.withRegisteredClient(registeredClient)
                                       .principalName("norm")
                                       .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                       .authorizedScopes(Set.of("r"))
                                       .token(access1) { meta -> meta.put("kid", kid) }
                                       .build()
        service.save(auth1)
        def persisted = authRepo.findAll().get(0)
        def hash1 = persisted.tokens.getAt(0).hashedTokenValue;

        and:
        def raw2 = "raw-" + UUID.randomUUID()
        def access2 = new OAuth2RefreshToken(raw2, now, now.plusSeconds(600))
        def auth2 = OAuth2Authorization.withRegisteredClient(registeredClient)
                                       .id(persisted.id.toString())
                                       .principalName("norm")
                                       .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                       .authorizedScopes(Set.of("r"))
                                       .token(new OAuth2AccessToken(BEARER, hash1, now, now.plusSeconds(600), Set.of("r"))) {
                                           meta -> meta.put("kid", kid)
                                       }
                                       .token(access2) { meta -> meta.put("kid", kid) }
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
    def "ID token claims and subject persist"() {
        given:
        def now = Instant.now()
        def idTok = new OidcIdToken(
                "id-" + UUID.randomUUID(), now, now.plusSeconds(300), Map.of("sub", "user-123")
        )
        def auth = OAuth2Authorization.withRegisteredClient(registeredClient)
                                      .principalName("oidc")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("openid"))
                                      .token(idTok) { meta -> meta.put("kid", kid) }
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
        def raw = "raw-" + UUID.randomUUID()
        def now = Instant.now()
        def access = new OAuth2AccessToken(BEARER, raw, now, now.plusSeconds(600), Set.of("r"))
        def auth = OAuth2Authorization.withRegisteredClient(registeredClient)
                                      .principalName("remove")
                                      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                      .authorizedScopes(Set.of("r"))
                                      .token(access) { meta -> meta.put("kid", kid) }
                                      .build()
        service.save(auth)
        assert authRepo.findAll().size() == 1

        when:
        auth = service.findByToken(TokenHasher.hmacCurrent(raw), null)
        service.remove(auth)

        then:
        authRepo.findAll().isEmpty()
    }

    static RegisteredClientModel minimalRegisteredClient() {
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
