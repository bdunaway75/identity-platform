package io.github.blakedunaway.authserver.business.api.controller;

import io.github.blakedunaway.authserver.business.model.DemoAccessCode;
import io.github.blakedunaway.authserver.business.model.user.PlatformRegisterDto;
import io.github.blakedunaway.authserver.business.service.DemoAccessCodeService;
import io.github.blakedunaway.authserver.business.service.UserService;
import io.github.blakedunaway.authserver.security.session.AuthSessionHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final String DEMO_ACCESS_CODE_ERROR_QUERY = "error=true";

    private final UserService userService;

    private final AuthenticationManager authenticationManager;

    private final AuthSessionHandler authSessionHandler;

    private final DemoAccessCodeService demoAccessCodeService;

    @Value("${auth-server.frontend.origin}")
    private String frontendOrigin;

    @PostMapping("/demo-access-code")
    public void loginWithDemoAccessCode(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        @RequestParam final Map<String, String> requestParams) throws IOException, ServletException {
        final String code = requestParams.get("code");
        final DemoAccessCode demoAccessCode = demoAccessCodeService.findByAccessCode(code);
        if (demoAccessCode == null || demoAccessCode.isDispensed() || demoAccessCode.getUser() == null) {
            log.warn("Demo access code login failed for code {}.", code);
            response.sendRedirect(frontendOrigin + "/demo-access?error=invalid_code");
            return;
        }

        final String email = demoAccessCode.getUser().getEmail();
        final Authentication result =
                PlatformRegisterDto.UsernamePasswordWithPlatformAuthenticationToken.authenticated(
                        email,
                        demoAccessCode.getUser().toSpring().getAuthorities()
                );

        demoAccessCodeService.save(demoAccessCode.toBuilder().dispensed(true).build());
        authSessionHandler.successfulAuthentication(request, response, result, buildFrontendAuthorizeUrl(requestParams));
    }

    @GetMapping("/login")
    public String login(final Model model) {
        model.addAttribute("platformRegisterDto", new PlatformRegisterDto());
        return "login";
    }

    @PostMapping("/login")
    public void login(final HttpServletRequest request,
                      final HttpServletResponse response,
                      @ModelAttribute("platformRegisterDto") final PlatformRegisterDto platformRegisterDto) throws IOException, ServletException {
        try {
            final Authentication result = authenticationManager.authenticate(platformRegisterDto.toAuthenticationToken());
            authSessionHandler.successfulAuthentication(request, response, result);
        } catch (final AuthenticationException ex) {
            log.warn("Platform login failed for {}.", platformRegisterDto.getEmail(), ex);
            authSessionHandler.unsuccessfulAuthentication(request, response, ex);
        }
    }

    @GetMapping("/signUp")
    public String signUp(final Model model) {
        model.addAttribute("platformRegisterDto", new PlatformRegisterDto());
        return "signUp";
    }

    @PostMapping("/signUp")
    public String signUp(@ModelAttribute("platformRegisterDto") @Valid final PlatformRegisterDto platformRegisterDto,
                         final BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            log.warn("Platform sign up validation failed for {}.", platformRegisterDto.getEmail());
            return "signUp";
        }
        userService.signUpPlatformUser(platformRegisterDto);
        return "redirect:/platform/login";
    }

    private String buildFrontendAuthorizeUrl(final Map<String, String> requestParams) {
        final UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/oauth2/authorize");

        requestParams.forEach((key, value) -> {
            if ("code".equals(key) || value == null || value.isBlank()) {
                return;
            }
            builder.queryParam(key, value);
        });

        return builder.build().toUriString();
    }

}
