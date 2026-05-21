package io.github.blakedunaway.authserver.business.service;

import io.github.blakedunaway.authserver.business.model.user.ClientRegisterDto;
import io.github.blakedunaway.authserver.business.model.user.PendingTwoFactorSignUp;
import io.github.blakedunaway.authserver.business.model.user.PlatformRegisterDto;
import io.github.blakedunaway.authserver.config.redis.RedisStore;
import io.github.blakedunaway.authserver.util.AuthenticationUtility;
import io.github.blakedunaway.authserver.util.EventStreamUtility;
import io.github.blakedunaway.authserver.util.RedisUtility;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private static final Duration SIGN_UP_CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFICATION_STATUS_TTL = Duration.ofMinutes(5);
    private static final String PLATFORM_LOGO_CONTENT_ID = "platformLogo";
    private static final String PLATFORM_LOGO_RESOURCE_PATH = "email/ip-logo.png";
    private static final String VERIFIED_MESSAGE = "Email verified. You can sign in now.";
    private static final String INVALID_LINK_MESSAGE = "This verification link is no longer valid. Start sign up again for a new email.";
    private static final String MISSING_CLIENT_ID_MESSAGE = "This client verification link is missing the required client id. Start sign up again for a new email.";

    private final RedisStore redisStore;

    private final EmailService emailService;

    private final EventStreamService eventStreamService;

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    @Value("${auth-server.issuer}")
    private String issuer;

    public PendingVerification sendClientSignUpVerification(final ClientRegisterDto clientRegisterDto) {
        Assert.notNull(clientRegisterDto, "Client registration is required");
        final PendingTwoFactorSignUp pendingTwoFactorSignUp = PendingTwoFactorSignUp.builder()
                                                                                    .email(clientRegisterDto.getEmail())
                                                                                    .passwordHash(passwordEncoder.encode(clientRegisterDto.getPassword()))
                                                                                    .clientId(clientRegisterDto.getClientId())
                                                                                    .platformFlow(false)
                                                                                    .build();
        final String code = UUID.randomUUID().toString();
        final String verificationSessionId = createVerificationSessionId(pendingTwoFactorSignUp, code);
        final String verificationUrl = UriComponentsBuilder.fromUriString(issuer)
                                                           .path("/two-factor/verify")
                                                           .queryParam("code", code)
                                                           .queryParam("flow", "client")
                                                           .queryParam("clientId", pendingTwoFactorSignUp.getClientId())
                                                           .build()
                                                           .encode()
                                                           .toUriString();
        sendVerificationEmail(pendingTwoFactorSignUp, verificationUrl);
        return new PendingVerification(verificationSessionId,
                                       pendingTwoFactorSignUp.getEmail(),
                                       pendingTwoFactorSignUp.getClientId(),
                                       false);
    }

    public PendingVerification sendPlatformSignUpVerification(final PlatformRegisterDto platformRegisterDto) {
        Assert.notNull(platformRegisterDto, "Platform registration is required");
        final PendingTwoFactorSignUp pendingTwoFactorSignUp = PendingTwoFactorSignUp.builder()
                                                                                    .email(platformRegisterDto.getEmail())
                                                                                    .passwordHash(passwordEncoder.encode(platformRegisterDto.getPassword()))
                                                                                    .platformFlow(true)
                                                                                    .build();
        final String code = UUID.randomUUID().toString();
        final String verificationSessionId = createVerificationSessionId(pendingTwoFactorSignUp, code);
        final String verificationUrl = UriComponentsBuilder.fromUriString(issuer)
                                                           .path("/two-factor/verify")
                                                           .queryParam("code", code)
                                                           .queryParam("flow", "platform")
                                                           .build()
                                                           .encode()
                                                           .toUriString();
        sendVerificationEmail(pendingTwoFactorSignUp, verificationUrl);
        return new PendingVerification(verificationSessionId,
                                       pendingTwoFactorSignUp.getEmail(),
                                       null,
                                       true);
    }

    public TwoFactorVerificationResult verifySignUpCode(final String code,
                                                        final boolean platformFlow,
                                                        final String clientId) {
        if (!platformFlow && StringUtils.isBlank(clientId)) {
            return TwoFactorVerificationResult.failure(AuthenticationUtility.buildClientLoginRedirect(null, MISSING_CLIENT_ID_MESSAGE));
        }

        if (StringUtils.isBlank(code)) {
            return failureRedirect(platformFlow, clientId);
        }

        final PendingTwoFactorSignUp pending = redisStore.consume(RedisUtility.TWO_FACTOR_SIGN_UP + code);
        if (pending == null) {
            return failureRedirect(platformFlow, clientId);
        }

        if (pending.isPlatformFlow()) {
            userService.saveVerifiedPlatformUser(pending.getEmail(), pending.getPasswordHash());
            final String redirectUrl = AuthenticationUtility.buildPlatformLoginRedirect(VERIFIED_MESSAGE);
            eventStreamService.storeAndSend(RedisUtility.TWO_FACTOR_VERIFICATION_STATUS + pending.getVerificationSessionId(),
                                            redirectUrl,
                                            VERIFICATION_STATUS_TTL,
                                            EventStreamUtility.VERIFIED_EVENT);
            return new TwoFactorVerificationResult(true, redirectUrl);
        }

        userService.saveVerifiedClientUser(pending.getEmail(), pending.getPasswordHash(), pending.getClientId());
        final String redirectUrl = AuthenticationUtility.buildClientLoginRedirect(pending.getClientId(), VERIFIED_MESSAGE);
        eventStreamService.storeAndSend(RedisUtility.TWO_FACTOR_VERIFICATION_STATUS + pending.getVerificationSessionId(),
                                        redirectUrl,
                                        VERIFICATION_STATUS_TTL,
                                        EventStreamUtility.VERIFIED_EVENT);
        return new TwoFactorVerificationResult(true, redirectUrl);
    }

    private String createVerificationSessionId(final PendingTwoFactorSignUp pendingTwoFactorSignUp, final String code) {
        final String verificationSessionId = eventStreamService.createSessionId();
        pendingTwoFactorSignUp.setVerificationSessionId(verificationSessionId);
        redisStore.put(RedisUtility.TWO_FACTOR_SIGN_UP + code, pendingTwoFactorSignUp, SIGN_UP_CODE_TTL);
        return verificationSessionId;
    }

    private void sendVerificationEmail(final PendingTwoFactorSignUp pendingTwoFactorSignUp,
                                       final String verificationUrl) {
        emailService.sendTemplateEmail(pendingTwoFactorSignUp.getEmail(),
                                       "Verify your email",
                                       "verify-sign-up.ftlh",
                                       Map.of("emailAddress", pendingTwoFactorSignUp.getEmail(),
                                              "verificationUrl", verificationUrl,
                                              "logoContentId", PLATFORM_LOGO_CONTENT_ID,
                                              "platformFlow", pendingTwoFactorSignUp.isPlatformFlow()),
                                       Map.of(PLATFORM_LOGO_CONTENT_ID, new ClassPathResource(PLATFORM_LOGO_RESOURCE_PATH))
        );
    }

    public record PendingVerification(String sessionId, String emailAddress, String clientId, boolean platformFlow) {
    }

    public record TwoFactorVerificationResult(boolean success, String redirectUrl) {

        public static TwoFactorVerificationResult failure(final String redirectUrl) {
            return new TwoFactorVerificationResult(false, redirectUrl);
        }

    }

    private TwoFactorVerificationResult failureRedirect(final boolean platformFlow, final String clientId) {
        if (platformFlow) {
            return TwoFactorVerificationResult.failure(AuthenticationUtility.buildPlatformLoginRedirect(INVALID_LINK_MESSAGE));
        }
        return TwoFactorVerificationResult.failure(AuthenticationUtility.buildClientLoginRedirect(clientId, INVALID_LINK_MESSAGE));
    }

}
