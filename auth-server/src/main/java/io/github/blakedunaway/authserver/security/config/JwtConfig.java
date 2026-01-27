package io.github.blakedunaway.authserver.security.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.blakedunaway.authserver.business.service.SigningKeyStore;
import io.github.blakedunaway.authserver.security.token.AppSignatureValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

@Configuration
public class JwtConfig {

    @Bean(name = "resource")
    JwtDecoder resourceJwtDecoder(JWKSource<SecurityContext> jwkSource,
                                  AuthorizationServerSettings settings,
                                  AppSignatureValidator appSignatureValidator) {
        NimbusJwtDecoder decoder = (NimbusJwtDecoder) OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);

        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(settings.getIssuer());

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, appSignatureValidator));

        return decoder;
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().issuer("http://localhost:8080").build();
    }

    @Bean
    JWKSource<SecurityContext> javaWebKeySource(final SigningKeyStore signingKeyStore) {
        return signingKeyStore.jwkSource();
    }

    @Bean
    @Primary
    JwtDecoder jwtSelfVerifier(final JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    JwtEncoder javaWebTokenEncoder(final JWKSource<SecurityContext> javaWebKeySource) {
        return new NimbusJwtEncoder(javaWebKeySource);
    }


}
