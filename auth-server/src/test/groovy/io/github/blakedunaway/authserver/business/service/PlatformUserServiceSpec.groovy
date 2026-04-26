package io.github.blakedunaway.authserver.business.service

import io.github.blakedunaway.authserver.TestSpec
import io.github.blakedunaway.authserver.business.model.Authority
import io.github.blakedunaway.authserver.business.model.RegisteredClientModel
import io.github.blakedunaway.authserver.business.model.user.ClientUser
import io.github.blakedunaway.authserver.business.model.user.PlatformRegisterDto
import io.github.blakedunaway.authserver.business.model.user.PlatformUser
import io.github.blakedunaway.authserver.business.model.user.PlatformUserTier
import io.github.blakedunaway.authserver.config.redis.RedisStore
import io.github.blakedunaway.authserver.integration.TokenSettingsJson
import io.github.blakedunaway.authserver.integration.entity.PlatformUserTierEntity
import io.github.blakedunaway.authserver.integration.entity.RegisteredClientEntity
import io.github.blakedunaway.authserver.integration.repository.jpa.AuthorityJpaRepository
import io.github.blakedunaway.authserver.integration.repository.jpa.PlatformUserTierJpaRepository
import io.github.blakedunaway.authserver.integration.repository.jpa.RegisterClientJpaRepository
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.test.annotation.DirtiesContext
import jakarta.validation.ValidationException

import java.time.LocalDateTime

@Import([UserService, RegisteredClientService])
class PlatformUserServiceSpec extends TestSpec {

    private static final String HASH = "\$argon2id\$v=19\$m=65536,t=2,p=1\$deadbeef\$deadbeef"
    private static final String FRONTEND_CLIENT_ID = "identity-platform"

    @Autowired
    private UserService service

    @Autowired
    private RegisteredClientService registeredClientService

    @Autowired
    private RegisterClientJpaRepository registerClientJpaRepository

    @Autowired
    private AuthorityJpaRepository authorityJpaRepository

    @Autowired
    private PlatformUserTierJpaRepository platformUserTierJpaRepository

    @Autowired
    private PasswordEncoder passwordEncoder

    @SpringBean
    private RedisStore redisStore = Mock()

    def setup() {
        redisStore.get(_ as String) >> [:]
        ensureFrontendClient()
        ensureFreeTier()
    }

    @DirtiesContext
    def "signUpPlatformUser persists hashed user and frontend-scoped role"() {
        given:
        def request = new PlatformRegisterDto("platform@example.com", "Password123!")

        when:
        def saved = service.signUpPlatformUser(request)

        then:
        saved != null
        saved.getEmail() == "platform@example.com"
        saved.getPasswordHash().startsWith("\$argon2")
        saved.getAuthorities()*.getName() as Set == ["ROLE_PLATFORM_USER"] as Set
        saved.getTier() != null
        saved.getTier().getName() == "FREE"

        and:
        def loaded = service.loadPlatformUserByEmail("platform@example.com")
        loaded != null
        loaded.getAuthorities()*.getName() as Set == ["ROLE_PLATFORM_USER"] as Set
    }

    @DirtiesContext
    def "savePlatformUser persists platform authorities against the frontend client"() {
        given:
        def platformUser = PlatformUser.from("owner@example.com")
                                       .passwordHash(HASH)
                                       .verified(true)
                                       .createdAt(LocalDateTime.now())
                                       .updatedAt(LocalDateTime.now())
                                       .authorities { auths ->
                                           auths.clear()
                                           auths.add(Authority.from("ROLE_PLATFORM_ADMIN"))
                                           auths.add(Authority.from("ROLE_PLATFORM_USER"))
                                       }
                                       .registeredClientIds { it.clear() }
                                       .locked(false)
                                       .expired(false)
                                       .credentialsExpired(false)
                                       .build()

        when:
        def saved = service.savePlatformUser(platformUser)

        then:
        saved != null
        saved.getAuthorities()*.getName() as Set == ["ROLE_PLATFORM_ADMIN", "ROLE_PLATFORM_USER"] as Set

        and:
        authorityJpaRepository.findAllByRegisteredClient_ClientIdAndNameIn(
                FRONTEND_CLIENT_ID,
                ["ROLE_PLATFORM_ADMIN", "ROLE_PLATFORM_USER"]
        )*.getName() as Set == ["ROLE_PLATFORM_ADMIN", "ROLE_PLATFORM_USER"] as Set
    }

