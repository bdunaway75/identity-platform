package io.github.blakedunaway.authserver.security.config;

import io.github.blakedunaway.authserver.business.model.enums.MetaDataKeys;
import jakarta.ws.rs.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class ResourceServerSecurityConfig {

    private static final String PLATFORM_CLIENT_ID = "identity-platform";

    /*Chain for the servers API*/
    @Order(2)
    @Bean
    SecurityFilterChain resourceApi(final HttpSecurity http,
                                    final CorsConfiguration corsConfiguration,
                                    final JwtDecoder jwtDecoder) throws Exception {
        http.securityMatcher("/platform/**")
            .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.OPTIONS, "/platform/**")
                                               .permitAll()
                                               .anyRequest()
                                               .access((authentication, context) -> {
                                                   final Object principal = authentication.get().getPrincipal();
                                                   if (!(principal instanceof final Jwt jwt)) {
                                                       return new org.springframework.security.authorization.AuthorizationDecision(false);
                                                   }

                                                   final String azp = jwt.getClaimAsString(MetaDataKeys.AZP.getValue());
                                                   return new org.springframework.security.authorization.AuthorizationDecision(
                                                           PLATFORM_CLIENT_ID.equalsIgnoreCase(azp)
                                                   );
                                               }))
            .cors(cors -> cors.configurationSource(request -> corsConfiguration))
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(eh -> eh.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint()))
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(jwtDecoder)
                                                               .jwtAuthenticationConverter(platformJwtAuthenticationConverter())))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> platformJwtAuthenticationConverter() {
        final JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        final JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            final Set<GrantedAuthority> authorities = new LinkedHashSet<>(scopeAuthoritiesConverter.convert(jwt));
            final List<String> claimedAuthorities = jwt.getClaimAsStringList("authorities");
            if (claimedAuthorities != null) {
                claimedAuthorities.stream()
                                  .filter(authority -> authority != null && !authority.isBlank())
                                  .map(authority -> authority.toUpperCase())
                                  .map(SimpleGrantedAuthority::new)
                                  .forEach(authorities::add);
            }
            return authorities;
        });
        return jwtAuthenticationConverter;
    }

}
