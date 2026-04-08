package io.github.blakedunaway.authserver.business.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OAuthErrorController {

    @GetMapping("/oauth-error")
    public String oauthError(
            @RequestParam String error,
            @RequestParam(required = false) String error_description,
            Model model
    ) {
        model.addAttribute("error", error);
        model.addAttribute("description", error_description);
        return "oauth-error";
    }
}
