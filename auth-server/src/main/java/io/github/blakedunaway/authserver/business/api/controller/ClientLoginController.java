package io.github.blakedunaway.authserver.business.api.controller;

import io.github.blakedunaway.authserver.business.api.dto.ClientUserActivity;
import io.github.blakedunaway.authserver.business.model.user.ClientRegisterDto;
import io.github.blakedunaway.authserver.business.service.UserService;
import io.github.blakedunaway.authserver.config.redis.RedisStore;
import io.github.blakedunaway.authserver.security.session.AuthSessionHandler;
import io.github.blakedunaway.authserver.util.RedisUtility;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Controller
public class ClientLoginController {

    private final UserService userService;

    private final AuthenticationManager authenticationManager;

    private final RequestCache requestCache;

    private final AuthSessionHandler authSessionHandler;

    private final RedisStore redisStore;

    @Value("${auth-server.frontend.client-id}")
    private String platformClientId;

    @GetMapping("/login")
    public String login(final HttpServletRequest request, final HttpServletResponse response, final Model model) {
        final ClientRegisterDto dto = new ClientRegisterDto();

        final SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null) {
            final UriComponents uri = UriComponentsBuilder.fromUriString(saved.getRedirectUrl()).build();
            dto.setClientId(uri.getQueryParams().getFirst("client_id"));
        } else {
            dto.setClientId(request.getParameter("client_id"));
        }

        if (dto.getClientId() == null || dto.getClientId().isBlank()) {
            return "redirect:/oauth-error?error=invalid_request&error_description=Missing%20client_id";
        }

        if (platformClientId.equals(dto.getClientId())) {
            return "redirect:/platform/login";
        }

        model.addAttribute("registerDto", dto);
        return "login";
    }

    @PostMapping("/login")
    public void login(final HttpServletRequest request,
                      final HttpServletResponse response,
                      @ModelAttribute("registerDto") final ClientRegisterDto clientRegisterDto)
            throws IOException, ServletException {

        final SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null && clientRegisterDto.getClientId() == null) {
            final UriComponents uri = UriComponentsBuilder.fromUriString(saved.getRedirectUrl()).build();
            clientRegisterDto.setClientId(uri.getQueryParams().getFirst("client_id"));
        }

        if (StringUtils.isEmpty(clientRegisterDto.getClientId())) {
            response.sendRedirect("/oauth-error?error=invalid_request&error_description=Missing%20client_id");
            return;
        }
        redisStore.pushToList(RedisUtility.CLIENT_LOGIN_ATTRIBUTE + clientRegisterDto.getClientId(),
                              ClientUserActivity.builder()
                                                .email(clientRegisterDto.getEmail())
                                                .activityTs(LocalDateTime.now())
                                                .build(),
                              Duration.ofMinutes(15));
        try {
            final Authentication result = authenticationManager.authenticate(clientRegisterDto.toAuthenticationToken());
            authSessionHandler.successfulAuthentication(request, response, result);
        } catch (final AuthenticationException ex) {
            authSessionHandler.unsuccessfulAuthentication(request, response, ex);
        }
    }

    @GetMapping("/signUp")
    public String signUp(final Model model) {
        model.addAttribute("registerDto", new ClientRegisterDto());
        return "signUp";
    }

    @PostMapping("/signUp")
    public String signUp(@ModelAttribute("registerDto") @Valid final ClientRegisterDto clientRegisterDto,
                         final BindingResult bindingResult,
                         final HttpServletRequest request,
                         final HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return "signUp"; // Return the form with errors displayed
        }
        final SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null) {
            final UriComponents uri = UriComponentsBuilder.fromUriString(saved.getRedirectUrl()).build();
            final String clientId = uri.getQueryParams().getFirst("client_id");
            clientRegisterDto.setClientId(clientId);
            if (clientId == null || clientId.isBlank()) {
                return "redirect:/oauth-error?error=invalid_request&error_description=Missing%20client_id";
            }
            userService.signUpClientUser(clientRegisterDto);
            redisStore.pushToList(RedisUtility.CLIENT_SIGNUP_ATTRIBUTE + clientRegisterDto.getClientId(),
                                  ClientUserActivity.builder()
                                                    .email(clientRegisterDto.getEmail())
                                                    .activityTs(LocalDateTime.now())
                                                    .build(),
                                  Duration.ofMinutes(15));
            return "redirect:/login";
        } else {
            return "redirect:/oauth-error?error=invalid_request&error_description=Missing%20client_id";
        }
    }

}
