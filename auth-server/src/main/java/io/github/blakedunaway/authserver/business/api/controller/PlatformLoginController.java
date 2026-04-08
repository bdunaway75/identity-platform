package io.github.blakedunaway.authserver.business.api.controller;

import io.github.blakedunaway.authserver.business.model.user.PlatformRegisterDto;
import io.github.blakedunaway.authserver.business.service.UserService;
import io.github.blakedunaway.authserver.security.session.AuthSessionHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

@RequiredArgsConstructor
@Controller
@RequestMapping("/platform")
public class PlatformLoginController {

    private final UserService userService;

    private final AuthenticationManager authenticationManager;

    private final AuthSessionHandler authSessionHandler;

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
            return "signUp";
        }
        userService.signUpPlatformUser(platformRegisterDto);
        return "redirect:/platform/login";
    }

}
