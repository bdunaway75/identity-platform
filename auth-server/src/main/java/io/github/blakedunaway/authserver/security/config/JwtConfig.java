package io.github.blakedunaway.authserver.security.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.blakedunaway.authserver.business.model.enums.MetaDataKeys;
import io.github.blakedunaway.authserver.business.service.SigningKeyStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

@Configuration
public class JwtConfig {

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

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return context -> {
            final String clientId = context.getRegisteredClient().getClientId();
            context.getClaims().claim(MetaDataKeys.AZP.getValue(), clientId);

            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                final Authentication principal = context.getPrincipal();
                if (principal != null && principal.getAuthorities() != null && !principal.getAuthorities().isEmpty()) {
                    context.getClaims().claim("authorities",
                                              principal.getAuthorities()
                                                       .stream()
                                                       .map(grantedAuthority -> grantedAuthority.getAuthority().toUpperCase())
                                                       .distinct()
                                                       .toList());
                }
            }
        };
    }


}
