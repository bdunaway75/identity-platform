package io.github.blakedunaway.authserver.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class AuthorizationServerSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain asChain(final HttpSecurity http,
                                @Qualifier("authorizationServerCorsConfiguration") final CorsConfiguration corsConfiguration) throws Exception {
        OAuth2AuthorizationServerConfigurer as = OAuth2AuthorizationServerConfigurer.authorizationServer();

        http.securityMatcher(as.getEndpointsMatcher())
            .cors(cors -> cors.configurationSource(request -> corsConfiguration))
            .formLogin(form ->
                               form.loginPage("/login")
                                   .loginProcessingUrl("/login")
                                   .failureUrl("/login?error=true")
                                   .permitAll())
            .with(as, (server) -> {
                server.oidc(Customizer.withDefaults());
                server.authorizationEndpoint(authorization ->
                                                     authorization.errorResponseHandler((request, response, exception) -> {
                                                         response.sendRedirect(
                                                                 "/oauth-error?error=" + URLEncoder.encode(String.valueOf(response.getStatus()),
                                                                                                           StandardCharsets.UTF_8) +
                                                                 "&error_description=" + URLEncoder.encode(exception.getMessage(),
                                                                                                           StandardCharsets.UTF_8)
                                                         );
                                                     })
                );

            })
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        return http.build();
    }

}
