package io.github.blakedunaway.authserver.config;

import io.github.blakedunaway.authserver.business.service.SigningKeyStore;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeRequestAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationConsentAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
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
        var as = OAuth2AuthorizationServerConfigurer.authorizationServer();
        var endpoints = as.getEndpointsMatcher();

        http.securityMatcher(endpoints)
            .with(as, c -> c.oidc(Customizer.withDefaults()))
            .authorizeHttpRequests(a -> a.anyRequest().authenticated())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
            .csrf(csrf -> csrf.ignoringRequestMatchers(endpoints));

        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain appChain(final HttpSecurity http, RequestCache requestCache) throws Exception {
        http.authorizeHttpRequests(a -> a
                    .requestMatchers("/login", "/ui/**", "/assets/**").permitAll()
                    .anyRequest().authenticated())
            .formLogin(Customizer.withDefaults())
            .requestCache(rc -> rc.requestCache(requestCache))
            .headers(h -> h
                    .frameOptions(f -> f.sameOrigin())
                    .xssProtection(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                                          // Set your canonical issuer URL. Must match what clients/resource servers expect.
                                          .issuer("https://auth.example.com")
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
