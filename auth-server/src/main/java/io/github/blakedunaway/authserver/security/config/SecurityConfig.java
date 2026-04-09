package io.github.blakedunaway.authserver.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @Qualifier("securityObjectMapper")
    ObjectMapper securityObjectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final List<com.fasterxml.jackson.databind.Module> securityModules =
                SecurityJackson2Modules.getModules(SecurityConfig.class.getClassLoader());
        objectMapper.registerModules(securityModules);
        objectMapper.registerModule(new OAuth2ClientJackson2Module());
        objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    @Bean
    CorsConfiguration authorizationServerCorsConfiguration() {
        final CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(false);
        config.addAllowedOriginPattern("*");
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        return config;
    }

    @Bean
    CorsConfiguration platformCorsConfiguration(@Value("${auth-server.platform.allowed-origins:http://localhost:5173}") final String allowedOriginsProperty) {
        final CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(Arrays.stream(allowedOriginsProperty.split(","))
                                       .map(String::trim)
                                       .filter(origin -> !origin.isEmpty())
                                       .collect(Collectors.toList()));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        return config;
    }

}
