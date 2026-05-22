package io.github.blakedunaway.authserver.business.api.controller;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import io.github.blakedunaway.authserver.business.model.user.PlatformUser;
import io.github.blakedunaway.authserver.business.model.user.PlatformUserTier;
import io.github.blakedunaway.authserver.business.service.EventStreamService;
import io.github.blakedunaway.authserver.business.service.EventStreamService.StreamEvent;
import io.github.blakedunaway.authserver.business.service.PlatformUserTierService;
import io.github.blakedunaway.authserver.business.service.UserService;
import io.github.blakedunaway.authserver.util.EventStreamUtility;
import io.github.blakedunaway.authserver.util.RedisUtility;
import io.github.blakedunaway.authserver.util.StripeEventUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/platform")
@RequiredArgsConstructor
@Slf4j
public class PlatformUserSubscriptionController {

    private final UserService userService;

    private final PlatformUserTierService platformUserTierService;

    private final StripeClient stripeClient;

    private final EventStreamService eventStreamService;

    private static final Duration CHECKOUT_REDIRECT_TTL = Duration.ofMinutes(30);
    private static final String SUBSCRIPTIONS_REDIRECT_PATH = "/subscriptions/success";
    private static final String SUBSCRIPTIONS_CANCEL_REDIRECT_PATH = "/subscriptions/cancel";

    @Value("${auth-server.frontend.origin}")
    private String frontendOrigin;

    @Value("${stripe.webhook-secret}")
    private String stripeWebhookSecret;

