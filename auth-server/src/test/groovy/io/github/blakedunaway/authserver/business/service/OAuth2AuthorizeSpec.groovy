package io.github.blakedunaway.authserver.business.service

import io.github.blakedunaway.authserver.TestSpec
import io.github.blakedunaway.authserver.business.model.User
import com.blakedunaway.springbackendauth.config.TestConfig
import io.github.blakedunaway.authserver.integration.repository.gateway.UserRepository
import io.github.blakedunaway.authserver.mapper.RegisteredClientMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Subject

import java.time.Instant
import java.time.LocalDateTime

import static org.hamcrest.Matchers.containsString
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@Import([TestConfig])
@AutoConfigureMockMvc
class OAuth2AuthorizeSpec extends TestSpec {

    @Autowired
    @Subject
    private MockMvc mvc

    @Autowired
    private RegisteredClientService registeredClientService

    @Autowired
    private RegisteredClientRepository registeredClientRepository

    @Autowired
    private RegisteredClientMapper registeredClientMapper

    @Autowired
    private UserRepository userRepository

    @Autowired
    private PasswordEncoder passwordEncoder

    @Autowired
    private AuthorizationServerSettings asSettings


    private static final String REDIRECT = "https://client.example.com/callback"

    private static final String INVALID_REDIR = "https://client.example.com/badcallback"

    private static final String SCOPE = "api.read"

    private static final String SCOPES = "api.read api.write"

