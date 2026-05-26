package io.github.blakedunaway.authserver.business.api.controller;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import io.github.blakedunaway.authserver.business.model.user.PlatformUser;
import io.github.blakedunaway.authserver.business.model.user.PlatformUserTier;
import io.github.blakedunaway.authserver.business.service.EventStreamService;
import io.github.blakedunaway.authserver.business.service.EventStreamService.StreamEvent;
import io.github.blakedunaway.authserver.business.service.PlatformUserTierService;
import io.github.blakedunaway.authserver.business.service.StripeBillingService;
import io.github.blakedunaway.authserver.business.service.StripeBillingService.PortalSession;
import io.github.blakedunaway.authserver.config.redis.RedisStore;
import io.github.blakedunaway.authserver.util.EventStreamUtility;
import io.github.blakedunaway.authserver.util.RedisUtility;
import io.github.blakedunaway.authserver.util.StripeUtility;
import io.github.blakedunaway.authserver.util.StripeUtility.SubscriptionChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/platform")
@RequiredArgsConstructor
@Slf4j
public class PlatformUserSubscriptionController {

    private static final Duration CHECKOUT_REDIRECT_TTL = Duration.ofMinutes(30);

    private static final String SUBSCRIPTIONS_REDIRECT_PATH = "/subscriptions/success";

    private static final String SUBSCRIPTIONS_CANCEL_REDIRECT_PATH = "/subscriptions/cancel";

    private final PlatformUserTierService platformUserTierService;

    private final StripeClient stripeClient;

    private final EventStreamService eventStreamService;

    private final RedisStore redisStore;

    private final StripeBillingService stripeBillingService;

    @Value("${auth-server.frontend.origin}")
    private String frontendOrigin;

    @Value("${stripe.webhook-secret}")
    private String stripeWebhookSecret;

