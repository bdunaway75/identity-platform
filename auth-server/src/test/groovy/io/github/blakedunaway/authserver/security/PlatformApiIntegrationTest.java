package io.github.blakedunaway.authserver.security;

import com.stripe.StripeClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.blakedunaway.authserver.business.api.dto.request.ClientUserRequest;
import io.github.blakedunaway.authserver.business.api.dto.request.RegisteredClientRequest;
import io.github.blakedunaway.authserver.business.model.Authority;
import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import io.github.blakedunaway.authserver.business.model.enums.MetaDataKeys;
import io.github.blakedunaway.authserver.business.model.user.ClientUser;
import io.github.blakedunaway.authserver.business.model.user.PlatformUser;
import io.github.blakedunaway.authserver.business.model.user.PlatformUserTier;
import io.github.blakedunaway.authserver.business.service.RegisteredClientService;
import io.github.blakedunaway.authserver.business.service.UserService;
import io.github.blakedunaway.authserver.config.app.Application;
import io.github.blakedunaway.authserver.config.redis.RedisStore;
import io.github.blakedunaway.authserver.integration.entity.PlatformUserTierEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientEntity;
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientScopeEntity;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
public class PlatformApiIntegrationTest {

    private static final String PLATFORM_CLIENT_ID = "identity-platform";
    private static final String FRONTEND_CLIENT_ID = "test-frontend-client";
    private static final ObjectMapper API_REQUEST_OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private RegisteredClientService registeredClientService;

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

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
    void unauthenticatedUsersCannotAccessPlatformApiEndpoints() throws Exception {
        final UUID registeredClientId = UUID.randomUUID();
        final UUID clientUserId = UUID.randomUUID();
        final UUID authTokenId = UUID.randomUUID();
        final String idsJson = typedRegisteredClientIdsJson(Set.of(registeredClientId));
        final String clientUserJson = plainJson(new ClientUserRequest());
        final String registeredClientJson = sanitizedMvcJson(registeredClientRequest(
                uniqueClientId("unauth"),
                null,
                Set.of(ClientAuthenticationMethod.NONE),
                Set.of(AuthorizationGrantType.AUTHORIZATION_CODE),
                Set.of("http://localhost:9901/callback"),
                Set.of("read"),
                true
        ));

        mockMvc.perform(post("/platform/api/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registeredClientJson))
               .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/platform/api/total-user-count"))
               .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/platform/api/total-client-count"))
               .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/platform/api"))
               .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/platform/api/{registeredClientId}", registeredClientId))
               .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/platform/api/{registeredClientId}/update", registeredClientId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registeredClientJson))
               .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/platform/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(idsJson))
               .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/platform/api/users/{clientUserId}", clientUserId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(clientUserJson))
               .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/platform/api/tokens")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(idsJson))
               .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/platform/api/tokens/{authTokenId}/invalidate", authTokenId))
               .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/platform/api/{registeredClientId}/tokens/invalidate", registeredClientId))
               .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/platform/api/tier-status"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void platformUserCanCreateAndReadOnlyOwnedRegisteredClients() throws Exception {
        final String platformEmail = uniqueEmail("platform-clients");
        savePlatformUser(platformEmail, "Password123!");
        final String bearerToken = bearerToken(platformEmail,
                                               PLATFORM_CLIENT_ID,
                                               List.of("ROLE_PLATFORM_USER", "PLATFORM_TIER_PAID"));

        final String requestedClientName = uniqueClientId("confidential-owned");
        final RegisteredClientFixture ownedClient = saveRegisteredClient(
                registeredClientRequest(
                        requestedClientName,
                        "ConfidentialSecret123!",
                        Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
                        Set.of(AuthorizationGrantType.AUTHORIZATION_CODE),
                        Set.of("http://localhost:9301/callback"),
                        Set.of("read"),
                        false
                ),
                "ConfidentialSecret123!"
        );
        userService.attachRegisteredClientToPlatformUser(platformEmail, ownedClient.registeredClient().getId());

        final RegisteredClientFixture foreignClient = savePublicAuthorizationCodeClient(
                "http://localhost:9302/callback",
                Set.of("foreign.read"),
                true
        );

        final JsonNode dashboard = objectMapper.readTree(mockMvc.perform(get("/platform/api/dashboard")
                                                                                 .header("Authorization", bearerToken))
                                                                        .andExpect(status().isOk())
                                                                        .andReturn()
                                                                        .getResponse()
                                                                        .getContentAsString());
        final JsonNode registeredClients = unwrapCollectionNode(dashboard.get("registeredClientResponses"));
        assertThat(registeredClients).hasSize(1);
        assertThat(registeredClients.get(0).get("id").asText()).isEqualTo(ownedClient.registeredClient().getId().toString());
        assertThat(registeredClients.get(0).get("clientSecret").isNull()).isTrue();
        assertThat(dashboard.get("totalRegisteredClients").asInt()).isEqualTo(1);
        assertThat(dashboard.get("demoUser").asBoolean()).isFalse();
        final JsonNode clientIds = unwrapCollectionNode(dashboard.get("clientIds"));
        assertThat(clientIds).anyMatch(node -> node.asText().equals(requestedClientName));
        assertThat(clientIds).noneMatch(node -> node.asText().equals(foreignClient.registeredClient().getClientId()));
    }

    @Test
    void platformUserCanCreatePublicAuthorizationCodeClientAndNewUserCanSignUpAuthenticate() throws Exception {
        final String platformEmail = uniqueEmail("platform-create-public");
        savePlatformUser(platformEmail, "Password123!", "BASIC");
        final String bearerToken = bearerToken(platformEmail,
                                               PLATFORM_CLIENT_ID,
                                               List.of("ROLE_PLATFORM_USER", "PLATFORM_TIER_PAID"));

        final String requestedClientName = uniqueClientId("created-public");
        final String redirectUri = "http://localhost:9601/callback";
        final Set<String> scopes = Set.of("read");
        final CreatedPlatformClient createdClient = createRegisteredClientViaPlatformApi(
                bearerToken,
                platformCreateRequest(
                        requestedClientName,
                        Set.of(ClientAuthenticationMethod.NONE),
                        Set.of(AuthorizationGrantType.AUTHORIZATION_CODE),
                        Set.of(redirectUri),
                        scopes,
                        true
                )
        );

        assertThat(createdClient.clientId()).isNotBlank();
        assertThat(createdClient.rawClientSecret()).isNull();
        assertThat(createdClient.registeredClient().getClientName()).isEqualTo(requestedClientName);
        assertThat(createdClient.registeredClient().getClientAuthenticationMethods())
                .containsExactlyInAnyOrder(ClientAuthenticationMethod.NONE.getValue());
        assertThat(createdClient.registeredClient().getAuthorizationGrantTypes())
                .containsExactlyInAnyOrder(AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
        assertThat(createdClient.registeredClient().getRedirectUris()).containsExactlyInAnyOrder(redirectUri);

        final String email = uniqueEmail("created-public-user");
        final String password = "Password123!";
        final String codeVerifier = "created-public-verifier-123456789012345678901234567890123456789012345";

        final String authorizationCode = signUpAndAuthorizeClientUserAndCaptureCode(
                createdClient,
                redirectUri,
                email,
                password,
                scopes,
                codeVerifier
        );

        mockMvc.perform(post("/oauth2/token")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param(MetaDataKeys.GRANT_TYPE.getValue(), AuthorizationGrantType.AUTHORIZATION_CODE.getValue())
                                .param(MetaDataKeys.CODE.getValue(), authorizationCode)
                                .param(MetaDataKeys.REDIRECT_URI.getValue(), redirectUri)
                                .param(MetaDataKeys.CLIENT_ID.getValue(), createdClient.clientId())
                                .param(MetaDataKeys.CODE_VERIFIER.getValue(), codeVerifier))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.access_token").exists())
               .andExpect(jsonPath("$.token_type").value("Bearer"));

        final JsonNode ownedUsers = unwrapCollectionResponse(mockMvc.perform(post("/platform/api/users")
                                                                                     .header("Authorization", bearerToken)
                                                                                     .contentType(MediaType.APPLICATION_JSON)
                                                                                     .content(typedRegisteredClientIdsJson(Set.of(createdClient.id()))))
                                                                            .andExpect(status().isOk())
                                                                            .andReturn());
        assertThat(ownedUsers).hasSize(1);
        assertThat(ownedUsers.get(0).get("clientId").asText()).isEqualTo(createdClient.clientId());
        assertThat(ownedUsers.get(0).get("email").asText()).isEqualTo(email);
    }

    @Test
    void platformUserCanCreateConfidentialAuthorizationCodeClientAndNewUserCanSignUpAuthenticate() throws Exception {
        final String platformEmail = uniqueEmail("platform-create-confidential");
        savePlatformUser(platformEmail, "Password123!", "BASIC");
        final String bearerToken = bearerToken(platformEmail,
                                               PLATFORM_CLIENT_ID,
                                               List.of("ROLE_PLATFORM_USER", "PLATFORM_TIER_PAID"));

        final String requestedClientName = uniqueClientId("created-confidential");
        final String redirectUri = "http://localhost:9602/callback";
        final Set<String> scopes = Set.of("payments.read");
        final CreatedPlatformClient createdClient = createRegisteredClientViaPlatformApi(
                bearerToken,
                platformCreateRequest(
                        requestedClientName,
                        Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
                        Set.of(AuthorizationGrantType.AUTHORIZATION_CODE),
                        Set.of(redirectUri),
                        scopes,
                        false
                )
        );

        assertThat(createdClient.clientId()).isNotBlank();
        assertThat(createdClient.rawClientSecret()).isNotBlank();
        assertThat(createdClient.registeredClient().getClientName()).isEqualTo(requestedClientName);
        assertThat(createdClient.registeredClient().getClientAuthenticationMethods())
                .containsExactlyInAnyOrder(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue());
        assertThat(createdClient.registeredClient().getAuthorizationGrantTypes())
                .containsExactlyInAnyOrder(AuthorizationGrantType.AUTHORIZATION_CODE.getValue());

        final String email = uniqueEmail("created-confidential-user");
        final String password = "Password123!";

        final String authorizationCode = signUpAndAuthorizeClientUserAndCaptureCode(
                createdClient,
                redirectUri,
                email,
                password,
                scopes,
                null
        );

        mockMvc.perform(post("/oauth2/token")
                                .with(httpBasic(createdClient.clientId(), createdClient.rawClientSecret()))
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param(MetaDataKeys.GRANT_TYPE.getValue(), AuthorizationGrantType.AUTHORIZATION_CODE.getValue())
                                .param(MetaDataKeys.CODE.getValue(), authorizationCode)
                                .param(MetaDataKeys.REDIRECT_URI.getValue(), redirectUri))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.access_token").exists())
               .andExpect(jsonPath("$.token_type").value("Bearer"));

        final JsonNode ownedUsers = unwrapCollectionResponse(mockMvc.perform(post("/platform/api/users")
                                                                                     .header("Authorization", bearerToken)
                                                                                     .contentType(MediaType.APPLICATION_JSON)
                                                                                     .content(typedRegisteredClientIdsJson(Set.of(createdClient.id()))))
                                                                            .andExpect(status().isOk())
                                                                            .andReturn());
        assertThat(ownedUsers).hasSize(1);
        assertThat(ownedUsers.get(0).get("clientId").asText()).isEqualTo(createdClient.clientId());
        assertThat(ownedUsers.get(0).get("email").asText()).isEqualTo(email);
    }

    @Test
    void platformUserCanCreateConfidentialClientCredentialsClientAndAuthenticate() throws Exception {
        final String platformEmail = uniqueEmail("platform-create-service");
        savePlatformUser(platformEmail, "Password123!", "BASIC");
        final String bearerToken = bearerToken(platformEmail,
                                               PLATFORM_CLIENT_ID,
                                               List.of("ROLE_PLATFORM_USER", "PLATFORM_TIER_PAID"));

        final String requestedClientName = uniqueClientId("created-service");
        final CreatedPlatformClient createdClient = createRegisteredClientViaPlatformApi(
                bearerToken,
                platformCreateRequest(
                        requestedClientName,
                        Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
                        Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS),
                        Collections.emptySet(),
                        Set.of("service.read"),
                        false
                )
        );

        assertThat(createdClient.clientId()).isNotBlank();
        assertThat(createdClient.rawClientSecret()).isNotBlank();
        assertThat(createdClient.registeredClient().getClientName()).isEqualTo(requestedClientName);
        assertThat(createdClient.registeredClient().getRedirectUris()).isEmpty();
        assertThat(createdClient.registeredClient().getClientAuthenticationMethods())
                .containsExactlyInAnyOrder(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue());
        assertThat(createdClient.registeredClient().getAuthorizationGrantTypes())
                .containsExactlyInAnyOrder(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());

        mockMvc.perform(post("/oauth2/token")
                                .with(httpBasic(createdClient.clientId(), createdClient.rawClientSecret()))
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param(MetaDataKeys.GRANT_TYPE.getValue(), AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())
                                .param(MetaDataKeys.SCOPE.getValue(), "service.read"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.access_token").exists())
               .andExpect(jsonPath("$.token_type").value("Bearer"));

        final JsonNode issuedTokens = unwrapCollectionResponse(mockMvc.perform(post("/platform/api/tokens")
                                                                                       .header("Authorization", bearerToken)
                                                                                       .contentType(MediaType.APPLICATION_JSON)
                                                                                       .content(typedRegisteredClientIdsJson(Set.of(createdClient.id()))))
                                                                              .andExpect(status().isOk())
                                                                              .andReturn());
        assertThat(issuedTokens).hasSize(1);
        assertThat(issuedTokens.get(0).get("subject").asText()).isEqualTo(createdClient.clientId());
    }

    @Test
    void platformUserCanUpdateOnlyOwnedRegisteredClient() throws Exception {
        final String platformEmail = uniqueEmail("platform-update-client");
        savePlatformUser(platformEmail, "Password123!");
        final String bearerToken = bearerToken(platformEmail,
                                               PLATFORM_CLIENT_ID,
                                               List.of("ROLE_PLATFORM_USER", "PLATFORM_TIER_PAID"));

        final RegisteredClientFixture ownedClient = saveRegisteredClient(
                registeredClientRequest(
                        uniqueClientId("owned-update"),
                        "UpdateSecret123!",
                        Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
                        Set.of(AuthorizationGrantType.AUTHORIZATION_CODE),
                        Set.of("https://owned.example.com/callback"),
                        Set.of("read"),
                        false
                ),
                "UpdateSecret123!"
        );
        userService.attachRegisteredClientToPlatformUser(platformEmail, ownedClient.registeredClient().getId());

        final RegisteredClientFixture foreignClient = savePublicAuthorizationCodeClient(
                "https://foreign.example.com/callback",
                Set.of("foreign.read"),
                true
        );

        final String updateRequestJson = registeredClientUpdateJson(
                "updated-owned-client",
                Set.of("https://updated.example.com/callback"),
                Set.of("https://updated.example.com/logout"),
                Set.of("read", "write"),
                Set.of("perm_manage"),
                Set.of("role_support"),
                clientSettingsRequest(false),
                tokenSettingsRequest()
        );
        assertMvcCanReadRegisteredClientRequest(updateRequestJson);

        final JsonNode updatedClient = objectMapper.readTree(mockMvc.perform(patch("/platform/api/{registeredClientId}/update",
                                                                                   ownedClient.registeredClient().getId())
                                                                            .header("Authorization", bearerToken)
                                                                            .contentType(MediaType.APPLICATION_JSON)
                                                                            .content(updateRequestJson))
                                                                   .andExpect(status().isOk())
                                                                   .andReturn()
                                                                   .getResponse()
                                                                   .getContentAsString());

        assertThat(updatedClient.get("id").asText()).isEqualTo(ownedClient.registeredClient().getId().toString());
        assertThat(updatedClient.get("clientId").asText()).isEqualTo(ownedClient.registeredClient().getClientId());
        assertThat(updatedClient.get("clientName").asText()).isEqualTo("updated-owned-client");
        assertThat(unwrapCollectionNode(updatedClient.get("redirectUris"))).extracting(JsonNode::asText)
                                                                           .containsExactly("https://updated.example.com/callback");
        assertThat(unwrapCollectionNode(updatedClient.get("postLogoutRedirectUris"))).extracting(JsonNode::asText)
                                                                                     .containsExactly("https://updated.example.com/logout");
        assertThat(unwrapCollectionNode(updatedClient.get("scopes"))).extracting(JsonNode::asText)
                                                                     .containsExactlyInAnyOrder("read", "write");
        assertThat(unwrapCollectionNode(updatedClient.get("authorities"))).extracting(JsonNode::asText)
                                                                          .containsExactly("PERM_MANAGE");
        assertThat(unwrapCollectionNode(updatedClient.get("roles"))).extracting(JsonNode::asText)
                                                                    .containsExactly("ROLE_SUPPORT");

        final RegisteredClientModel persistedOwnedClient =
                registeredClientService.findRegisteredClientById(ownedClient.registeredClient().getId());
        assertThat(persistedOwnedClient.getClientName()).isEqualTo("updated-owned-client");
        assertThat(persistedOwnedClient.getRedirectUris()).containsExactly("https://updated.example.com/callback");
        assertThat(persistedOwnedClient.getPostLogoutRedirectUris()).containsExactly("https://updated.example.com/logout");
        assertThat(persistedOwnedClient.getAuthorities()).containsExactly("PERM_MANAGE");
        assertThat(persistedOwnedClient.getRoles()).containsExactly("ROLE_SUPPORT");

        final String authorityPruneEmail = uniqueEmail("owned-update-prune");
        saveClientUser(
                authorityPruneEmail,
                "Password123!",
                ownedClient.registeredClient().getClientId(),
                Set.of("PERM_MANAGE", "ROLE_SUPPORT")
        );

        final String removeAuthorityRequestJson = registeredClientUpdateJson(
                "updated-owned-client",
                Set.of("https://updated.example.com/callback"),
                Set.of("https://updated.example.com/logout"),
                Set.of("read", "write"),
                Collections.emptySet(),
                Collections.emptySet(),
                clientSettingsRequest(false),
                tokenSettingsRequest()
        );

        mockMvc.perform(patch("/platform/api/{registeredClientId}/update",
                              ownedClient.registeredClient().getId())
                                .header("Authorization", bearerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(removeAuthorityRequestJson))
               .andExpect(status().isOk());

        final ClientUser prunedClientUser = userService.findClientUsersByRegisteredClientIds(Set.of(ownedClient.registeredClient().getId()))
                                                       .stream()
                                                       .filter(clientUser -> authorityPruneEmail.equalsIgnoreCase(clientUser.getEmail()))
                                                       .findFirst()
                                                       .orElseThrow();
        assertThat(prunedClientUser.getAuthorities()).isEmpty();

        mockMvc.perform(patch("/platform/api/{registeredClientId}/update", foreignClient.registeredClient().getId())
                                .header("Authorization", bearerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateRequestJson))
               .andExpect(status().isNotFound());
    }

    @Test
    void platformUserCountsOnlyOwnedClientsAndUsers() throws Exception {
        final String platformEmail = uniqueEmail("platform-counts");
        savePlatformUser(platformEmail, "Password123!");

        final RegisteredClientFixture ownedClientOne = savePublicAuthorizationCodeClient(
                "http://localhost:9401/callback",
                Set.of("read"),
                true
        );
        final RegisteredClientFixture ownedClientTwo = savePublicAuthorizationCodeClient(
                "http://localhost:9402/callback",
                Set.of("write"),
                true
        );
        final RegisteredClientFixture foreignClient = savePublicAuthorizationCodeClient(
                "http://localhost:9403/callback",
                Set.of("foreign"),
                true
        );

        userService.attachRegisteredClientToPlatformUser(platformEmail, ownedClientOne.registeredClient().getId());
        userService.attachRegisteredClientToPlatformUser(platformEmail, ownedClientTwo.registeredClient().getId());

        saveClientUser(uniqueEmail("owned-user-1"), "Password123!", ownedClientOne.registeredClient().getClientId());
        saveClientUser(uniqueEmail("owned-user-2"), "Password123!", ownedClientTwo.registeredClient().getClientId());
        saveClientUser(uniqueEmail("foreign-user"), "Password123!", foreignClient.registeredClient().getClientId());

        final String bearerToken = bearerToken(platformEmail, PLATFORM_CLIENT_ID, List.of("ROLE_PLATFORM_USER"));

        mockMvc.perform(get("/platform/api/dashboard")
                                .header("Authorization", bearerToken))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.demoUser").value(false))
               .andExpect(jsonPath("$.totalRegisteredClients").value(2))
               .andExpect(jsonPath("$.totalUsers").value(2));
    }

    @Test
    void platformUserCanFetchAndUpdateOnlyOwnedClientUsers() throws Exception {
        final String platformEmail = uniqueEmail("platform-users");
        savePlatformUser(platformEmail, "Password123!");

        final RegisteredClientFixture ownedClient = savePublicAuthorizationCodeClient(
                "http://localhost:9501/callback",
                Set.of("read"),
                true
        );
        final RegisteredClientFixture foreignClient = savePublicAuthorizationCodeClient(
                "http://localhost:9502/callback",
                Set.of("foreign"),
                true
        );
        userService.attachRegisteredClientToPlatformUser(platformEmail, ownedClient.registeredClient().getId());

        final ClientUser ownedUser = saveClientUser(
                uniqueEmail("owned-client-user"),
                "Password123!",
                ownedClient.registeredClient().getClientId()
        );
        final ClientUser foreignUser = saveClientUser(
                uniqueEmail("foreign-client-user"),
                "Password123!",
                foreignClient.registeredClient().getClientId()
        );

        final String bearerToken = bearerToken(platformEmail,
                                               PLATFORM_CLIENT_ID,
                                               List.of("ROLE_PLATFORM_USER", "PLATFORM_TIER_PAID"));

        final JsonNode clientUsers = unwrapCollectionResponse(mockMvc.perform(post("/platform/api/users")
                                                                                      .header("Authorization", bearerToken)
                                                                                      .contentType(MediaType.APPLICATION_JSON)
                                                                                      .content(typedRegisteredClientIdsJson(Set.of(
                                                                                              ownedClient.registeredClient().getId(),
                                                                                              foreignClient.registeredClient().getId()
                                                                                      ))))
                                                                             .andExpect(status().isOk())
                                                                             .andReturn());
        assertThat(clientUsers).hasSize(1);
        assertThat(clientUsers.get(0).get("id").asText()).isEqualTo(ownedUser.getId().toString());
        assertThat(clientUsers.get(0).get("email").asText()).isEqualTo(ownedUser.getEmail());

        final ClientUserRequest updateRequest = new ClientUserRequest();
        updateRequest.setEmail("updated-" + ownedUser.getEmail());
        updateRequest.setVerified(Boolean.FALSE);
        updateRequest.setLocked(Boolean.TRUE);
        updateRequest.setAuthorities(new LinkedHashSet<>(Set.of("ROLE_SUPPORT", "ROLE_USER")));

        final ClientUser updatedUser = userService.updateClientUser(
                ownedUser.getId(),
                Set.of(ownedClient.registeredClient().getId()),
                updateRequest
        );
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getId()).isEqualTo(ownedUser.getId());
        assertThat(updatedUser.getEmail()).isEqualTo(updateRequest.getEmail());
        assertThat(updatedUser.isVerified()).isFalse();
        assertThat(updatedUser.isLocked()).isTrue();

        final ClientUser missingUpdatedUser = userService.updateClientUser(
                foreignUser.getId(),
                Set.of(ownedClient.registeredClient().getId()),
                updateRequest
        );
        assertThat(missingUpdatedUser).isNull();
    }

    @Test
    void platformUserCanFetchAndInvalidateOnlyOwnedTokens() throws Exception {
        final String platformEmail = uniqueEmail("platform-tokens");
        savePlatformUser(platformEmail, "Password123!");

        final RegisteredClientFixture ownedClient = saveConfidentialClientCredentialsClient(Set.of("service.read"));
        final RegisteredClientFixture foreignClient = saveConfidentialClientCredentialsClient(Set.of("foreign.read"));
        userService.attachRegisteredClientToPlatformUser(platformEmail, ownedClient.registeredClient().getId());

        issueClientCredentialsToken(
                ownedClient.registeredClient().getClientId(),
                ownedClient.rawClientSecret(),
                "service.read"
        );
        issueClientCredentialsToken(
                foreignClient.registeredClient().getClientId(),
                foreignClient.rawClientSecret(),
                "foreign.read"
        );

        final String bearerToken = bearerToken(platformEmail,
                                               PLATFORM_CLIENT_ID,
                                               List.of("ROLE_PLATFORM_USER", "PLATFORM_TIER_PAID"));

        final MvcResult tokensResult = mockMvc.perform(post("/platform/api/tokens")
                                                               .header("Authorization", bearerToken)
                                                               .contentType(MediaType.APPLICATION_JSON)
                                                               .content(typedRegisteredClientIdsJson(Set.of(
                                                                       ownedClient.registeredClient().getId(),
                                                                       foreignClient.registeredClient().getId()
                                                               ))))
                                              .andExpect(status().isOk())
                                              .andReturn();

        final JsonNode tokens = unwrapCollectionResponse(tokensResult);
        final UUID ownedTokenId = UUID.fromString(tokens.get(0).get("id").asText());

        mockMvc.perform(post("/platform/api/tokens/{authTokenId}/invalidate", ownedTokenId)
                                .header("Authorization", bearerToken))
               .andExpect(status().isNoContent());

        issueClientCredentialsToken(
                ownedClient.registeredClient().getClientId(),
                ownedClient.rawClientSecret(),
                "service.read"
        );

        final JsonNode updatedTokens = unwrapCollectionResponse(mockMvc.perform(post("/platform/api/tokens")
                                                                                        .header("Authorization", bearerToken)
                                                                                        .contentType(MediaType.APPLICATION_JSON)
                                                                                        .content(typedRegisteredClientIdsJson(Set.of(
                                                                                                ownedClient.registeredClient().getId()
                                                                                        ))))
                                                                               .andExpect(status().isOk())
                                                                               .andReturn());
        int activeTokenCount = 0;
        int revokedTokenCount = 0;
        for (final JsonNode updatedToken : updatedTokens) {
            if (updatedToken.get("revokedAt").isNull()) {
                activeTokenCount++;
            } else {
                revokedTokenCount++;
            }
        }
        assertThat(updatedTokens).hasSize(2);
        assertThat(activeTokenCount).isEqualTo(1);
        assertThat(revokedTokenCount).isEqualTo(1);

        mockMvc.perform(post("/platform/api/tokens/{authTokenId}/invalidate", UUID.randomUUID())
                                .header("Authorization", bearerToken))
               .andExpect(status().isNotFound());

        final MvcResult invalidateAllResult = mockMvc.perform(post("/platform/api/{registeredClientId}/tokens/invalidate",
                                                                   ownedClient.registeredClient().getId())
                                                                      .header("Authorization", bearerToken))
                                                     .andExpect(status().isOk())
                                                     .andReturn();
        assertThat(Integer.parseInt(invalidateAllResult.getResponse().getContentAsString())).isGreaterThanOrEqualTo(1);

        mockMvc.perform(post("/platform/api/{registeredClientId}/tokens/invalidate",
                             foreignClient.registeredClient().getId())
                                .header("Authorization", bearerToken))
               .andExpect(status().isNotFound());
    }

    private CreatedPlatformClient createRegisteredClientViaPlatformApi(final String bearerToken,
                                                                       final RegisteredClientRequest request)
            throws Exception {
        final JsonNode response = objectMapper.readTree(mockMvc.perform(post("/platform/api/create")
                                                                                .header("Authorization", bearerToken)
                                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                                .content(sanitizedMvcJson(request)))
                                                                       .andExpect(status().isOk())
                                                                       .andReturn()
                                                                       .getResponse()
                                                                       .getContentAsString());
        final UUID createdClientId = UUID.fromString(response.get("id").asText());
        return new CreatedPlatformClient(
                createdClientId,
                response.get("clientId").asText(),
                response.path("clientSecret").isNull() ? null : response.get("clientSecret").asText(),
                registeredClientService.findRegisteredClientById(createdClientId)
        );
    }

    private RegisteredClientRequest platformCreateRequest(final String clientName,
                                                          final Set<ClientAuthenticationMethod> authenticationMethods,
                                                          final Set<AuthorizationGrantType> grantTypes,
                                                          final Set<String> redirectUris,
                                                          final Set<String> scopes,
                                                          final boolean requireProofKey) {
        return registeredClientRequest(
                uniqueClientId("seed"),
                null,
                authenticationMethods,
                grantTypes,
                redirectUris,
                scopes,
                requireProofKey
        ).toBuilder()
         .clientId(null)
         .clientIdIssuedAt(null)
         .clientSecret(null)
         .clientSecretExpiresAt(null)
         .clientName(clientName)
         .build();
    }

    private String signUpAndAuthorizeClientUserAndCaptureCode(final CreatedPlatformClient createdClient,
                                                              final String redirectUri,
                                                              final String email,
                                                              final String password,
                                                              final Set<String> scopes,
                                                              final String codeVerifier) throws Exception {
        final MockHttpSession session = beginAuthorization(createdClient.clientId(),
                                                           redirectUri,
                                                           scopes,
                                                           codeVerifier);

        mockMvc.perform(post("/signUp")
                                .session(session)
                                .with(csrf())
                                .param("email", email)
                                .param("password", password))
               .andExpect(status().is3xxRedirection())
               .andExpect(header().string("Location", endsWith("/login")));

        final List<ClientUser> createdUsers = userService.findClientUsersByRegisteredClientIds(Set.of(createdClient.id()));
        assertThat(createdUsers).anySatisfy(clientUser -> {
            assertThat(clientUser.getEmail()).isEqualTo(email);
            assertThat(clientUser.getClientId()).isEqualTo(createdClient.clientId());
        });

        return completeAuthorizationAndCaptureCode(session,
                                                   createdClient.clientId(),
                                                   email,
                                                   password);
    }

    private MockHttpSession beginAuthorization(final String clientId,
                                               final String redirectUri,
                                               final Set<String> scopes,
                                               final String codeVerifier) throws Exception {
        final MultiValueMap<String, String> authorizeParams = new LinkedMultiValueMap<>();
        authorizeParams.add("response_type", "code");
        authorizeParams.add("client_id", clientId);
        authorizeParams.add("redirect_uri", redirectUri);
        authorizeParams.add("scope", String.join(" ", scopes));
        authorizeParams.add("state", UUID.randomUUID().toString());
        if (codeVerifier != null) {
            authorizeParams.add(MetaDataKeys.CODE_CHALLENGE.getValue(), sha256Base64Url(codeVerifier));
            authorizeParams.add(MetaDataKeys.CODE_CHALLENGE_METHOD.getValue(), "S256");
        }

        final MvcResult authorizeRedirect = mockMvc.perform(get(
                                                            UriComponentsBuilder.fromPath("/oauth2/authorize")
                                                                                .queryParams(authorizeParams)
                                                                                .build()
                                                                                .encode()
                                                                                .toUriString())
                                                            .accept(MediaType.TEXT_HTML))
                                                   .andExpect(status().is3xxRedirection())
                                                   .andExpect(header().string("Location", endsWith("/login")))
                                                   .andReturn();

        return (MockHttpSession) authorizeRedirect.getRequest().getSession(false);
    }

    private String completeAuthorizationAndCaptureCode(final MockHttpSession session,
                                                       final String clientId,
                                                       final String email,
                                                       final String password) throws Exception {
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

    private String sha256Base64Url(final String value) throws Exception {
        final MessageDigest messageDigest = java.security.MessageDigest.getInstance("SHA-256");
        final byte[] digest = messageDigest.digest(value.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private MvcResult issueClientCredentialsToken(final String clientId,
                                                  final String clientSecret,
                                                  final String scope) throws Exception {
        return mockMvc.perform(post("/oauth2/token")
                                       .with(httpBasic(clientId, clientSecret))
                                       .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                       .param(MetaDataKeys.GRANT_TYPE.getValue(), AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())
                                       .param(MetaDataKeys.SCOPE.getValue(), scope))
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.access_token").exists())
                      .andReturn();
    }

    private ClientUser saveClientUser(final String email,
                                      final String rawPassword,
                                      final String clientId) {
        return saveClientUser(email, rawPassword, clientId, Set.of("ROLE_USER"));
    }

    private ClientUser saveClientUser(final String email,
                                      final String rawPassword,
                                      final String clientId,
                                      final Set<String> authorityNames) {
        final ClientUser clientUser = ClientUser.from(email)
                                                .email(email)
                                                .clientId(clientId)
                                                .passwordHash(passwordEncoder.encode(rawPassword))
                                                .verified(true)
                                                .createdAt(LocalDateTime.now())
                                                .updatedAt(LocalDateTime.now())
                                                .userAttributes(new HashMap<>())
                                                .authorities(authorities -> authorityNames.forEach(authorityName -> authorities.add(Authority.from(authorityName))))
                                                .locked(false)
                                                .expired(false)
                                                .credentialsExpired(false)
                                                .build();
        return userService.saveUser(clientUser);
    }

    private void savePlatformUser(final String email,
                                  final String rawPassword) {
        savePlatformUser(email, rawPassword, "BASIC");
    }

    private void savePlatformUser(final String email,
                                  final String rawPassword,
                                  final String tierName) {
        ensureFrontendClientExists();
        final PlatformUser.PlatformUserBuilder builder = PlatformUser.from(email)
                                                                     .email(email)
                                                                     .passwordHash(passwordEncoder.encode(rawPassword))
                                                                     .verified(true)
                                                                     .createdAt(LocalDateTime.now())
                                                                     .updatedAt(LocalDateTime.now())
                                                                     .registeredClientIds(ids -> ids.clear())
                                                                     .authorities(authorities -> authorities.add(Authority.from("ROLE_PLATFORM_USER")))
                                                                     .locked(false)
                                                                     .expired(false)
                                                                     .credentialsExpired(false);
        if (tierName != null && !tierName.isBlank()) {
            builder.tier(resolvePlatformUserTier(tierName));
        }
        final PlatformUser platformUser = builder.build();
        userService.savePlatformUser(platformUser);
    }

    private PlatformUserTier resolvePlatformUserTier(final String tierName) {
        final PlatformUserTierEntity tierEntity = platformUserTierJpaRepository.findByTierNameIgnoreCase(tierName)
                                                                               .orElseThrow();
        return PlatformUserTier.builder()
                               .id(tierEntity.getTierId())
                               .stripePriceId(tierEntity.getStripPriceId())
                               .name(tierEntity.getTierName())
                               .price(tierEntity.getTierPrice())
                               .description(tierEntity.getTierDescription())
                               .tierOrder(tierEntity.getTierOrder())
                               .allowedNumberOfRegisteredClients(tierEntity.getAllowedNumberOfRegisteredClients())
                               .allowedNumberOfGlobalUsers(tierEntity.getAllowedNumberOfGlobalUsers())
                               .allowedNumberOfGlobalScopes(tierEntity.getAllowedNumberOfGlobalScopes())
                               .allowedNumberOfGlobalAuthorities(tierEntity.getAllowedNumberOfGlobalAuthorities())
                               .build();
    }

    private void ensureFrontendClientExists() {
        ensureClientExists(
                PLATFORM_CLIENT_ID,
                "http://localhost:9001/callback",
                Set.of("openid")
        );
        ensureClientExists(
                FRONTEND_CLIENT_ID,
                "http://localhost:9999/frontend/callback",
                Set.of("openid")
        );
    }

    private void ensureClientExists(final String clientId,
                                    final String redirectUri,
                                    final Set<String> scopes) {
        if (registerClientJpaRepository.findByClientId(clientId).isPresent()) {
            return;
        }
        saveRegisteredClient(registeredClientRequest(
                clientId,
                null,
                Set.of(ClientAuthenticationMethod.NONE),
                Set.of(AuthorizationGrantType.AUTHORIZATION_CODE),
                Set.of(redirectUri),
                scopes,
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
                0,
                "A starter production tier for smaller teams that need a few clients and enough room for regular user activity.",
                1,
                5,
                500,
                500,
                500
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
                org.springframework.security.oauth2.jwt.JwsHeader.with(SignatureAlgorithm.RS256).build(),
                claims
        )).getTokenValue();
    }

    private String uniqueEmail(final String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private String uniqueClientId(final String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private String mvcJson(final Object value) throws Exception {
        return requestMappingHandlerAdapter.getMessageConverters()
                                           .stream()
                                           .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                                           .map(MappingJackson2HttpMessageConverter.class::cast)
                                           .findFirst()
                                           .orElseThrow()
                                           .getObjectMapper()
                                           .writeValueAsString(value);
    }

    private void assertMvcCanReadRegisteredClientRequest(final String json) throws Exception {
        final ObjectMapper mvcMapper = requestMappingHandlerAdapter.getMessageConverters()
                                                                   .stream()
                                                                   .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                                                                   .map(MappingJackson2HttpMessageConverter.class::cast)
                                                                   .findFirst()
                                                                   .orElseThrow()
                                                                   .getObjectMapper();
        try {
            mvcMapper.readValue(json, RegisteredClientRequest.class);
        } catch (final Exception ex) {
            throw new IllegalStateException("MVC could not read update request JSON: " + ex.getMessage(), ex);
        }
    }

    private String plainJson(final Object value) throws Exception {
        return API_REQUEST_OBJECT_MAPPER.writeValueAsString(value);
    }

    private String registeredClientUpdateJson(final String clientName,
                                              final Set<String> redirectUris,
                                              final Set<String> postLogoutRedirectUris,
                                              final Set<String> scopes,
                                              final Set<String> authorities,
                                              final Set<String> roles,
                                              final Map<String, Object> clientSettings,
                                              final Map<String, Object> tokenSettings) throws Exception {
        final RegisteredClientRequest request = RegisteredClientRequest.builder()
                                                                      .clientName(clientName)
                                                                      .clientAuthenticationMethods(new LinkedHashSet<>())
                                                                      .authorizationGrantTypes(new LinkedHashSet<>())
                                                                      .redirectUris(redirectUris == null
                                                                                    ? new LinkedHashSet<>()
                                                                                    : new LinkedHashSet<>(redirectUris))
                                                                      .postLogoutRedirectUris(postLogoutRedirectUris == null
                                                                                              ? new LinkedHashSet<>()
                                                                                              : new LinkedHashSet<>(postLogoutRedirectUris))
                                                                      .scopes(scopes == null ? new LinkedHashSet<>() : new LinkedHashSet<>(scopes))
                                                                      .authorities(authorities == null
                                                                                   ? new LinkedHashSet<>()
                                                                                   : new LinkedHashSet<>(authorities))
                                                                      .roles(roles == null ? new LinkedHashSet<>() : new LinkedHashSet<>(roles))
                                                                      .clientSettings(clientSettings)
                                                                      .tokenSettings(tokenSettings)
                                                                      .build();
        return sanitizedMvcJson(request);
    }

    private String sanitizedMvcJson(final Object value) throws Exception {
        final JsonNode json = API_REQUEST_OBJECT_MAPPER.readTree(mvcJson(value));
        if (json instanceof ObjectNode objectNode) {
            sanitizeNestedTypeIds(objectNode, true);
        }
        return API_REQUEST_OBJECT_MAPPER.writeValueAsString(json);
    }

    private void sanitizeNestedTypeIds(final JsonNode node, final boolean keepCurrentTypeId) {
        if (node == null) {
            return;
        }

        if (node instanceof ObjectNode objectNode) {
            final JsonNode typeIdNode = objectNode.get("@class");
            final boolean preserveJavaUtilTypeId =
                    typeIdNode != null
                    && typeIdNode.isTextual()
                    && typeIdNode.asText().startsWith("java.util.");
            if (!keepCurrentTypeId && !preserveJavaUtilTypeId) {
                objectNode.remove("@class");
            }

            final List<String> fieldNames = new java.util.ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);
            for (final String fieldName : fieldNames) {
                sanitizeNestedTypeIds(objectNode.get(fieldName), false);
            }
            return;
        }

        if (node instanceof ArrayNode arrayNode) {
            if (arrayNode.size() == 2 && arrayNode.get(0).isTextual()) {
                final String typeId = arrayNode.get(0).asText();
                if (typeId.startsWith("java.util.ImmutableCollections$Set")) {
                    arrayNode.set(0, TextNode.valueOf(LinkedHashSet.class.getName()));
                }
            }

            for (final JsonNode child : arrayNode) {
                sanitizeNestedTypeIds(child, false);
            }
        }
    }

    private String typedRegisteredClientIdsJson(final Set<UUID> registeredClientIds) {
        return registeredClientIds == null
               ? "[]"
               : "[" + registeredClientIds.stream()
                                          .map(UUID::toString)
                                          .map(id -> "\"" + id + "\"")
                                          .collect(Collectors.joining(",")) + "]";
    }

    private String typedStringSetJson(final Set<String> values) throws Exception {
        final String joinedValues = values.stream()
                                          .map(value -> {
                                              try {
                                                  return plainJson(value);
                                              } catch (final Exception e) {
                                                  throw new IllegalStateException(e);
                                              }
                                          })
                                          .collect(Collectors.joining(","));
        return "[\"java.util.LinkedHashSet\",[" + joinedValues + "]]";
    }

    private JsonNode unwrapCollectionResponse(final MvcResult mvcResult) throws Exception {
        final JsonNode response = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        if (response.isArray() && response.size() == 2 && response.get(0).isTextual() && response.get(1).isArray()) {
            return response.get(1);
        }
        return response;
    }

    private JsonNode unwrapCollectionNode(final JsonNode response) {
        if (response != null && response.isArray() && response.size() == 2 && response.get(0).isTextual() && response.get(1).isArray()) {
            return response.get(1);
        }
        return response;
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
                                      .redirectUris(redirectUris == null
                                                    ? new LinkedHashSet<>()
                                                    : new LinkedHashSet<>(redirectUris))
                                      .postLogoutRedirectUris(new LinkedHashSet<>())
                                      .scopes(scopes == null ? new LinkedHashSet<>() : new LinkedHashSet<>(scopes))
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

    private ClientSettings clientSettings(final boolean requireProofKey) {
        return ClientSettings.builder()
                             .requireProofKey(requireProofKey)
                             .requireAuthorizationConsent(false)
                             .build();
    }

    private HashMap<String, Object> clientSettingsRequest(final boolean requireProofKey) {
        return new HashMap<>(clientSettings(requireProofKey).getSettings());
    }

    private HashMap<String, Object> tokenSettingsRequest() {
        final HashMap<String, Object> tokenSettings = new HashMap<>();
        tokenSettings.put("reuseRefreshTokens", true);
        return tokenSettings;
    }

    private record RegisteredClientFixture(RegisteredClientModel registeredClient, String rawClientSecret) {
    }

    private record CreatedPlatformClient(UUID id,
                                         String clientId,
                                         String rawClientSecret,
                                         RegisteredClientModel registeredClient) {
    }

}
