package io.github.blakedunaway.authserver.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.blakedunaway.authserver.business.service.SigningKeyStore;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain asChain(final HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer as = OAuth2AuthorizationServerConfigurer.authorizationServer();
        RequestMatcher endpoints = as.getEndpointsMatcher();

        http.securityMatcher(endpoints)
            .with(as, c -> c.oidc(Customizer.withDefaults()))
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        return http.build();
    }

    /*
    Chain for the servers API
     */
    @Bean
    @Order(2)
    SecurityFilterChain resourceApi(HttpSecurity http, JwtDecoder resourceJwtDecoder) throws Exception {
        http.securityMatcher("/rest/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth
                    .jwt(jwt -> jwt.decoder(resourceJwtDecoder))
            )
            .sessionManagement(sm ->
                                       sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        return http.build();
    }

    /*
    Decoder intended only for the servers API
     */
    @Bean
    JwtDecoder resourceJwtDecoder(
            JWKSource<SecurityContext> jwkSource,
            AuthorizationServerSettings settings,
            AppSignatureValidator appSignatureValidator
    ) {
        NimbusJwtDecoder decoder =
                (NimbusJwtDecoder) OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);

        OAuth2TokenValidator<Jwt> issuer =
                JwtValidators.createDefaultWithIssuer(settings.getIssuer());

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(issuer, appSignatureValidator)
        );

        return decoder;
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                                          .build();
    }

    @Bean
    JWKSource<SecurityContext> javaWebKeySource(final SigningKeyStore signingKeyStore) {
        return signingKeyStore.jwkSource();
    }

    @Bean
    JwtDecoder jwtSelfVerifier(final JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    JwtEncoder javaWebTokenEncoder(final JWKSource<SecurityContext> javaWebKeySource) {
        return new NimbusJwtEncoder(javaWebKeySource);
    }

    @Bean
    RequestCache requestCache() {
        return new HttpSessionRequestCache();
    }

    @Bean
    Jackson2ObjectMapperBuilderCustomizer oauth2SecurityJackson() {
        return builder -> {
            final List<com.fasterxml.jackson.databind.Module> securityModules = SecurityJackson2Modules.getModules(
                    SecurityConfig.class.getClassLoader());
            builder.modules(securityModules);

            builder.modulesToInstall(
                    OAuth2ClientJackson2Module.class,
                    OAuth2AuthorizationServerJackson2Module.class,
                    JavaTimeModule.class
            );
        };
    }

}