    @PreAuthorize("hasRole('PLATFORM_USER')")
    @PostMapping("/subscription")
    public ResponseEntity<String> createCheckoutSession(@AuthenticationPrincipal final PlatformUser platformUser,
                                                        @RequestBody final String stripePriceId) throws StripeException {
        if (platformUser.isDemoUser()) {
            log.warn("Checkout session creation rejected because the authenticated platform user {} is a demo user.", platformUser.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (stripePriceId.isBlank()) {
            log.warn("Checkout session creation rejected for platform user {} because the requested tier was blank.", platformUser.getEmail());
            return ResponseEntity.badRequest().body("A valid subscription tier is required.");
        }
        final PlatformUserTier platformUserTier = platformUserTierService.findTierByStripePriceId(stripePriceId);
        if (platformUserTier == null || platformUserTier.getId() == null) {
            log.warn("Checkout session creation rejected for platform user {} because stripe price id {} did not resolve to a tier.",
                     platformUser.getEmail(),
                     stripePriceId);
            return ResponseEntity.badRequest().body("A valid subscription tier is required.");
        }
        if (StripeUtility.findActiveSubscriptionByPlatformUserId(platformUser.getId().toString(), stripeClient) != null) {
            final SubscriptionChange request = StripeUtility.resolveSubscriptionChange(platformUser,
                                                                                       stripePriceId,
                                                                                       platformUserTierService,
                                                                                       stripeClient);
            if (request.failed()) {
                return ResponseEntity.status(request.getResponse().getStatusCode())
                                     .body(request.getResponse().getBody() == null
                                           ? "Unable to open subscription management."
                                           : request.getResponse().getBody().getOrDefault("message",
                                                                                          "Unable to open subscription management."));
            }

            final PortalSession portalSession = stripeBillingService.createSubscriptionPortalSession(request);
            if (portalSession == null) {
                log.warn("Subscription portal rejected for platform user {} because the active Stripe subscription did not include a customer.",
                         platformUser.getEmail());
                return ResponseEntity.badRequest().body("No active Stripe customer was found for this subscription.");
            }
            storePortalTrackingId(portalSession);
            return ResponseEntity.ok(portalSession.session().getUrl());
        }
        return ResponseEntity.ok(stripeBillingService.createCheckoutSession(platformUser, platformUserTier));
    }

    @GetMapping(value = "/subscription-events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToCheckoutStatus(@RequestParam("session_id") final String sessionId) {
        if (sessionId.isBlank()) {
            throw new IllegalArgumentException("session_id is required");
        }

        return eventStreamService.subscribe(RedisUtility.SUBSCRIPTION_CHECKOUT_STATUS + sessionId,
                                            EventStreamUtility.REDIRECT_EVENT,
                                            new StreamEvent(EventStreamUtility.WAITING_EVENT,
                                                            EventStreamUtility.CONNECTED_MESSAGE,
                                                            false));
    }

    @PreAuthorize("hasRole('PLATFORM_USER')")
    @PostMapping("/subscription/portal")
    public ResponseEntity<String> createSubscriptionPortalSession(@AuthenticationPrincipal final PlatformUser platformUser,
                                                                  @RequestBody final String stripePriceId) throws StripeException {
        final SubscriptionChange request = StripeUtility.resolveSubscriptionChange(platformUser,
                                                                                   stripePriceId,
                                                                                   platformUserTierService,
                                                                                   stripeClient);
        if (request.failed()) {
            return ResponseEntity.status(request.getResponse().getStatusCode())
                                 .body(request.getResponse().getBody() == null
                                       ? "Unable to open subscription management."
                                       : request.getResponse().getBody().getOrDefault("message",
                                                                                      "Unable to open subscription management."));
        }

        final PortalSession portalSession =
                stripeBillingService.createSubscriptionPortalSession(request);
        if (portalSession == null) {
            log.warn("Subscription portal rejected for platform user {} because the active Stripe subscription did not include a customer.",
                     platformUser.getEmail());
            return ResponseEntity.badRequest().body("No active Stripe customer was found for this subscription.");
        }

        storePortalTrackingId(portalSession);
        return ResponseEntity.ok(portalSession.session().getUrl());
    }

    @PostMapping("/billing-webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader) {
        try {
            final Event event = Webhook.constructEvent(payload, signatureHeader, stripeWebhookSecret);
            log.info("Stripe billing webhook received event type {} with id {}.", event.getType(), event.getId());
            if (StripeUtility.CHECKOUT_SESSION_COMPLETED.equals(event.getType())) {
                final Session session = (Session) event.getDataObjectDeserializer().deserializeUnsafe();
                if (session != null && !stripeBillingService.syncPlatformUserTier(session.getMetadata().get("platformUserId"),
                                                                                  session.getMetadata().get("tierId"),
                                                                                  null)) {
                    log.error("Stripe checkout session webhook failed to sync a platform user tier for session {}.", session.getId());
                    eventStreamService.storeAndSend(RedisUtility.SUBSCRIPTION_CHECKOUT_STATUS + session.getId(),
                                                    SUBSCRIPTIONS_CANCEL_REDIRECT_PATH,
                                                    CHECKOUT_REDIRECT_TTL,
                                                    EventStreamUtility.REDIRECT_EVENT);
                    return ResponseEntity.badRequest()
                                         .body(Map.of("message", "An error occurred processing your subscription, support has been notified."));
                }
                if (session != null) {
                    eventStreamService.storeAndSend(RedisUtility.SUBSCRIPTION_CHECKOUT_STATUS + session.getId(),
                                                    SUBSCRIPTIONS_REDIRECT_PATH,
                                                    CHECKOUT_REDIRECT_TTL,
                                                    EventStreamUtility.REDIRECT_EVENT);
                }
            }

            if (StripeUtility.CUSTOMER_SUBSCRIPTION_UPDATED.equals(event.getType())) {
                final Subscription subscription = (Subscription) event.getDataObjectDeserializer().deserializeUnsafe();
                final String portalTrackingId = resolvePortalTrackingId(subscription);
                if (subscription != null && !stripeBillingService.syncPlatformUserTier(subscription.getMetadata().get("platformUserId"),
                                                                                       subscription.getMetadata().get("tierId"),
                                                                                       subscription)) {
                    log.error("Stripe subscription webhook failed to sync a platform user tier for event {} and subscription {}.",
                              event.getType(),
                              subscription.getId());
                    sendPortalRedirect(portalTrackingId, SUBSCRIPTIONS_CANCEL_REDIRECT_PATH);
                    return ResponseEntity.badRequest()
                                         .body(Map.of("message", "An error occurred processing your subscription, support has been notified."));
                }
                sendPortalRedirect(portalTrackingId, SUBSCRIPTIONS_REDIRECT_PATH);
            }
            if (StripeUtility.INVOICE_PAYMENT_FAILED.equals(event.getType()) && !stripeBillingService.handleRecurringInvoicePaymentFailed(event)) {
                log.error("Stripe invoice payment failed webhook could not downgrade the affected platform user for event {}.",
                          event.getId());
                return ResponseEntity.badRequest()
                                     .body(Map.of("message", "An error occurred processing your subscription, support has been notified."));
            }
            log.info("Stripe billing webhook processed event type {} with id {}.", event.getType(), event.getId());
            return ResponseEntity.ok().body(Map.of("message", "User tier updated successfully."));
        } catch (Exception e) {
            log.error("Stripe billing webhook processing failed.", e);
            return ResponseEntity.badRequest().body(Map.of("message", "An error occurred processing your subscription, support has been notified."));
        }
    }

    private void storePortalTrackingId(final PortalSession portalSession) {
        redisStore.put(RedisUtility.SUBSCRIPTION_PORTAL_TRACKING + portalSession.subscriptionId(),
                       portalSession.trackingId(),
                       CHECKOUT_REDIRECT_TTL);
    }

    private String resolvePortalTrackingId(final Subscription subscription) {
        if (subscription == null || StringUtils.isBlank(subscription.getId())) {
            return null;
        }
        return redisStore.get(RedisUtility.SUBSCRIPTION_PORTAL_TRACKING + subscription.getId());
    }

    private void sendPortalRedirect(final String portalTrackingId, final String redirectPath) {
        if (StringUtils.isBlank(portalTrackingId)) {
            return;
        }

        eventStreamService.storeAndSend(RedisUtility.SUBSCRIPTION_CHECKOUT_STATUS + portalTrackingId,
                                        redirectPath,
                                        CHECKOUT_REDIRECT_TTL,
                                        EventStreamUtility.REDIRECT_EVENT);
    }

}
