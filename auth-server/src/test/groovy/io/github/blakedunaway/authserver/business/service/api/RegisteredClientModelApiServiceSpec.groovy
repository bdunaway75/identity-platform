package io.github.blakedunaway.authserver.business.service.api

import com.blakedunaway.iamclientapi.api.dto.RegisteredClientDto
import io.github.blakedunaway.authserver.TestSpec
import io.github.blakedunaway.authserver.config.TestConfig
import io.github.blakedunaway.authserver.integration.repository.jpa.RegisterClientJpaRepository
import io.github.blakedunaway.authserver.mapper.RegisteredClientMapper
import jakarta.ws.rs.core.Response
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

import java.time.Duration
import java.time.LocalDateTime

@Import(TestConfig)
class RegisteredClientModelApiServiceSpec extends TestSpec {

    @Autowired
    RegisteredClientApiService service

    @Autowired
    RegisterClientJpaRepository repository

    @Autowired
    RegisteredClientMapper mapper

    def "public client registered"() {

        given:
        def client =
                RegisteredClientDto.builder()
                                   .clientAuthenticationMethods(Set.of("none"))
                                   .authorizationGrantTypes(Set.of("authorization_code"))
                                   .clientId("123")
                                   .clientIdIssuedAt(LocalDateTime.now())
                                   .clientSecretExpiresAt(LocalDateTime.now())
                                   .tokenSettings(Map.of("authorization-code-time-to-live", Duration.ofMinutes(10)))
                                   .clientSettings(Map.of("settings.client.require-proof-key", true))
                                   .redirectUris(Set.of("https://redirect/api"))
                                   .clientName("test")
                                   .build()
        when:
        def result = service.registerClient(client)

        then:
        result.getStatus() == 201
        repository.findByClientId("123").isPresent()
    }

    def "confidential client registered"() {
        given:
        def client =
                RegisteredClientDto.builder()
                                   .clientAuthenticationMethods(Set.of("client_secret_basic"))
                                   .authorizationGrantTypes(Set.of("authorization_code"))
                                   .clientId("1234")
                                   .clientIdIssuedAt(LocalDateTime.now())
                                   .clientSecretExpiresAt(LocalDateTime.now())
                                   .tokenSettings(Map.of("authorization-code-time-to-live", Duration.ofMinutes(10)))
                                   .clientSettings(Map.of("settings.client.require-proof-key", true))
                                   .redirectUris(Set.of("https://redirect/api"))
                                   .clientName("test")
                                   .build()
        when:
        def result = service.registerClient(client)

        then:
        result.getStatus() == 201
        repository.findByClientId("123").isPresent()
    }

    def "public client failed"() {
        given:
        def client =
                RegisteredClientDto.builder()
                                   .clientAuthenticationMethods(Set.of("none"))
                                   .authorizationGrantTypes(Set.of("client_credentials"))
                                   .clientIdIssuedAt(LocalDateTime.now())
                                   .clientSecretExpiresAt(LocalDateTime.now())
                                   .tokenSettings(Map.of("authorization-code-time-to-live", Duration.ofMinutes(10)))
                                   .clientSettings(null)
                                   .redirectUris(Set.of("https://redirect/api"))
                                   .clientName("test")
                                   .build()
        when:
        def result = service.registerClient(client)

        then:
        result.status == Response.Status.BAD_REQUEST.statusCode
    }

    def "confidential client failed"() {
        given:
        def client =
                RegisteredClientDto.builder()
                                   .clientAuthenticationMethods(Set.of("client_secret_basic"))
                                   .authorizationGrantTypes(Set.of("client_credentials"))
                                   .clientId("123")
                                   .clientIdIssuedAt(LocalDateTime.now())
                                   .clientSecretExpiresAt(LocalDateTime.now())
                                   .tokenSettings(Map.of("authorization-code-time-to-live", Duration.ofMinutes(10)))
                                   .clientSettings(Map.of("settings.client.require-proof-key", true))
                                   .redirectUris(null)
                                   .clientName("test")
                                   .build()
        when:
        def result = service.registerClient(client)

        then:
        result.status == Response.Status.BAD_REQUEST.statusCode
    }

    def "confidential client update"() {
        given:
        def client =
                RegisteredClientDto.builder()
                                   .clientSecret("lol")
                                   .clientAuthenticationMethods(Set.of("none"))
                                   .authorizationGrantTypes(Set.of("client_credentials"))
                                   .clientId("12345")
                                   .clientIdIssuedAt(LocalDateTime.now())
                                   .clientSecretExpiresAt(LocalDateTime.now())
                                   .tokenSettings(Map.of("authorization-code-time-to-live", Duration.ofMinutes(10)))
                                   .clientSettings(Map.of("settings.client.require-proof-key", false))
                                   .redirectUris(null)
                                   .clientName("test")
                                   .build()
        when:
        service.registerClient(client)
        def result = service.updateClient(client.toBuilder().clientName("test2").build())

        then:
        result.status == Response.Status.OK.statusCode
        repository.findByClientId(client.getClientId()).isPresent()
        repository.findByClientId(client.getClientId()).get().getClientName().equals("test2")
    }

    def "confidential client update orphan removal"() {
        given:
        def client =
                RegisteredClientDto.builder()
                                   .clientAuthenticationMethods(Set.of("non"))
                                   .clientSecret("lol1")
                                   .authorizationGrantTypes(Set.of("client_credentials"))
                                   .clientId("123456")
                                   .clientIdIssuedAt(LocalDateTime.now())
                                   .clientSecretExpiresAt(LocalDateTime.now())
                                   .tokenSettings(Map.of("authorization-code-time-to-live", Duration.ofMinutes(10)))
                                   .clientSettings(Map.of("settings.client.require-proof-key", false))
                                   .redirectUris(null)
                                   .clientName("test")
                                   .build()
        when:
        service.registerClient(client)
        def result = service.updateClient(client.toBuilder().tokenSettings(null).build())

        then:
        result.status == Response.Status.OK.statusCode
        repository.findByClientId(client.getClientId()).isPresent()
        repository.findByClientId(client.getClientId()).get().getTokenSettings().size() > client.getTokenSettings().size()
    }
}
