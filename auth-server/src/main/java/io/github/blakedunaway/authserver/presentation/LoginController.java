package io.github.blakedunaway.authserver.presentation;

import io.github.blakedunaway.authserver.business.model.RegisterDto;
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

@RequiredArgsConstructor
@Controller
public class LoginController {

    private final UserService userService;

    private final AuthenticationManager authenticationManager;

    private final RequestCache requestCache;

    private final AuthSessionHandler authSessionHandler;

    @GetMapping("/login")
    public String login(HttpServletRequest request, HttpServletResponse response, Model model) {
        RegisterDto dto = new RegisterDto();

        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null) {
            UriComponents uri = UriComponentsBuilder.fromUriString(saved.getRedirectUrl()).build();
            dto.setClientId(uri.getQueryParams().getFirst("client_id"));
        } else {
            dto.setClientId(request.getParameter("client_id"));
        }

        model.addAttribute("registerDto", dto);
        return "login";
    }

    @PostMapping("/login")
    public void login(HttpServletRequest request,
                      HttpServletResponse response,
                      @ModelAttribute("registerDto") RegisterDto registerDto) throws IOException, ServletException {

        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null && registerDto.getClientId() == null) {
            UriComponents uri = UriComponentsBuilder.fromUriString(saved.getRedirectUrl()).build();
            registerDto.setClientId(uri.getQueryParams().getFirst("client_id"));
        }

        try {
            Authentication result = authenticationManager.authenticate(registerDto.toAuthenticationToken());
            authSessionHandler.successfulAuthentication(request, response, result); // this commits
        } catch (AuthenticationException ex) {
            authSessionHandler.unsuccessfulAuthentication(request, response, ex); // this commits (usually)
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
            registerDto.setClientId(clientId);
            userService.signUp(registerDto);
            return "redirect:/login";
        } else {
            return "signUp";
        }
    }

}
