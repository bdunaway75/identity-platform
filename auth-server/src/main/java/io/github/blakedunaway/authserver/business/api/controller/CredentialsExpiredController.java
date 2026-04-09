package io.github.blakedunaway.authserver.business.api.controller;

import io.github.blakedunaway.authserver.business.api.dto.CredentialsExpiredPasswordChangeRequest;
import io.github.blakedunaway.authserver.business.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequiredArgsConstructor
public class CredentialsExpiredController {

    private final UserService userService;

    @GetMapping("/credentials-expired")
    public String clientCredentialsExpired(@RequestParam(required = false) final String email,
                                           @RequestParam(required = false) final String clientId,
                                           @RequestParam(required = false) final String error,
                                           final Model model) {
        if (email == null || email.isBlank() || clientId == null || clientId.isBlank()) {
            return "redirect:" + UriComponentsBuilder.fromPath("/login")
                                                     .queryParam("message", "Unable to start the password update flow. Please sign in again.")
                                                     .encode()
                                                     .build()
                                                     .toUriString();
        }

        final CredentialsExpiredPasswordChangeRequest request = new CredentialsExpiredPasswordChangeRequest();
        request.setEmail(email);
        request.setClientId(clientId);
        model.addAttribute("passwordChangeRequest", request);
        model.addAttribute("isPlatformFlow", false);
        model.addAttribute("errorMessage", error);
        return "credentials-expired";
    }

    @PostMapping("/credentials-expired")
    public String clientCredentialsExpired(@ModelAttribute("passwordChangeRequest")
                                           @Valid final CredentialsExpiredPasswordChangeRequest request,
                                           final BindingResult bindingResult,
                                           final Model model) {
        if (request.getClientId() == null || request.getClientId().isBlank()) {
            return "redirect:" + UriComponentsBuilder.fromPath("/login")
                                                     .queryParam("message", "Unable to determine which client user is changing their password.")
                                                     .encode()
                                                     .build()
                                                     .toUriString();
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("passwordChangeRequest", request);
            model.addAttribute("isPlatformFlow", false);
            model.addAttribute("errorMessage", bindingResult.getAllErrors().getFirst().getDefaultMessage());
            return "credentials-expired";
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            model.addAttribute("passwordChangeRequest", request);
            model.addAttribute("isPlatformFlow", false);
            model.addAttribute("errorMessage", "New password and confirmation must match.");
            return "credentials-expired";
        }

        final boolean updated = userService.updateExpiredClientUserPassword(
                request.getClientId(),
                request.getEmail(),
                request.getCurrentPassword(),
                request.getNewPassword()
        );
        if (!updated) {
            model.addAttribute("passwordChangeRequest", request);
            model.addAttribute("isPlatformFlow", false);
            model.addAttribute("errorMessage", "We could not verify your current password. Please try again.");
            return "credentials-expired";
        }

        return "redirect:" + UriComponentsBuilder.fromPath("/login")
                                                 .queryParam("client_id", request.getClientId())
                                                 .queryParam("message", "Password updated successfully. Please sign in with your new password.")
                                                 .encode()
                                                 .build()
                                                 .toUriString();
    }

    @GetMapping("/platform/credentials-expired")
    public String platformCredentialsExpired(@RequestParam(required = false) final String email,
                                             @RequestParam(required = false) final String error,
                                             final Model model) {
        if (email == null || email.isBlank()) {
            return "redirect:" + UriComponentsBuilder.fromPath("/platform/login")
                                                     .queryParam("message", "Unable to start the password update flow. Please sign in again.")
                                                     .encode()
                                                     .build()
                                                     .toUriString();
        }

        final CredentialsExpiredPasswordChangeRequest request = new CredentialsExpiredPasswordChangeRequest();
        request.setEmail(email);
        model.addAttribute("passwordChangeRequest", request);
        model.addAttribute("isPlatformFlow", true);
        model.addAttribute("errorMessage", error);
        return "credentials-expired";
    }

    @PostMapping("/platform/credentials-expired")
    public String platformCredentialsExpired(@ModelAttribute("passwordChangeRequest")
                                             @Valid final CredentialsExpiredPasswordChangeRequest request,
                                             final BindingResult bindingResult,
                                             final Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("passwordChangeRequest", request);
            model.addAttribute("isPlatformFlow", true);
            model.addAttribute("errorMessage", bindingResult.getAllErrors().getFirst().getDefaultMessage());
            return "credentials-expired";
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            model.addAttribute("passwordChangeRequest", request);
            model.addAttribute("isPlatformFlow", true);
            model.addAttribute("errorMessage", "New password and confirmation must match.");
            return "credentials-expired";
        }

        final boolean updated = userService.updateExpiredPlatformUserPassword(
                request.getEmail(),
                request.getCurrentPassword(),
                request.getNewPassword()
        );
        if (!updated) {
            model.addAttribute("passwordChangeRequest", request);
            model.addAttribute("isPlatformFlow", true);
            model.addAttribute("errorMessage", "We could not verify your current password. Please try again.");
            return "credentials-expired";
        }

        return "redirect:" + UriComponentsBuilder.fromPath("/platform/login")
                                                 .queryParam("message", "Password updated successfully. Please sign in with your new password.")
                                                 .encode()
                                                 .build()
                                                 .toUriString();
    }

}
