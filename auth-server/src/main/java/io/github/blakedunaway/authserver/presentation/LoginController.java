package io.github.blakedunaway.authserver.presentation;

import io.github.blakedunaway.authserver.business.model.RegisterDto;
import io.github.blakedunaway.authserver.business.service.UserService;
import io.github.blakedunaway.authserver.config.AuthSessionConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@RequiredArgsConstructor
@Controller
public class LoginController {

    private final UserService userService;

    private final AuthenticationManager authenticationManager;

    private final RequestCache requestCache;

    private final AuthSessionConfig authSessionConfig;

    @GetMapping("/login")
    public String login(final HttpServletRequest request, final Model model) {
        model.addAttribute("registerDto", new RegisterDto());
        return "login";
    }

    @PostMapping("/login")
    public void login(final HttpServletRequest request,
                      final HttpServletResponse response,
                      @ModelAttribute final RegisterDto registerDto) throws ServletException, IOException {
        final SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null) {
            final UriComponents uri = UriComponentsBuilder.fromUriString(saved.getRedirectUrl()).build();
            final String clientId = uri.getQueryParams().getFirst("client_id");
            registerDto.setRegisteredClientId(clientId);
            try {
                final Authentication result = authenticationManager.authenticate(registerDto.toAuthenticationToken());
                Assert.notNull(result, "UserDetailsAuthenticationProvider.badCredentials");
                authSessionConfig.successfulAuthentication(request, response, result);
            } catch (Exception failed) {
                authSessionConfig.unsuccessfulAuthentication(request, response, (AuthenticationException) failed);
            }

        }
    }

    @GetMapping("/signUp")
    public String signUp(final Model model) {
        model.addAttribute("registerDto", new RegisterDto());
        return "signUp";
    }

    @PostMapping("/signUp")
    public String signUp(@ModelAttribute("registerDto") @Valid final RegisterDto registerDto,
                         BindingResult bindingResult,
                         HttpServletRequest request, HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return "signUp"; // Return the form with errors displayed
        }
        final SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null) {
            final UriComponents uri = UriComponentsBuilder.fromUriString(saved.getRedirectUrl()).build();
            final String clientId = uri.getQueryParams().getFirst("client_id");
            registerDto.setRegisteredClientId(clientId);
            userService.signUp(registerDto);
            return "redirect:/login";
        } else {
            return "signUp";
        }
    }

}