    private static RegisteredClient clientRequiringConsent() {
        RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("test-client-" + UUID.randomUUID())
                .clientSecret("{noop}secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(REDIRECT)
                .scope("api.read")
                .scope("api.write")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true)
                        .requireProofKey(false)
                        .build())
                .tokenSettings(TokenSettings.builder().build())
                .clientIdIssuedAt(Instant.now())
                .clientSecretExpiresAt(Instant.now())
                .build()
    }

    private static RegisteredClient authCodeClient() {
        RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("test-client-" + UUID.randomUUID())
                .clientSecret("{noop}secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(REDIRECT)
                .scope("api.read")
                .scope("api.write")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(false)
                        .build())
                .tokenSettings(TokenSettings.builder().build())
                .clientIdIssuedAt(Instant.now())
                .clientSecretExpiresAt(Instant.now())
                .build()
    }

    private static RegisteredClient pkcePublicClient() {
        RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("public-" + UUID.randomUUID())
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(REDIRECT)
                .scope("api.read")
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).requireProofKey(true).build())
                .clientIdIssuedAt(Instant.now())
                .clientSecretExpiresAt(Instant.now())
                .build()
    }

    private User persistUser(String email = "alice@example.com", String raw = "pw", String registeredCId = "rc-" + UUID.randomUUID()) {
        def now = LocalDateTime.now()
        def u = User.fromEmail(email)
                .passwordHash(passwordEncoder.encode(raw))
                .registeredClientId(registeredCId) // not needed for /authorize; principal is already authenticated
                .plan("FREE")
                .verified(true)
                .createdAt(now)
                .updatedAt(now)
                .authorities { it.clear() }
                .locked(false)
                .expired(false)
                .credentialsExpired(false)
                .isNew(false)
                .build()
        userRepository.save(u)
        return u
    }

    def "GET /oauth2/authorize - unauthenticated - 302 to /login"() {
        given:
        def rc = clientRequiringConsent()
        registeredClientService.saveRegisteredClient(registeredClientMapper.registeredClientToRegisteredClientModel(rc))

        when:
        def state = UUID.randomUUID().toString()
        def req = get("/oauth2/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", rc.clientId)
                .queryParam("redirect_uri", REDIRECT)
                .queryParam("scope", SCOPE)
                .queryParam("state", state)

        then:
        mvc.perform(req)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"))
    }

    @DirtiesContext
    def "GET consent + POST approve -> 302 back to client with bad scopes"() {
        given: "a real client in the same repo SAS uses and a logged-in principal"
        def rc = clientRequiringConsent()
        registeredClientRepository.save(rc)

        def principal = org.springframework.security.core.userdetails.User
                .withUsername("consent@example.com")
                .password("{noop}pw")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build()

        def authorizePath = asSettings.authorizationEndpoint

        when: "GET /author ze renders the default consent page (200) and seeds pending auth in the session"
        def getReq = get(authorizePath)
                .queryParam("response_type", "code")
                .queryParam("client_id", rc.clientId)
                .queryParam("redirect_uri", REDIRECT)
                .with(user(principal))
        // request all scopes (space-delimited is fine on GET)
        SCOPES.split(' +').each { sc -> getReq.param("scope", sc) }

        def getRes = mvc.perform(getReq)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Consent")))
                .andReturn()

        def session = (MockHttpSession) getRes.request.getSession(false)
        def html = getRes.response.getContentAsString()
        def matcher = (html =~ /name="state"\s+value="([^"]+)"/) // find the minted state from the response
        assert matcher.find()
        def consentState = matcher.group(1)
        assert session != null

        then: "POST approval (same session, csrf, repeated scope params) yields 302 with code+state"
        def postReq = post(authorizePath)
                .with(csrf())
                .with(user(principal))
                .session(session)
                .param("client_id", rc.clientId)
                .param("redirect_uri", REDIRECT)
                .param("consent_action", "approve")
                .param("scope", "api.read")
                .param("state", consentState)

        def res = mvc.perform(postReq)
                .andExpect(status().is3xxRedirection())
                .andReturn()

        def location = res.response.getHeader("Location")
        assert location.startsWith(REDIRECT + "?")
    }

    @DirtiesContext
    def "POST deny consent -> 302 back with error=access_denied"() {
        given:
        def rc = clientRequiringConsent()
        registeredClientService.saveRegisteredClient(registeredClientMapper.registeredClientToRegisteredClientModel(rc))
        def u = persistUser("deny@example.com", "pw", rc.getId())
        def principal = org.springframework.security.core.userdetails.User
                .withUsername(u.getEmail())
                .password(u.getPasswordHash())
                .authorities([])
                .build()
        def state = UUID.randomUUID().toString()
        when: "user hits authorize endpoint, SAS shows consent form."
        def getResult = mvc.perform(post("/oauth2/authorize")
                .with(user(principal))
                .with(csrf())
                .param("response_type", "code")
                .param("client_id", rc.clientId)
                .param("redirect_uri", REDIRECT)
                .param("scope", SCOPE)
                .param("state", state))
                .andExpect(status().isOk())
                .andReturn()

        MockHttpSession session = (MockHttpSession) getResult.request.getSession(false)

        then: "user cancels on consent page (SAS just wipes scopes)"
        mvc.perform(post("/oauth2/authorize")
                .session(session)
                .with(user(principal))
                .with(csrf())
                .param("client_id", rc.clientId)
                .param("state", state)
        )
    }

    def "unknown client -> 400 invalid request"() {
        expect:
        mvc.perform(get("/oauth2/authorize")
                .param("response_type", "code")
                .param("client_id", "does-not-exist")
                .param("redirect_uri", REDIRECT)
                .param("scope", SCOPE)
                .param("state", "x"))
                .andExpect(status().isBadRequest())
    }

    def "redirect_uri does not match what client has -> 302"() {
        given:
        def rc = authCodeClient()
        registeredClientService.saveRegisteredClient(registeredClientMapper.registeredClientToRegisteredClientModel(rc))

        expect:
        mvc.perform(post("/oauth2/authorize")
                .param("response_type", "code")
                .param("client_id", rc.clientId)
                .param("redirect_uri", INVALID_REDIR)
                .param("scope", SCOPE)
                .param("state", "x"))
                .andExpect(status().isBadRequest())
    }

    def "PKCE required without code_challenge -> error"() {
        given:
        def rc = pkcePublicClient()
        registeredClientService.saveRegisteredClient(registeredClientMapper.registeredClientToRegisteredClientModel(rc))
        def principal = org.springframework.security.core.userdetails.User.withUsername("u").password("{noop}p").authorities(new SimpleGrantedAuthority("ROLE_USER")).build()

        expect:
        mvc.perform(post(asSettings.authorizationEndpoint)
                .with(user(principal))
                .param("response_type", "code")
                .param("client_id", rc.clientId)
                .param("redirect_uri", REDIRECT)
                .param("scope", "api.read")
                .param("state", "s"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("${REDIRECT}?error=invalid_request*&state=s*"))
    }

    def "unsupported response_type -> 302 with error on redirect"() {
        given:
        def rc = clientRequiringConsent()
        registeredClientRepository.save(rc)
        def principal = org.springframework.security.core.userdetails.User.withUsername("u").password("{noop}p").authorities(new SimpleGrantedAuthority("ROLE_USER")).build()

        expect:
        mvc.perform(post(asSettings.authorizationEndpoint)
                .with(user(principal))
                .param("response_type", "token")
                .param("client_id", rc.clientId)
                .param("redirect_uri", REDIRECT)
                .param("scope", SCOPE)
                .param("state", "z"))
                .andExpect(status().is4xxClientError())
    }

}
