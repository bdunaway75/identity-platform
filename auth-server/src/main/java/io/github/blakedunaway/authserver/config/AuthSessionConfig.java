package io.github.blakedunaway.authserver.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class AuthSessionConfig {

    private final SecurityContextHolderStrategy securityContextHolderStrategy;

    private final SecurityContextRepository securityContextRepository;

    private final AuthenticationSuccessHandler authenticationSuccessHandler;

    private final AuthenticationFailureHandler authenticationFailureHandler;

    public void successfulAuthentication(final HttpServletRequest request,
                                         final HttpServletResponse response,
                                         final Authentication authResult) throws ServletException, IOException {
        final SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authResult);
        securityContextRepository.saveContext(context, request, response);
        authenticationSuccessHandler.onAuthenticationSuccess(request, response, authResult);

    }

    public void unsuccessfulAuthentication(final HttpServletRequest request,
                                           final HttpServletResponse response,
                                           final AuthenticationException failed) throws ServletException, IOException {
        securityContextHolderStrategy.clearContext();
        authenticationFailureHandler.onAuthenticationFailure(request, response, failed);
    }

}
