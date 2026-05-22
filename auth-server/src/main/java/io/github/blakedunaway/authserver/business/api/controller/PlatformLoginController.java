package io.github.blakedunaway.authserver.business.api.controller;

import io.github.blakedunaway.authserver.business.model.DemoAccessCode;
import io.github.blakedunaway.authserver.business.model.user.PlatformRegisterDto;
import io.github.blakedunaway.authserver.business.service.DemoAccessCodeService;
import io.github.blakedunaway.authserver.business.service.TwoFactorService;
import io.github.blakedunaway.authserver.business.service.TwoFactorService.PendingVerification;
import io.github.blakedunaway.authserver.business.validation.PlatformCredentialValidator;
import io.github.blakedunaway.authserver.security.session.AuthSessionHandler;
import io.github.blakedunaway.authserver.util.AuthenticationUtility;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
@Controller
@RequestMapping("/platform")
@Slf4j
public class PlatformLoginController {

    private final AuthenticationManager authenticationManager;

    private final AuthSessionHandler authSessionHandler;

    private final DemoAccessCodeService demoAccessCodeService;

    private final TwoFactorService twoFactorService;

    private final PlatformCredentialValidator platformCredentialValidator;

    @Value("${auth-server.frontend.origin}")
    private String frontendOrigin;

    @Value("${auth-server.beta-mode:false}")
    private boolean betaMode;

    @PostMapping("/demo-access-code")
    public void loginWithDemoAccessCode(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        @RequestParam final Map<String, String> requestParams) throws IOException {
        final String code = requestParams.get("code");
        final DemoAccessCode demoAccessCode = demoAccessCodeService.findByAccessCode(code);
        if (demoAccessCode == null || demoAccessCode.isExhausted() || demoAccessCode.getUser() == null || !demoAccessCode.getUser().isDemoUser()) {
            log.warn("Demo access code login failed for code {}.", code);
            response.sendRedirect(UriComponentsBuilder.fromUriString(frontendOrigin)
                                                      .path("/demo-access")
                                                      .queryParam("error", "invalid_code")
                                                      .build()
                                                      .encode()
                                                      .toUriString());
            return;
        }

        final String email = demoAccessCode.getUser().getEmail();
        final Authentication result =
                PlatformRegisterDto.UsernamePasswordWithPlatformAuthenticationToken.authenticated(
                        email,
                        demoAccessCode.getUser().toSpring().getAuthorities()
                );

        demoAccessCodeService.save(demoAccessCode.recordUse());
        authSessionHandler.successfulAuthentication(request, response, result, buildFrontendAuthorizeUrl(requestParams));
    }

    @GetMapping("/login")
    public String login(final Model model) {
        if (betaMode) {
            return "redirect:" + buildFrontendBetaRedirectUrl();
        }

        model.addAttribute("platformRegisterDto", new PlatformRegisterDto());
        return "login";
    }

    @PostMapping("/login")
    public String login(final HttpServletRequest request,
                        final HttpServletResponse response,
                        @ModelAttribute("platformRegisterDto") final PlatformRegisterDto platformRegisterDto,
                        final BindingResult bindingResult) throws IOException, ServletException {
        if (betaMode) {
            response.sendRedirect(buildFrontendBetaRedirectUrl());
            return null;
        }

        platformCredentialValidator.validateForLogin(platformRegisterDto, bindingResult);
        if (bindingResult.hasErrors()) {
            return "login";
        }

        try {
            final Authentication result = authenticationManager.authenticate(platformRegisterDto.toAuthenticationToken());
            authSessionHandler.successfulAuthentication(request, response, result);
        } catch (final AuthenticationException ex) {
            log.warn("Platform login failed for {}.", platformRegisterDto.getEmail(), ex);
            AuthenticationUtility.rejectAuthenticationFailure(bindingResult, ex);
            return "login";
        }
        return null;
    }

    @GetMapping("/signUp")
    public String signUp(final Model model) {
        if (betaMode) {
            return "redirect:" + buildFrontendBetaRedirectUrl();
        }

        model.addAttribute("platformRegisterDto", new PlatformRegisterDto());
        return "signUp";
    }

    @PostMapping("/signUp")
    public String signUp(@ModelAttribute("platformRegisterDto") final PlatformRegisterDto platformRegisterDto,
                         final BindingResult bindingResult) {
        if (betaMode) {
            return "redirect:" + buildFrontendBetaRedirectUrl();
        }

        platformCredentialValidator.validateForSignUp(platformRegisterDto, bindingResult);
        if (bindingResult.hasErrors()) {
            log.warn("Platform sign up validation failed for {}.", platformRegisterDto.getEmail());
            return "signUp";
        }
        final PendingVerification pendingVerification = twoFactorService.sendPlatformSignUpVerification(platformRegisterDto);
        return "redirect:" + UriComponentsBuilder.fromPath("/two-factor/pending")
                                                 .queryParam("session", pendingVerification.sessionId())
                                                 .queryParam("email", pendingVerification.emailAddress())
                                                 .queryParam("flow", "platform")
                                                 .build()
                                                 .encode()
                                                 .toUriString();
    }

    private String buildFrontendBetaRedirectUrl() {
        return UriComponentsBuilder.fromUriString(frontendOrigin)
                                   .path("/app/login")
                                   .queryParam("beta", 1)
                                   .build()
                                   .encode()
                                   .toUriString();
    }

    private String buildFrontendAuthorizeUrl(final Map<String, String> requestParams) {
        final UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/oauth2/authorize");

        requestParams.forEach((key, value) -> {
            if ("code".equals(key) || StringUtils.isBlank(value)) {
                return;
            }
            builder.queryParam(key, value);
        });

        return builder.build().toUriString();
    }

}
