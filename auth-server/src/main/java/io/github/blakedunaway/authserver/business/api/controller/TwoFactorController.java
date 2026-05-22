package io.github.blakedunaway.authserver.business.api.controller;

import io.github.blakedunaway.authserver.business.service.EventStreamService;
import io.github.blakedunaway.authserver.business.service.EventStreamService.StreamEvent;
import io.github.blakedunaway.authserver.business.service.TwoFactorService;
import io.github.blakedunaway.authserver.business.service.TwoFactorService.TwoFactorVerificationResult;
import io.github.blakedunaway.authserver.util.EventStreamUtility;
import io.github.blakedunaway.authserver.util.RedisUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class TwoFactorController {

    private final TwoFactorService twoFactorService;

    private final EventStreamService eventStreamService;

    @GetMapping("/two-factor/pending")
    public String pending(@RequestParam("session") final String session,
                          @RequestParam("email") final String email,
                          @RequestParam("flow") final String flow,
                          @RequestParam(value = "clientId", required = false) final String clientId,
                          final Model model) {
        final boolean platformFlow = "platform".equalsIgnoreCase(flow);
        final String fallbackHref = platformFlow
                                    ? "/platform/login"
                                    : UriComponentsBuilder.fromPath("/login")
                                                          .queryParamIfPresent("client_id", Optional.ofNullable(clientId))
                                                          .build()
                                                          .encode()
                                                          .toUriString();

        model.addAttribute("sessionId", session);
        model.addAttribute("emailAddress", email);
        model.addAttribute("fallbackHref", fallbackHref);
        return "two-factor-pending";
    }

    @GetMapping(path = "/two-factor/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@RequestParam("session") final String session) {
        return eventStreamService.subscribe(RedisUtility.TWO_FACTOR_VERIFICATION_STATUS + session,
                                            EventStreamUtility.VERIFIED_EVENT,
                                            new StreamEvent(EventStreamUtility.WAITING_EVENT,
                                                            EventStreamUtility.CONNECTED_MESSAGE,
                                                            false));
    }

    @GetMapping("/two-factor/verify")
    public String verify(@RequestParam("code") final String code,
                         @RequestParam(value = "flow", required = false) final String flow,
                         @RequestParam(value = "clientId", required = false) final String clientId) {
        final TwoFactorVerificationResult result = twoFactorService.verifySignUpCode(code, "platform".equalsIgnoreCase(flow), clientId);
        return "redirect:" + result.redirectUrl();
    }
}
