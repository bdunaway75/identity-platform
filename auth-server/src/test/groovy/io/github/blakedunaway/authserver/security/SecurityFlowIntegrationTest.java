package io.github.blakedunaway.authserver.security;

import com.stripe.StripeClient;
import io.github.blakedunaway.authserver.business.api.dto.RegisteredClientRequest;
import io.github.blakedunaway.authserver.business.model.Authority;
import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import io.github.blakedunaway.authserver.business.model.enums.MetaDataKeys;
import io.github.blakedunaway.authserver.business.model.user.ClientUser;
import io.github.blakedunaway.authserver.business.model.user.PlatformUser;
import io.github.blakedunaway.authserver.business.service.UserService;
import io.github.blakedunaway.authserver.config.app.Application;
import io.github.blakedunaway.authserver.config.redis.RedisStore;
import io.github.blakedunaway.authserver.integration.entity.DemoAccessCodeEntity;
import io.github.blakedunaway.authserver.integration.entity.PlatformUserEntity;
import io.github.blakedunaway.authserver.integration.entity.PlatformUserTierEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientScopeEntity;
import io.github.blakedunaway.authserver.integration.repository.jpa.DemoAccessCodeJpaRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.PlatformUserJpaRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.PlatformUserTierJpaRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.RegisterClientJpaRepository;
import io.github.blakedunaway.authserver.integration.repository.jpa.RegisteredClientScopeJpaRepository;
import io.github.blakedunaway.authserver.mapper.RegisteredClientMapper;
import io.github.blakedunaway.authserver.mapper.TokenSettingsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
public class SecurityFlowIntegrationTest {

    private static final String PLATFORM_CLIENT_ID = "identity-platform";
    private static final String FRONTEND_CLIENT_ID = "test-frontend-client";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private RegisteredClientMapper registeredClientMapper;

    @Autowired
    private TokenSettingsMapper tokenSettingsMapper;

    @Autowired
    private RegisterClientJpaRepository registerClientJpaRepository;

    @Autowired
    private RegisteredClientScopeJpaRepository registeredClientScopeJpaRepository;

    @Autowired
    private PlatformUserTierJpaRepository platformUserTierJpaRepository;

    @Autowired
    private PlatformUserJpaRepository platformUserJpaRepository;

    @Autowired
    private DemoAccessCodeJpaRepository demoAccessCodeJpaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private RedisStore redisStore;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @MockitoBean
    private StripeClient stripeClient;

    private final Map<String, Object> redisValues = new HashMap<>();

    @BeforeEach
    void setUpRedisStore() {
        redisValues.clear();
        when(redisStore.get(anyString())).thenAnswer(invocation -> redisValues.get(invocation.getArgument(0, String.class)));
        doAnswer(invocation -> {
            redisValues.put(invocation.getArgument(0, String.class), invocation.getArgument(1));
            return null;
        }).when(redisStore).put(anyString(), any(), any());
        doAnswer(invocation -> redisValues.remove(invocation.getArgument(0, String.class))).when(redisStore).consume(anyString());
        ensureFrontendClientExists();
        ensureBasicTierExists();
    }

