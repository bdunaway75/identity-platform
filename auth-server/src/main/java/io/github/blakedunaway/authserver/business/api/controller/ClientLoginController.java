package io.github.blakedunaway.authserver.business.api.controller;

import io.github.blakedunaway.authserver.business.api.dto.response.ClientUserActivity;
import io.github.blakedunaway.authserver.business.model.user.ClientRegisterDto;
import io.github.blakedunaway.authserver.business.service.TwoFactorService;
import io.github.blakedunaway.authserver.business.service.TwoFactorService.PendingVerification;
import io.github.blakedunaway.authserver.business.validation.ClientCredentialValidator;
import io.github.blakedunaway.authserver.config.redis.RedisStore;
import io.github.blakedunaway.authserver.security.session.AuthSessionHandler;
import io.github.blakedunaway.authserver.util.AuthenticationUtility;
import io.github.blakedunaway.authserver.util.RedisUtility;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
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
@Slf4j
public class ClientLoginController {

    private final AuthenticationManager authenticationManager;

    private final RequestCache requestCache;

    private final AuthSessionHandler authSessionHandler;

    private final RedisStore redisStore;

    private final TwoFactorService twoFactorService;

    private final ClientCredentialValidator clientCredentialValidator;

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

        final BindingResult bindingResult = new BeanPropertyBindingResult(dto, "registerDto");
        clientCredentialValidator.validateClientId(dto, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("registerDto", dto);
            model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "registerDto", bindingResult);
            return "login";
        }

        if (platformClientId.equals(dto.getClientId())) {
            return "redirect:/platform/login";
        }

        model.addAttribute("registerDto", dto);
        return "login";
    }

    @PostMapping("/login")
    public String login(final HttpServletRequest request,
                        final HttpServletResponse response,
                        @ModelAttribute("registerDto") final ClientRegisterDto clientRegisterDto,
                        final BindingResult bindingResult)
            throws IOException, ServletException {

        final SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null && clientRegisterDto.getClientId() == null) {
            final UriComponents uri = UriComponentsBuilder.fromUriString(saved.getRedirectUrl()).build();
            clientRegisterDto.setClientId(uri.getQueryParams().getFirst("client_id"));
        }

        clientCredentialValidator.validateForLogin(clientRegisterDto, bindingResult);
        if (bindingResult.hasErrors()) {
            return "login";
        }
        redisStore.pushToList(RedisUtility.CLIENT_LOGIN_ATTRIBUTE + clientRegisterDto.getClientId(),
                              ClientUserActivity.recordActivity(clientRegisterDto.getEmail()),
                              Duration.ofMinutes(15));
        try {
            final Authentication result = authenticationManager.authenticate(clientRegisterDto.toAuthenticationToken());
            authSessionHandler.successfulAuthentication(request, response, result);
        } catch (final AuthenticationException ex) {
            log.warn("Client login failed for email {} and client {}", clientRegisterDto.getEmail(), clientRegisterDto.getClientId(), ex);
            AuthenticationUtility.rejectAuthenticationFailure(bindingResult, ex);
            return "login";
        }
        return null;
    }

    @GetMapping("/signUp")
    public String signUp(final HttpServletRequest request, final HttpServletResponse response, final Model model) {
        final ClientRegisterDto dto = new ClientRegisterDto();
        final SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null) {
            final UriComponents uri = UriComponentsBuilder.fromUriString(saved.getRedirectUrl()).build();
            dto.setClientId(uri.getQueryParams().getFirst("client_id"));
        } else {
            dto.setClientId(request.getParameter("client_id"));
        }

        model.addAttribute("registerDto", dto);
        return "signUp";
    }

    @PostMapping("/signUp")
    public String signUp(@ModelAttribute("registerDto") final ClientRegisterDto clientRegisterDto,
                         final BindingResult bindingResult,
                         final HttpServletRequest request,
                         final HttpServletResponse response) {
        final SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null) {
            final UriComponents uri = UriComponentsBuilder.fromUriString(saved.getRedirectUrl()).build();
            final String clientId = uri.getQueryParams().getFirst("client_id");
            if (clientRegisterDto.getClientId() == null || clientRegisterDto.getClientId().isBlank()) {
                clientRegisterDto.setClientId(clientId);
            }
        }

        clientCredentialValidator.validateForSignUp(clientRegisterDto, bindingResult);
        if (bindingResult.hasErrors()) {
            return "signUp";
        }

        final PendingVerification pendingVerification = twoFactorService.sendClientSignUpVerification(clientRegisterDto);
        redisStore.pushToList(RedisUtility.CLIENT_SIGNUP_ATTRIBUTE + clientRegisterDto.getClientId(),
                              ClientUserActivity.recordActivity(clientRegisterDto.getEmail()),
                              Duration.ofMinutes(15));
        return "redirect:" + UriComponentsBuilder.fromPath("/two-factor/pending")
                                                 .queryParam("session", pendingVerification.sessionId())
                                                 .queryParam("email", pendingVerification.emailAddress())
                                                 .queryParam("clientId", pendingVerification.clientId())
                                                 .queryParam("flow", "client")
                                                 .build()
                                                 .encode()
                                                 .toUriString();
    }

}
