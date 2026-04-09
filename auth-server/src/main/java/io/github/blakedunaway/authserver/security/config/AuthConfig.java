package io.github.blakedunaway.authserver.security.config;

import io.github.blakedunaway.authserver.security.provider.ClientAwareDaoAuthProvider;
import io.github.blakedunaway.authserver.security.provider.PlatformUserDaoAuthProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
public class AuthConfig {

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    RequestCache requestCache() {
        return new HttpSessionRequestCache();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler(RequestCache requestCache) {
        SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler();
        handler.setRequestCache(requestCache);
        handler.setDefaultTargetUrl("/login");
        return handler;
    }

    @Bean
    public AuthenticationFailureHandler failureHandler() {
        return (request, response, exception) -> {
            final String requestUri = request.getRequestURI();
            final boolean platformFlow = "/platform/login".equals(request.getServletPath())
                                         || (requestUri != null && requestUri.endsWith("/platform/login"));

            if (exception instanceof CredentialsExpiredException) {
                final String credentialsPath = platformFlow ? "/platform/credentials-expired" : "/credentials-expired";
                final UriComponentsBuilder builder = UriComponentsBuilder.fromPath(credentialsPath)
                                                                         .queryParam("email", request.getParameter("email"))
                                                                         .queryParam("error", "Your password has expired. Choose a new one to continue.");
                final String clientId = request.getParameter("clientId");
                if (!platformFlow && clientId != null && !clientId.isBlank()) {
                    builder.queryParam("clientId", clientId);
                }
                response.sendRedirect(builder.encode().build().toUriString());
                return;
            }

            final String errorMessage;
            if (exception instanceof LockedException) {
                errorMessage = "Your account is locked.";
            } else if (exception instanceof DisabledException) {
                errorMessage = "Your account is disabled.";
            } else if (exception instanceof AccountExpiredException) {
                errorMessage = "Your account has expired.";
            } else {
                errorMessage = "We could not sign you in with those credentials. Please try again.";
            }

            final UriComponentsBuilder builder = UriComponentsBuilder.fromPath(platformFlow ? "/platform/login" : "/login")
                                                                     .queryParam("error", errorMessage);
            final String clientId = request.getParameter("clientId");
            if (!platformFlow && clientId != null && !clientId.isBlank()) {
                builder.queryParam("client_id", clientId);
            }
            response.sendRedirect(builder.encode().build().toUriString());
        };
    }

    @Bean
    public SecurityContextHolderStrategy securityContextHolderStrategy() {
        return SecurityContextHolder.getContextHolderStrategy();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(
                16,
                32,
                1,
                1 << 16,
                2
        );
    }

    @Bean
    AuthenticationManager authenticationManager(final ClientAwareDaoAuthProvider clientDaoAuthProvider,
                                                final PlatformUserDaoAuthProvider platformUserDaoAuthProvider) {
        return new ProviderManager(platformUserDaoAuthProvider, clientDaoAuthProvider);
    }
}
