package io.github.blakedunaway.authserver.business.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Slf4j
public class OAuthErrorController {

    @GetMapping("/oauth-error")
    public String oauthError(
            @RequestParam final String error,
            @RequestParam(required = false) final String error_description,
            Model model
    ) {
        model.addAttribute("error", error);
        model.addAttribute("description", error_description);
        log.warn("OAuth error page rendered with error {} and description {}", error, error_description);
        return "oauth-error";
    }
}
