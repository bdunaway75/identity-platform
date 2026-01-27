package io.github.blakedunaway.authserver.security.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ResourceServerSecurityConfig {

    /*
   Chain for the servers API
    */
    @Order(2)
    @Bean
    SecurityFilterChain resourceApi(HttpSecurity http, @Qualifier("resource") JwtDecoder resourceJwtDecoder)
            throws Exception {
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

}
