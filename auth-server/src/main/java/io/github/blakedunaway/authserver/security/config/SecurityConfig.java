package io.github.blakedunaway.authserver.security.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

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

    @Bean
    CorsConfiguration corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return config;
    }

}