    @PreAuthorize("hasRole('PLATFORM_USER')")
    @PostMapping("/subscription")
    public ResponseEntity<String> createCheckoutSession(@AuthenticationPrincipal final Jwt jwt,
                                                        @RequestBody final String stripePriceId) throws StripeException {
        final PlatformUser platformUser = userService.loadPlatformUserByEmail(jwt.getSubject());
        if (platformUser == null || platformUser.isDemoUser()) {
            log.warn("Checkout session creation rejected because the platform user {} could not be resolved, or is a demo user.", jwt.getSubject());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (stripePriceId.isBlank()) {
            log.warn("Checkout session creation rejected for platform user {} because the requested tier was blank.", jwt.getSubject());
            return ResponseEntity.badRequest().body("A valid subscription tier is required.");
        }
        final PlatformUserTier platformUserTier = platformUserTierService.findTierByStripePriceId(stripePriceId);
        if (platformUserTier == null || platformUserTier.getId() == null) {
            log.warn("Checkout session creation rejected for platform user {} because stripe price id {} did not resolve to a tier.", jwt.getSubject(), stripePriceId);
            return ResponseEntity.badRequest().body("A valid subscription tier is required.");
        }
        final SessionCreateParams params = SessionCreateParams.builder()
                                                                     .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                                                                     .setSuccessUrl(
                                                                             frontendOrigin + "/subscriptions/checkout?session_id={CHECKOUT_SESSION_ID}")
                                                                     .setCancelUrl(frontendOrigin + "/subscriptions/cancel")
                                                                     .addLineItem(
                                                                             SessionCreateParams.LineItem.builder()
                                                                                                         .setPrice(platformUserTier.getStripePriceId())
                                                                                                         .setQuantity(1L)
                                                                                                         .build()
                                                                     )
                                                                     .putMetadata("tierId", platformUserTier.getId().toString())
                                                                     .putMetadata("platformUserId", platformUser.getId().toString())
                                                                     .setSubscriptionData(
                                                                             SessionCreateParams.SubscriptionData.builder()
                                                                                                                .putMetadata("platformUserId", platformUser.getId().toString())
                                                                                                                .putMetadata("tierId", platformUserTier.getId().toString())
                                                                                                                .build()
                                                                     )
                                                                     .build();

        final Session session = stripeClient.v1().checkout().sessions().create(params);
        return ResponseEntity.ok(session.getUrl());
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
    @PostMapping(value = "/subscription/downgrade")
    public ResponseEntity<Map<String, String>> downgradeSubscription(@AuthenticationPrincipal final Jwt jwt,
                                                                     @RequestBody final String stripePriceId) throws StripeException {
        final SubscriptionChangeRequest request = resolveSubscriptionChangeRequest(jwt, stripePriceId);
        if (request.failureResponse() != null) {
            return request.failureResponse();
        }
        if (request.platformUserTier().getTierOrder() >= request.platformUser().getTier().getTierOrder()) {
            log.warn("Platform user {} attempted an invalid downgrade to tier {}.", jwt.getSubject(), stripePriceId);
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid subscription tier."));
        }
        updateSubscriptionTier(request);
        return ResponseEntity.ok(Map.of("message", "Subscription downgrade submitted successfully."));
    }

    @PreAuthorize("hasRole('PLATFORM_USER')")
    @PostMapping(value = "/subscription/upgrade")
    public ResponseEntity<Map<String, String>> upgradeSubscription(@AuthenticationPrincipal final Jwt jwt,
                                                                   @RequestBody final String stripePriceId) throws StripeException {
        final SubscriptionChangeRequest request = resolveSubscriptionChangeRequest(jwt, stripePriceId);
        if (request.failureResponse() != null) {
            return request.failureResponse();
        }
        if (request.platformUserTier().getTierOrder() <= request.platformUser().getTier().getTierOrder()) {
            log.warn("Platform user {} attempted an invalid upgrade to tier {}.", jwt.getSubject(), stripePriceId);
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid subscription tier."));
        }
        updateSubscriptionTier(request);
        return ResponseEntity.ok(Map.of("message", "Subscription upgrade submitted successfully."));
    }

    @PostMapping("/billing-webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader) {
        try {
            final Event event = Webhook.constructEvent(payload, signatureHeader, stripeWebhookSecret);
            if (StripeEventUtility.CHECKOUT_SESSION_COMPLETED.equals(event.getType())) {
                final Session session = (Session) event.getDataObjectDeserializer().deserializeUnsafe();
                if (session != null && !syncPlatformUserTier(session.getMetadata().get("platformUserId"),
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

            if (StripeEventUtility.CUSTOMER_SUBSCRIPTION_UPDATED.equals(event.getType())) {
                final Subscription subscription = (Subscription) event.getDataObjectDeserializer().deserializeUnsafe();
                if (subscription != null && !syncPlatformUserTier(subscription.getMetadata().get("platformUserId"),
                                                                  subscription.getMetadata().get("tierId"),
                                                                  subscription)) {
                    log.error("Stripe subscription update webhook failed to sync a platform user tier for subscription {}.", subscription.getId());
                    return ResponseEntity.badRequest()
                                         .body(Map.of("message", "An error occurred processing your subscription, support has been notified."));
                }
            }
            return ResponseEntity.ok().body(Map.of("message", "User tier updated successfully."));
        } catch (Exception e) {
            log.error("Stripe billing webhook processing failed.", e);
            return ResponseEntity.badRequest().body(Map.of("message", "An error occurred processing your subscription, support has been notified."));
        }
    }

    private Subscription findActiveSubscriptionByPlatformUserId(final UUID platformUserId) throws StripeException {
        final SubscriptionListParams params = SubscriptionListParams.builder()
                                                                   .setStatus(SubscriptionListParams.Status.ACTIVE)
                                                                   .setLimit(100L)
                                                                   .build();

        for (final Subscription subscription : stripeClient.v1().subscriptions().list(params).autoPagingIterable()) {
            if (subscription.getMetadata() != null
                && Objects.equals(subscription.getMetadata().get("platformUserId"), platformUserId.toString())) {
                return subscription;
            }
        }
        return null;
    }

    private SubscriptionChangeRequest resolveSubscriptionChangeRequest(final Jwt jwt,
                                                                      final String stripePriceId) throws StripeException {
        if (stripePriceId.isBlank()) {
            log.warn("Subscription change rejected because the requested stripe price id was blank.");
            return new SubscriptionChangeRequest(null,
                                                 null,
                                                 null,
                                                 ResponseEntity.badRequest().body(Map.of("message", "A valid subscription tier is required.")));
        }

        final PlatformUser platformUser = userService.loadPlatformUserByEmail(jwt.getSubject());
        if (platformUser == null) {
            log.warn("Subscription change rejected because the platform user {} could not be resolved.", jwt.getSubject());
            return new SubscriptionChangeRequest(null, null, null, ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        final PlatformUserTier platformUserTier = platformUserTierService.findTierByStripePriceId(stripePriceId);
        if (platformUserTier == null || platformUser.getTier() == null) {
            log.warn("Subscription change rejected for platform user {} because stripe price id {} did not resolve to a valid tier.", jwt.getSubject(), stripePriceId);
            return new SubscriptionChangeRequest(null,
                                                 null,
                                                 null,
                                                 ResponseEntity.badRequest().body(Map.of("message", "Invalid subscription tier.")));
        }

        final Subscription subscription = findActiveSubscriptionByPlatformUserId(platformUser.getId());
        if (subscription == null) {
            log.warn("Subscription change rejected for platform user {} because no active Stripe subscription was found.", jwt.getSubject());
            return new SubscriptionChangeRequest(null,
                                                 null,
                                                 null,
                                                 ResponseEntity.badRequest().body(Map.of("message", "No active Stripe subscription was found.")));
        }

        final SubscriptionItem subscriptionItem = subscription.getItems() == null || subscription.getItems().getData().isEmpty()
                                                  ? null
                                                  : subscription.getItems().getData().getFirst();
        if (subscriptionItem == null) {
            log.warn("Subscription change rejected for platform user {} because no Stripe subscription item was found.", jwt.getSubject());
            return new SubscriptionChangeRequest(null,
                                                 null,
                                                 null,
                                                 ResponseEntity.badRequest().body(Map.of("message", "No active Stripe subscription item was found.")));
        }

        return new SubscriptionChangeRequest(platformUser, platformUserTier, subscriptionItem, null);
    }

    private void updateSubscriptionTier(final SubscriptionChangeRequest request) throws StripeException {
        stripeClient.v1()
                    .subscriptions()
                    .update(
                            request.subscriptionItem().getSubscription(),
                            SubscriptionUpdateParams.builder()
                                                    .addItem(
                                                            SubscriptionUpdateParams.Item.builder()
                                                                                         .setId(request.subscriptionItem().getId())
                                                                                         .setPrice(request.platformUserTier().getStripePriceId())
                                                                                         .build()
                                                    )
                                                    .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.NONE)
                                                    .putMetadata("platformUserId", request.platformUser().getId().toString())
                                                    .putMetadata("tierId", request.platformUserTier().getId().toString())
                                                    .build()
                    );
    }

    private boolean syncPlatformUserTier(final String platformUserId,
                                         final String tierId,
                                         final Subscription subscription) {
        if (StringUtils.isBlank(platformUserId)) {
            return false;
        }

        final PlatformUser platformUser = userService.loadPlatformUserById(UUID.fromString(platformUserId));
        final PlatformUserTier tier = resolvePlatformUserTier(tierId, subscription);
        if (tier == null || platformUser == null) {
            return false;
        }

        userService.savePlatformUser(PlatformUser.from(platformUser).tier(tier).build());
        return true;
    }

    private PlatformUserTier resolvePlatformUserTier(final String tierId,
                                                     final Subscription subscription) {
        if (StringUtils.isNotBlank(tierId)) {
            final PlatformUserTier tier = platformUserTierService.findTierById(tierId);
            if (tier != null) {
                return tier;
            }
        }

        if (subscription == null || subscription.getItems() == null || subscription.getItems().getData().isEmpty()) {
            return null;
        }

        final SubscriptionItem subscriptionItem = subscription.getItems().getData().getFirst();
        if (subscriptionItem == null || subscriptionItem.getPrice() == null || subscriptionItem.getPrice().getId() == null) {
            return null;
        }

        return platformUserTierService.findTierByStripePriceId(subscriptionItem.getPrice().getId());
    }

    private record SubscriptionChangeRequest(PlatformUser platformUser,
                                             PlatformUserTier platformUserTier,
                                             SubscriptionItem subscriptionItem,
                                             ResponseEntity<Map<String, String>> failureResponse) {
    }


}