    @DirtiesContext
    def "loadPlatformUserDetailsByEmail returns spring user details with platform authorities"() {
        given:
        service.savePlatformUser(
                PlatformUser.from("details@example.com")
                            .passwordHash(HASH)
                            .verified(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .authorities { auths ->
                                auths.clear()
                                auths.add(Authority.from("ROLE_PLATFORM_USER"))
                            }
                            .registeredClientIds { it.clear() }
                            .locked(false)
                            .expired(false)
                            .credentialsExpired(false)
                            .build()
        )

        when:
        UserDetails details = service.loadPlatformUserDetailsByEmail("details@example.com")

        then:
        details != null
        details.getUsername() == "details@example.com"
        details.getAuthorities()*.getAuthority() as Set == ["ROLE_PLATFORM_USER"] as Set
    }

    @DirtiesContext
    def "attachRegisteredClientToPlatformUser adds owned client ids and ownership helpers reflect them"() {
        given:
        def firstClient = saveClient("owned-client-one")
        def secondClient = saveClient("owned-client-two")
        def savedPlatformUser = service.savePlatformUser(
                PlatformUser.from("attach@example.com")
                            .passwordHash(HASH)
                            .verified(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .authorities { auths ->
                                auths.clear()
                                auths.add(Authority.from("ROLE_PLATFORM_USER"))
                            }
                            .registeredClientIds { it.clear() }
                            .locked(false)
                            .expired(false)
                            .credentialsExpired(false)
                            .build()
        )
        service.attachRegisteredClientToPlatformUser("attach@example.com", firstClient.getId())
        service.attachRegisteredClientToPlatformUser("attach@example.com", secondClient.getId())
        when:
        def attached = service.loadPlatformUserByEmail("attach@example.com")

        then:
        attached != null
        attached.getRegisteredClientIds() as Set == [firstClient.getId(), secondClient.getId()] as Set
        service.filterOwnedRegisteredClientIds(
                attached,
                [firstClient.getId(), UUID.randomUUID()] as Set
        ) == [firstClient.getId()] as Set

        and:
        savedPlatformUser != null
    }

    @DirtiesContext
    def "savePlatformUser rejects registered clients that exceed the tier allowance"() {
        given:
        def firstClient = saveClient("limited-client-one")
        def secondClient = saveClient("limited-client-two")
        platformUserTierJpaRepository.save(new PlatformUserTierEntity(null, null, "LIMITED", 0, "limited tier", 1, 1, 500, 500, 500))

        def platformUser = PlatformUser.from("limited@example.com")
                                       .passwordHash(HASH)
                                       .verified(true)
                                       .createdAt(LocalDateTime.now())
                                       .updatedAt(LocalDateTime.now())
                                       .tier(PlatformUserTier.builder().name("LIMITED").build())
                                       .authorities { auths ->
                                           auths.clear()
                                           auths.add(Authority.from("ROLE_PLATFORM_USER"))
                                       }
                                       .registeredClientIds { ids ->
                                           ids.clear()
                                           ids.add(firstClient.getId())
                                           ids.add(secondClient.getId())
                                       }
                                       .locked(false)
                                       .expired(false)
                                       .credentialsExpired(false)
                                       .build()

        when:
        service.savePlatformUser(platformUser)

        then:
        thrown(ValidationException)
    }

    @DirtiesContext
    def "savePlatformUser rejects registered clients whose scopes or authorities exceed the tier allowance"() {
        given:
        def limitedClient = registeredClientService.saveRegisteredClient(
                RegisteredClientModel.builder()
                                     .id(null)
                                     .clientId(null)
                                     .clientName("scope-heavy")
                                     .clientSecret("secret")
                                     .clientAuthenticationMethods(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC))
                                     .authorizationGrantTypes(Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS))
                                     .tokenSettings(TokenSettings.builder().build())
                                     .clientSettings(ClientSettings.builder().build())
                                     .clientIdIssuedAt(LocalDateTime.now())
                                     .clientSecretExpiresAt(LocalDateTime.now())
                                     .postLogoutRedirectUris(Set.of("https://test.com/logout"))
                                     .scopes(Set.of("read", "write"))
                                     .redirectUris(Set.of("https://test.com/callback"))
                                     .authorities(Set.of("PERM_ONE"))
                                     .roles(Set.of())
                                     .build()
        )
        platformUserTierJpaRepository.save(new PlatformUserTierEntity(null, null, "TIGHT", 0, "tight tier", 1, 5, 500, 1, 0))

        def platformUser = PlatformUser.from("tight@example.com")
                                       .passwordHash(HASH)
                                       .verified(true)
                                       .createdAt(LocalDateTime.now())
                                       .updatedAt(LocalDateTime.now())
                                       .tier(PlatformUserTier.builder().name("TIGHT").build())
                                       .authorities { auths ->
                                           auths.clear()
                                           auths.add(Authority.from("ROLE_PLATFORM_USER"))
                                       }
                                       .registeredClientIds { ids ->
                                           ids.clear()
                                           ids.add(limitedClient.getId())
                                       }
                                       .locked(false)
                                       .expired(false)
                                       .credentialsExpired(false)
                                       .build()

        when:
        service.savePlatformUser(platformUser)

        then:
        thrown(ValidationException)
    }

