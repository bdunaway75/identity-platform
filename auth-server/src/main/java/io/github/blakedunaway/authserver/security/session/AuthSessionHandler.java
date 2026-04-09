package io.github.blakedunaway.authserver.security.session;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class AuthSessionHandler {

    private final SecurityContextHolderStrategy securityContextHolderStrategy;

    private final SecurityContextRepository securityContextRepository;

    private final AuthenticationSuccessHandler authenticationSuccessHandler;

    private final AuthenticationFailureHandler authenticationFailureHandler;

    public void successfulAuthentication(final HttpServletRequest request,
                                         final HttpServletResponse response,
                                         final Authentication authResult) throws ServletException, IOException {
        storeSecurityContext(request, response, authResult);
        authenticationSuccessHandler.onAuthenticationSuccess(request, response, authResult);

    }

    public void successfulAuthentication(final HttpServletRequest request,
                                         final HttpServletResponse response,
                                         final Authentication authResult,
                                         final String redirectUrl) throws IOException {
        storeSecurityContext(request, response, authResult);
        response.sendRedirect(redirectUrl);
    }

    public void unsuccessfulAuthentication(final HttpServletRequest request,
                                           final HttpServletResponse response,
                                           final AuthenticationException failed) throws ServletException, IOException {
        securityContextHolderStrategy.clearContext();
        authenticationFailureHandler.onAuthenticationFailure(request, response, failed);
    }

    private void storeSecurityContext(final HttpServletRequest request,
                                      final HttpServletResponse response,
                                      final Authentication authResult) {
        final SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

}
