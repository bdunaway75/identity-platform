package io.github.blakedunaway.authserver.security.config;

import com.stripe.StripeClient;
import com.stripe.param.v2.core.AccountCreateParams;
import io.github.blakedunaway.authserver.business.model.enums.MetaDataKeys;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class ResourceServerSecurityConfig {

    @Value("${auth-server.frontend.client-id}")
    private String platformClientId;

    /*Chain for the servers API*/
    @Order(2)
    @Bean
    SecurityFilterChain resourceApi(final HttpSecurity http,
                                    @Qualifier("platformCorsConfiguration") final CorsConfiguration corsConfiguration,
                                    final JwtDecoder jwtDecoder) throws Exception {
        http.securityMatcher("/platform/**")
            .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.OPTIONS, "/platform/**")
                                               .permitAll()
                                               .requestMatchers("/platform/login",
                                                                "/platform/signUp",
                                                                "/platform/demo-access-code/**",
                                                                "/platform/credentials-expired",
                                                                "/platform/subscription-status",
                                                                "/platform/billing-webhook")
                                               .permitAll()
                                               .anyRequest()
                                               .access((authentication, context) -> {
                                                   final Object authenticated = authentication.get();
                                                   if (!(authenticated instanceof final JwtAuthenticationToken jwtAuthenticationToken)) {
                                                       return new AuthorizationDecision(false);
                                                   }
                                                   final Jwt jwt = jwtAuthenticationToken.getToken();
                                                   final String azp = jwt.getClaimAsString(MetaDataKeys.AZP.getValue());
                                                   return new AuthorizationDecision(platformClientId.equalsIgnoreCase(azp));
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
            final Set<GrantedAuthority> authorities = new HashSet<>(scopeAuthoritiesConverter.convert(jwt));
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

    @Bean
    StripeClient stripeClient(@Value("${stripe.secret-key}") String secretKey) {
        return StripeClient.builder()
                           .setApiKey(secretKey)
                           .build();
    }

}