    @Test
    void platformEndpointRequiresBearerToken() throws Exception {
        mockMvc.perform(get("/platform/api/dashboard"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void platformEndpointRejectsJwtIssuedForDifferentClient() throws Exception {
        final String email = uniqueEmail("platform-azp");
        savePlatformUser(email, "Password123!");

        mockMvc.perform(get("/platform/api/dashboard")
                                .header("Authorization", bearerToken(email, "different-client", List.of("ROLE_PLATFORM_USER"))))
               .andExpect(status().isForbidden());
    }

    @Test
    void platformEndpointRejectsJwtWithoutPlatformRole() throws Exception {
        final String email = uniqueEmail("platform-no-role");
        savePlatformUser(email, "Password123!");

        mockMvc.perform(get("/platform/api/dashboard")
                                .header("Authorization", bearerToken(email, PLATFORM_CLIENT_ID, List.of("ROLE_USER"))))
               .andExpect(status().isForbidden());
    }

    @Test
    void platformEndpointAllowsPlatformJwtWithPlatformRole() throws Exception {
        final String email = uniqueEmail("platform-ok");
        savePlatformUser(email, "Password123!");

        mockMvc.perform(get("/platform/api/dashboard")
                                .header("Authorization", bearerToken(email, PLATFORM_CLIENT_ID, List.of("ROLE_PLATFORM_USER"))))
                .andExpect(status().isOk())
               .andExpect(jsonPath("$.tier.name").value("BASIC"));
    }

    @Test
    void platformClientAuthorizationRoutesToPlatformLogin() throws Exception {
        final String redirectUri = "http://localhost:9001/callback";
        savePublicAuthorizationCodeClientWithExplicitClientId(
                PLATFORM_CLIENT_ID,
                redirectUri,
                Set.of("read"),
                true
        );

        final String codeVerifier = "platform-verifier-12345678901234567890123456789012345678901234567890";
        final String codeChallenge = sha256Base64Url(codeVerifier);
        final MultiValueMap<String, String> authorizeParams = new LinkedMultiValueMap<>();
        authorizeParams.add("response_type", "code");
        authorizeParams.add("client_id", PLATFORM_CLIENT_ID);
        authorizeParams.add("redirect_uri", redirectUri);
        authorizeParams.add("scope", "read");
        authorizeParams.add("state", "platform-state");
        authorizeParams.add("code_challenge", codeChallenge);
        authorizeParams.add("code_challenge_method", "S256");

        final MvcResult authorizeRedirect = mockMvc.perform(get(
                                                            UriComponentsBuilder.fromPath("/oauth2/authorize")
                                                                                .queryParams(authorizeParams)
                                                                                .build(true)
                                                                                .toUriString())
                                                            .accept(MediaType.TEXT_HTML))
                                                 .andExpect(status().is3xxRedirection())
                                                 .andExpect(header().string("Location", endsWith("/login")))
                                                 .andReturn();

        final MockHttpSession session = (MockHttpSession) authorizeRedirect.getRequest().getSession(false);

        mockMvc.perform(get("/login").session(session))
               .andExpect(status().is3xxRedirection())
               .andExpect(header().string("Location", endsWith("/platform/login")));
    }

    @Test
    void platformLoginWithExpiredCredentialsRedirectsToCredentialsExpiredPage() throws Exception {
        final String email = uniqueEmail("platform-cred-expired");
        final String password = "Password123!";

        final PlatformUser platformUser = PlatformUser.from(email)
                                                      .email(email)
                                                      .passwordHash(passwordEncoder.encode(password))
                                                      .verified(true)
                                                      .createdAt(LocalDateTime.now())
                                                      .updatedAt(LocalDateTime.now())
                                                      .registeredClientIds(ids -> ids.clear())
                                                      .authorities(authorities -> authorities.add(Authority.from("ROLE_PLATFORM_USER")))
                                                      .locked(false)
                                                      .expired(false)
                                                      .credentialsExpired(true)
                                                      .build();
        userService.savePlatformUser(platformUser);

        final MvcResult loginRedirect = mockMvc.perform(post("/platform/login")
                                                                .with(csrf())
                                                                .param("email", email)
                                                                .param("password", password))
                                               .andExpect(status().is3xxRedirection())
                                               .andReturn();

        final String credentialsRedirectLocation = loginRedirect.getResponse().getHeader("Location");
        final UriComponents credentialsUri = UriComponentsBuilder.fromUriString(credentialsRedirectLocation)
                                                                 .build();

        assertThat(credentialsUri.getPath()).isEqualTo("/platform/credentials-expired");
        assertThat(credentialsUri.getQueryParams().getFirst("email")).isEqualTo(email);
        assertThat(credentialsRedirectLocation)
                .contains("error=Your%20password%20has%20expired.%20Choose%20a%20new%20one%20to%20continue.");
    }

    @Test
    void clientLoginWithExpiredCredentialsCanResetPasswordAndSignInAgain() throws Exception {
        final String redirectUri = "http://localhost:9301/callback";
        final String email = uniqueEmail("client-cred-expired");
        final String oldPassword = "Password123!";
        final String newPassword = "UpdatedPassword123!";
        final String codeVerifier = "expired-reset-verifier-1234567890123456789012345678901234567890123";

        final RegisteredClientFixture registeredClient =
                savePublicAuthorizationCodeClient(redirectUri, Set.of("read"), true);
        final String clientId = registeredClient.registeredClient().getClientId();

        final ClientUser clientUser = ClientUser.from(email)
                                                .email(email)
                                                .clientId(clientId)
                                                .passwordHash(passwordEncoder.encode(oldPassword))
                                                .verified(true)
                                                .createdAt(LocalDateTime.now())
                                                .updatedAt(LocalDateTime.now())
                                                .userAttributes(new HashMap<>())
                                                .authorities(authorities -> authorities.add(Authority.from("ROLE_USER")))
                                                .locked(false)
                                                .expired(false)
                                                .credentialsExpired(true)
                                                .build();
        userService.saveUser(clientUser);

        final MultiValueMap<String, String> authorizeParams = new LinkedMultiValueMap<>();
        authorizeParams.add("response_type", "code");
        authorizeParams.add("client_id", clientId);
        authorizeParams.add("redirect_uri", redirectUri);
        authorizeParams.add("scope", "read");
        authorizeParams.add("state", UUID.randomUUID().toString());
        authorizeParams.add(MetaDataKeys.CODE_CHALLENGE.getValue(), sha256Base64Url(codeVerifier));
        authorizeParams.add(MetaDataKeys.CODE_CHALLENGE_METHOD.getValue(), "S256");

        final MvcResult authorizeRedirect = mockMvc.perform(get(
                                                            UriComponentsBuilder.fromPath("/oauth2/authorize")
                                                                                .queryParams(authorizeParams)
                                                                                .build(true)
                                                                                .toUriString())
                                                            .accept(MediaType.TEXT_HTML))
                                                   .andExpect(status().is3xxRedirection())
                                                   .andExpect(header().string("Location", endsWith("/login")))
                                                   .andReturn();

        final MockHttpSession session = (MockHttpSession) authorizeRedirect.getRequest().getSession(false);

        final MvcResult loginRedirect = mockMvc.perform(post("/login")
                                                                .session(session)
                                                                .with(csrf())
                                                                .param("email", email)
                                                                .param("password", oldPassword)
                                                                .param("clientId", clientId))
                                               .andExpect(status().is3xxRedirection())
                                               .andReturn();

        final String credentialsRedirectLocation = loginRedirect.getResponse().getHeader("Location");
        final UriComponents credentialsUri = UriComponentsBuilder.fromUriString(credentialsRedirectLocation)
                                                                 .build();
        assertThat(credentialsUri.getPath()).isEqualTo("/credentials-expired");
        assertThat(credentialsUri.getQueryParams().getFirst("email")).isEqualTo(email);
        assertThat(credentialsUri.getQueryParams().getFirst("clientId")).isEqualTo(clientId);
        assertThat(credentialsRedirectLocation)
                .contains("error=Your%20password%20has%20expired.%20Choose%20a%20new%20one%20to%20continue.");

        final MvcResult passwordResetRedirect = mockMvc.perform(post("/credentials-expired")
                                                                        .with(csrf())
                                                                        .param("email", email)
                                                                        .param("clientId", clientId)
                                                                        .param("currentPassword", oldPassword)
                                                                        .param("newPassword", newPassword)
                                                                        .param("confirmPassword", newPassword))
                                                       .andExpect(status().is3xxRedirection())
                                                       .andReturn();

        final String loginRedirectLocation = passwordResetRedirect.getResponse().getHeader("Location");
        final UriComponents loginUri = UriComponentsBuilder.fromUriString(loginRedirectLocation)
                                                           .build();
        assertThat(loginUri.getPath()).isEqualTo("/login");
        assertThat(loginUri.getQueryParams().getFirst("client_id")).isEqualTo(clientId);
        assertThat(loginRedirectLocation)
                .contains("message=Password%20updated%20successfully.%20Please%20sign%20in%20with%20your%20new%20password.");

        final String authorizationCode = authorizeClientUserAndCaptureCode(
                clientId,
                redirectUri,
                email,
                newPassword,
                Set.of("read"),
                codeVerifier
        );

        assertThat(authorizationCode).isNotBlank();
    }

    @Test
    @Transactional
    void demoAccessCodeAuthorizationFlowSucceeds() throws Exception {
        final String email = uniqueEmail("platform-demo");
        final String demoCode = "DEMO" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        final String redirectUri = "http://localhost:9999/frontend/callback";
        final String codeVerifier = "demo-verifier-12345678901234567890123456789012345678901234567890";
        savePlatformUser(email, "Password123!");
        final PlatformUserEntity platformUserEntity = platformUserJpaRepository.findByEmailIgnoreCase(email).orElseThrow();
        demoAccessCodeJpaRepository.save(new DemoAccessCodeEntity(
                null,
                demoCode,
                platformUserEntity,
                false
        ));

        final MultiValueMap<String, String> authorizeParams = new LinkedMultiValueMap<>();
        authorizeParams.add("response_type", "code");
        authorizeParams.add("client_id", FRONTEND_CLIENT_ID);
        authorizeParams.add("redirect_uri", redirectUri);
        authorizeParams.add("scope", "openid");
        authorizeParams.add("state", UUID.randomUUID().toString());
        authorizeParams.add(MetaDataKeys.CODE_CHALLENGE.getValue(), sha256Base64Url(codeVerifier));
        authorizeParams.add(MetaDataKeys.CODE_CHALLENGE_METHOD.getValue(), "S256");

        final MvcResult demoRedirect = mockMvc.perform(post("/platform/demo-access-code")
                                                               .with(csrf())
                                                               .params(authorizeParams)
                                                               .param("code", demoCode))
                                              .andExpect(status().is3xxRedirection())
                                              .andReturn();

        final MockHttpSession session = (MockHttpSession) demoRedirect.getRequest().getSession(false);
        final UriComponents authorizeUri = UriComponentsBuilder.fromUriString(demoRedirect.getResponse().getHeader("Location"))
                                                               .build();

        final MvcResult callbackRedirect = mockMvc.perform(get(authorizeUri.getPath())
                                                                   .session(session)
                                                                   .queryParams(authorizeUri.getQueryParams()))
                                                  .andExpect(status().is3xxRedirection())
                                                  .andReturn();

        final UriComponents callbackUri = UriComponentsBuilder.fromUriString(callbackRedirect.getResponse().getHeader("Location"))
                                                             .build();
        final String authorizationCode = callbackUri.getQueryParams().getFirst(MetaDataKeys.CODE.getValue());

        assertThat(authorizationCode).isNotBlank();
        assertThat(demoAccessCodeJpaRepository.findByAccessCode(demoCode))
                .isPresent()
                .get()
                .extracting(code -> code.isDispensed())
                .isEqualTo(true);
    }

    @Test
    void publicClientAuthorizationCodePkceFlowSucceeds() throws Exception {
        final String redirectUri = "http://localhost:9101/callback";
        final String email = uniqueEmail("public-user");
        final String password = "Password123!";
        final String codeVerifier = "public-verifier-12345678901234567890123456789012345678901234567890";

        final RegisteredClientFixture registeredClient =
                savePublicAuthorizationCodeClient(redirectUri, Set.of("read"), true);
        final String clientId = registeredClient.registeredClient().getClientId();
        saveClientUser(email, password, clientId);

        final String authorizationCode = authorizeClientUserAndCaptureCode(
                clientId,
                redirectUri,
                email,
                password,
                Set.of("read"),
                codeVerifier
        );

        mockMvc.perform(post("/oauth2/token")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param(MetaDataKeys.GRANT_TYPE.getValue(), AuthorizationGrantType.AUTHORIZATION_CODE.getValue())
                                .param(MetaDataKeys.CODE.getValue(), authorizationCode)
                                .param(MetaDataKeys.REDIRECT_URI.getValue(), redirectUri)
                                .param(MetaDataKeys.CLIENT_ID.getValue(), clientId)
                                .param(MetaDataKeys.CODE_VERIFIER.getValue(), codeVerifier))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.access_token").exists())
               .andExpect(jsonPath("$.token_type").value("Bearer"));
    }

    @Test
    void confidentialClientAuthorizationCodeFlowSucceeds() throws Exception {
        final String redirectUri = "http://localhost:9201/callback";
        final String email = uniqueEmail("confidential-user");
        final String password = "Password123!";

        final RegisteredClientFixture registeredClient =
                saveConfidentialAuthorizationCodeClient(redirectUri, Set.of("read"));
        final String clientId = registeredClient.registeredClient().getClientId();
        final String clientSecret = registeredClient.rawClientSecret();
        saveClientUser(email, password, clientId);

        final String authorizationCode = authorizeClientUserAndCaptureCode(
                clientId,
                redirectUri,
                email,
                password,
                Set.of("read"),
                null
        );

        mockMvc.perform(post("/oauth2/token")
                                .with(httpBasic(clientId, clientSecret))
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param(MetaDataKeys.GRANT_TYPE.getValue(), AuthorizationGrantType.AUTHORIZATION_CODE.getValue())
                                .param(MetaDataKeys.CODE.getValue(), authorizationCode)
                                .param(MetaDataKeys.REDIRECT_URI.getValue(), redirectUri))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.access_token").exists())
               .andExpect(jsonPath("$.token_type").value("Bearer"));
    }

    @Test
    void confidentialClientCredentialsFlowSucceeds() throws Exception {
        final RegisteredClientFixture registeredClient =
                saveConfidentialClientCredentialsClient(Set.of("service.read"));
        final String clientId = registeredClient.registeredClient().getClientId();
        final String clientSecret = registeredClient.rawClientSecret();

        mockMvc.perform(post("/oauth2/token")
                                .with(httpBasic(clientId, clientSecret))
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param(MetaDataKeys.GRANT_TYPE.getValue(), AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())
                                .param(MetaDataKeys.SCOPE.getValue(), "service.read"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.access_token").exists())
               .andExpect(jsonPath("$.token_type").value("Bearer"));
    }

    private String authorizeClientUserAndCaptureCode(final String clientId,
                                                     final String redirectUri,
                                                     final String email,
                                                     final String password,
                                                     final Set<String> scopes,
                                                     final String codeVerifier) throws Exception {
        final String scope = String.join(" ", scopes);
        final MultiValueMap<String, String> authorizeParams = new LinkedMultiValueMap<>();
        authorizeParams.add("response_type", "code");
        authorizeParams.add("client_id", clientId);
        authorizeParams.add("redirect_uri", redirectUri);
        authorizeParams.add("scope", scope);
        authorizeParams.add("state", UUID.randomUUID().toString());
        if (codeVerifier != null) {
            authorizeParams.add(MetaDataKeys.CODE_CHALLENGE.getValue(), sha256Base64Url(codeVerifier));
            authorizeParams.add(MetaDataKeys.CODE_CHALLENGE_METHOD.getValue(), "S256");
        }

        final MvcResult authorizeRedirect = mockMvc.perform(get(
                                                            UriComponentsBuilder.fromPath("/oauth2/authorize")
                                                                                .queryParams(authorizeParams)
                                                                                .build(true)
                                                                                .toUriString())
                                                            .accept(MediaType.TEXT_HTML))
                                                   .andExpect(status().is3xxRedirection())
                                                   .andExpect(header().string("Location", endsWith("/login")))
                                                   .andReturn();

        final MockHttpSession session = (MockHttpSession) authorizeRedirect.getRequest().getSession(false);

        final MvcResult loginRedirect = mockMvc.perform(post("/login")
                                                                .session(session)
                                                                .with(csrf())
                                                                .param("email", email)
                                                                .param("password", password)
                                                                .param("clientId", clientId))
                                               .andExpect(status().is3xxRedirection())
                                               .andReturn();

        final UriComponents authorizeUri = UriComponentsBuilder.fromUriString(loginRedirect.getResponse().getHeader("Location"))
                                                               .build();

        final MvcResult callbackRedirect = mockMvc.perform(get(authorizeUri.getPath())
                                                                   .session(session)
                                                                   .queryParams(authorizeUri.getQueryParams()))
                                                  .andExpect(status().is3xxRedirection())
                                                  .andReturn();

        final UriComponents callbackUri = UriComponentsBuilder.fromUriString(callbackRedirect.getResponse().getHeader("Location"))
                                                             .build();
        return callbackUri.getQueryParams().getFirst(MetaDataKeys.CODE.getValue());
    }

    private RegisteredClientFixture savePublicAuthorizationCodeClient(final String redirectUri,
                                                                      final Set<String> scopes,
                                                                      final boolean requireProofKey) {
        return saveRegisteredClient(registeredClientRequest(
                uniqueClientId("public"),
                null,
                Set.of(ClientAuthenticationMethod.NONE),
                Set.of(AuthorizationGrantType.AUTHORIZATION_CODE),
                Set.of(redirectUri),
                scopes,
                requireProofKey
        ), null);
    }

    private RegisteredClientModel savePublicAuthorizationCodeClientWithExplicitClientId(final String clientId,
                                                                                        final String redirectUri,
                                                                                        final Set<String> scopes,
                                                                                        final boolean requireProofKey) {
        return saveRegisteredClient(registeredClientRequest(
                clientId,
                null,
                Set.of(ClientAuthenticationMethod.NONE),
                Set.of(AuthorizationGrantType.AUTHORIZATION_CODE),
                Set.of(redirectUri),
                scopes,
                requireProofKey
        ), null).registeredClient();
    }

    private RegisteredClientFixture saveConfidentialAuthorizationCodeClient(final String redirectUri,
                                                                            final Set<String> scopes) {
        final String rawClientSecret = "ConfidentialSecret123!";
        return saveRegisteredClient(registeredClientRequest(
                uniqueClientId("confidential-auth-code"),
                rawClientSecret,
                Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
                Set.of(AuthorizationGrantType.AUTHORIZATION_CODE),
                Set.of(redirectUri),
                scopes,
                false
        ), rawClientSecret);
    }

    private RegisteredClientFixture saveConfidentialClientCredentialsClient(final Set<String> scopes) {
        final String rawClientSecret = "ServiceSecret123!";
        return saveRegisteredClient(registeredClientRequest(
                uniqueClientId("service"),
                rawClientSecret,
                Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
                Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS),
                Collections.emptySet(),
                scopes,
                false
        ), rawClientSecret);
    }

    private void saveClientUser(final String email,
                                final String rawPassword,
                                final String clientId) {
        final ClientUser clientUser = ClientUser.from(email)
                                                .email(email)
                                                .clientId(clientId)
                                                .passwordHash(passwordEncoder.encode(rawPassword))
                                                .verified(true)
                                                .createdAt(LocalDateTime.now())
                                                .updatedAt(LocalDateTime.now())
                                                .userAttributes(new HashMap<>())
                                                .authorities(authorities -> authorities.add(Authority.from("ROLE_USER")))
                                                .locked(false)
                                                .expired(false)
                                                .credentialsExpired(false)
                                                .build();
        userService.saveUser(clientUser);
    }

    private void savePlatformUser(final String email,
                                  final String rawPassword) {
        ensureFrontendClientExists();
        final PlatformUser platformUser = PlatformUser.from(email)
                                                      .email(email)
                                                      .passwordHash(passwordEncoder.encode(rawPassword))
                                                      .verified(true)
                                                      .createdAt(LocalDateTime.now())
                                                      .updatedAt(LocalDateTime.now())
                                                      .registeredClientIds(ids -> ids.clear())
                                                      .authorities(authorities -> authorities.add(Authority.from("ROLE_PLATFORM_USER")))
                                                      .locked(false)
                                                      .expired(false)
                                                      .credentialsExpired(false)
                                                      .build();
        userService.savePlatformUser(platformUser);
    }

    private void ensureFrontendClientExists() {
        if (registerClientJpaRepository.findByClientId(FRONTEND_CLIENT_ID).isPresent()) {
            return;
        }
        saveRegisteredClient(registeredClientRequest(
                FRONTEND_CLIENT_ID,
                null,
                Set.of(ClientAuthenticationMethod.NONE),
                Set.of(AuthorizationGrantType.AUTHORIZATION_CODE),
                Set.of("http://localhost:9999/frontend/callback"),
                Set.of("openid"),
                true
        ), null);
    }

    private void ensureBasicTierExists() {
        if (platformUserTierJpaRepository.findByTierNameIgnoreCase("BASIC").isPresent()) {
            return;
        }

        platformUserTierJpaRepository.save(new PlatformUserTierEntity(
                null,
                null,
                "BASIC",
                15,
                "A starter production tier for smaller teams that need a few clients and enough room for regular user activity.",
                1,
                3,
                300,
                25,
                25
        ));
    }

    private String bearerToken(final String subject,
                               final String azp,
                               final List<String> authorities) {
        final Instant issuedAt = Instant.now();
        final JwtClaimsSet claims = JwtClaimsSet.builder()
                                                .subject(subject)
                                                .issuedAt(issuedAt)
                                                .expiresAt(issuedAt.plus(15, ChronoUnit.MINUTES))
                                                .claim(MetaDataKeys.AZP.getValue(), azp)
                                                .claim("authorities", authorities)
                                                .build();

        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(),
                claims
        )).getTokenValue();
    }

    private String sha256Base64Url(final String value) throws Exception {
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        final byte[] digest = messageDigest.digest(value.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private String uniqueEmail(final String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private String uniqueClientId(final String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private ClientSettings clientSettings(final boolean requireProofKey) {
        return ClientSettings.builder()
                             .requireProofKey(requireProofKey)
                             .requireAuthorizationConsent(false)
                             .build();
    }

    private RegisteredClientRequest registeredClientRequest(final String clientId,
                                                            final String clientSecret,
                                                            final Set<ClientAuthenticationMethod> authenticationMethods,
                                                            final Set<AuthorizationGrantType> grantTypes,
                                                            final Set<String> redirectUris,
                                                            final Set<String> scopes,
                                                            final boolean requireProofKey) {
        return RegisteredClientRequest.builder()
                                      .clientId(clientId)
                                      .clientIdIssuedAt(LocalDateTime.now())
                                      .clientSecret(clientSecret)
                                      .clientSecretExpiresAt(LocalDateTime.now().plusDays(30))
                                      .clientName(clientId)
                                      .clientAuthenticationMethods(authenticationMethods.stream()
                                                                                       .map(ClientAuthenticationMethod::getValue)
                                                                                       .collect(Collectors.toCollection(LinkedHashSet::new)))
                                      .authorizationGrantTypes(grantTypes.stream()
                                                                         .map(AuthorizationGrantType::getValue)
                                                                         .collect(Collectors.toCollection(LinkedHashSet::new)))
                                      .redirectUris(redirectUris)
                                      .postLogoutRedirectUris(Collections.emptySet())
                                      .scopes(scopes)
                                      .clientSettings(clientSettingsRequest(requireProofKey))
                                      .tokenSettings(tokenSettingsRequest())
                                      .build();
    }

    private RegisteredClientFixture saveRegisteredClient(final RegisteredClientRequest registeredClientRequest,
                                                         final String rawClientSecret) {
        final boolean requireProofKey = Boolean.TRUE.equals(registeredClientRequest.getClientSettings()
                                                                                   .get("requireProofKey"));
        final RegisteredClientEntity entity = RegisteredClientEntity.create(
                null,
                registeredClientRequest.getClientId(),
                registeredClientRequest.getClientIdIssuedAt(),
                rawClientSecret == null ? null : passwordEncoder.encode(rawClientSecret),
                registeredClientRequest.getClientSecretExpiresAt(),
                registeredClientRequest.getClientName(),
                new HashMap<>(clientSettings(requireProofKey).getSettings()),
                tokenSettingsMapper.oAuthTokenSettingsToTokenSettingsJson(TokenSettings.builder().build())
        );
        registeredClientRequest.getClientAuthenticationMethods().forEach(entity::addClientAuthenticationMethod);
        registeredClientRequest.getAuthorizationGrantTypes().forEach(entity::addAuthorizationGrantType);
        registeredClientRequest.getRedirectUris().forEach(entity::addRedirectUri);
        registeredClientRequest.getPostLogoutRedirectUris().forEach(entity::addPostLogoutRedirectUri);
        entity.setScopes(resolveManagedScopes(registeredClientRequest.getScopes()));
        final RegisteredClientEntity savedEntity = registerClientJpaRepository.save(entity);
        return new RegisteredClientFixture(
                registeredClientMapper.registeredClientEntityToRegisteredClientModel(savedEntity),
                rawClientSecret
        );
    }

    private Set<RegisteredClientScopeEntity> resolveManagedScopes(final Set<String> scopes) {
        final Set<String> requestedScopes = scopes == null
                                            ? Collections.emptySet()
                                            : scopes.stream()
                                                    .filter(scope -> scope != null && !scope.isBlank())
                                                    .map(String::trim)
                                                    .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requestedScopes.isEmpty()) {
            return new LinkedHashSet<>();
        }

        final Set<RegisteredClientScopeEntity> attachedScopes =
                registeredClientScopeJpaRepository.findAllByScopeIn(requestedScopes);
        final Set<String> existingScopes = attachedScopes.stream()
                                                         .map(RegisteredClientScopeEntity::getScope)
                                                         .collect(Collectors.toSet());

        final Set<RegisteredClientScopeEntity> createdScopes = requestedScopes.stream()
                                                                              .filter(scope -> !existingScopes.contains(scope))
                                                                              .map(scope -> new RegisteredClientScopeEntity(null, scope))
                                                                              .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!createdScopes.isEmpty()) {
            attachedScopes.addAll(registeredClientScopeJpaRepository.saveAll(createdScopes));
        }
        return new LinkedHashSet<>(attachedScopes);
    }

    private HashMap<String, Object> clientSettingsRequest(final boolean requireProofKey) {
        final HashMap<String, Object> clientSettings = new HashMap<>();
        clientSettings.put("requireProofKey", requireProofKey);
        clientSettings.put("requireAuthorizationConsent", false);
        return clientSettings;
    }

    private HashMap<String, Object> tokenSettingsRequest() {
        final HashMap<String, Object> tokenSettings = new HashMap<>();
        tokenSettings.put("accessTokenTimeToLiveMinutes", 5);
        tokenSettings.put("refreshTokenTimeToLiveMinutes", 60);
        tokenSettings.put("authorizationCodeTimeToLiveMinutes", 5);
        tokenSettings.put("reuseRefreshTokens", true);
        return tokenSettings;
    }

    private record RegisteredClientFixture(RegisteredClientModel registeredClient, String rawClientSecret) {
    }

}