    @DirtiesContext
    def "updateExpiredPlatformUserPassword rehashes password and clears expired flags"() {
        given:
        def email = "expired-platform@example.com"
        def oldPassword = "Password123!"
        def newPassword = "UpdatedPassword123!"
        service.savePlatformUser(
                PlatformUser.from(email)
                            .passwordHash(passwordEncoder.encode(oldPassword))
                            .verified(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .authorities { auths ->
                                auths.clear()
                                auths.add(Authority.from("ROLE_PLATFORM_USER"))
                            }
                            .registeredClientIds { it.clear() }
                            .locked(false)
                            .expired(true)
                            .credentialsExpired(true)
                            .build()
        )

        when:
        def updated = service.updateExpiredPlatformUserPassword(email, oldPassword, newPassword)
        def reloaded = service.loadPlatformUserByEmail(email)

        then:
        updated
        reloaded != null
        passwordEncoder.matches(newPassword, reloaded.getPasswordHash())
        !passwordEncoder.matches(oldPassword, reloaded.getPasswordHash())
        !reloaded.isExpired()
        !reloaded.isCredentialsExpired()
    }

    @DirtiesContext
    def "updateExpiredClientUserPassword rehashes password and clears expired flags"() {
        given:
        def registeredClient = saveClient("expired-client-password-reset")
        def email = "expired-client@example.com"
        def oldPassword = "Password123!"
        def newPassword = "UpdatedPassword123!"
        service.saveUser(
                ClientUser.from(email)
                          .passwordHash(passwordEncoder.encode(oldPassword))
                          .clientId(registeredClient.getClientId())
                          .verified(true)
                          .createdAt(LocalDateTime.now())
                          .updatedAt(LocalDateTime.now())
                          .userAttributes([:])
                          .authorities { auths ->
                              auths.clear()
                              auths.add(Authority.from("ROLE_USER"))
                          }
                          .locked(false)
                          .expired(true)
                          .credentialsExpired(true)
                          .build()
        )

        when:
        def updated = service.updateExpiredClientUserPassword(
                registeredClient.getClientId(),
                email,
                oldPassword,
                newPassword
        )
        def reloaded = service.loadUserDetailsByEmailAndClientId(registeredClient.getClientId(), email)

        then:
        updated
        reloaded != null
        passwordEncoder.matches(newPassword, reloaded.getPassword())
        !passwordEncoder.matches(oldPassword, reloaded.getPassword())
        reloaded.isAccountNonExpired()
        reloaded.isCredentialsNonExpired()
    }

    @DirtiesContext
    def "attachRegisteredClientToPlatformUser throws when platform user is missing"() {
        given:
        def registeredClient = saveClient("missing-owner-client")

        when:
        service.attachRegisteredClientToPlatformUser("missing@example.com", registeredClient.getId())

        then:
        thrown(UsernameNotFoundException)
    }

    private void ensureFrontendClient() {
        if (registerClientJpaRepository.findByClientId(FRONTEND_CLIENT_ID).present) {
            return
        }

        registerClientJpaRepository.save(
                RegisteredClientEntity.create(
                        null,
                        FRONTEND_CLIENT_ID,
                        LocalDateTime.now(),
                        HASH,
                        LocalDateTime.now().plusDays(30),
                        "frontend-client",
                        [:],
                        TokenSettingsJson.builder().build()
                )
        )
    }

    private void ensureFreeTier() {
        if (platformUserTierJpaRepository.findByTierNameIgnoreCase("FREE").present) {
            return
        }

        platformUserTierJpaRepository.save(new PlatformUserTierEntity(null, null, "FREE", 0, "free tier", 1, 5, 500, 500, 500))
    }

    private RegisteredClientModel saveClient(final String clientName) {
        return registeredClientService.saveRegisteredClient(
                RegisteredClientModel.builder()
                                     .id(null)
                                     .clientId(null)
                                     .clientName(clientName)
                                     .clientSecret("secret")
                                     .clientAuthenticationMethods(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC))
                                     .authorizationGrantTypes(Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS))
                                     .tokenSettings(TokenSettings.builder().build())
                                     .clientSettings(ClientSettings.builder().build())
                                     .clientIdIssuedAt(LocalDateTime.now())
                                     .clientSecretExpiresAt(LocalDateTime.now())
                                     .postLogoutRedirectUris(Set.of("https://test.com/logout"))
                                     .scopes(Set.of("read"))
                                     .redirectUris(Set.of("https://test.com/callback"))
                                     .authorities(Set.of())
                                     .roles(Set.of())
                                     .build()
        )
    }
}
