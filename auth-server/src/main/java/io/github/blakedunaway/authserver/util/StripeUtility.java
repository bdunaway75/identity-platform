package io.github.blakedunaway.authserver.util;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.param.SubscriptionListParams;
import io.github.blakedunaway.authserver.business.model.user.PlatformUser;
import io.github.blakedunaway.authserver.business.model.user.PlatformUserTier;
import io.github.blakedunaway.authserver.business.service.PlatformUserTierService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Objects;

@UtilityClass
public class StripeUtility {

    public static final String CHECKOUT_SESSION_COMPLETED = "checkout.session.completed";
    public static final String CUSTOMER_SUBSCRIPTION_DELETED = "customer.subscription.deleted";
    public static final String CUSTOMER_SUBSCRIPTION_UPDATED = "customer.subscription.updated";
    public static final String CUSTOMER_SUBSCRIPTION_CREATED = "customer.subscription.created";
    public static final String INVOICE_PAID = "invoice.paid";
    public static final String INVOICE_PAYMENT_FAILED = "invoice.payment_failed";
    public static final String BILLING_REASON_SUBSCRIPTION_CREATE = "subscription_create";
    public static final String BILLING_REASON_SUBSCRIPTION_CYCLE = "subscription_cycle";
    public static final String BILLING_REASON_SUBSCRIPTION_UPDATE = "subscription_update";

    public static SubscriptionChange resolveSubscriptionChange(final PlatformUser platformUser,
                                                               final String stripePriceId,
                                                               final PlatformUserTierService platformUserTierService,
                                                               final StripeClient stripeClient) throws StripeException {
        if (stripePriceId.isBlank()) {
            return SubscriptionChange.failure(ResponseEntity.badRequest().body(Map.of("message", "A valid subscription tier is required.")));
        }

        if (platformUser == null) {
            return SubscriptionChange.failure(ResponseEntity.status(401).build());
        }

        final PlatformUserTier platformUserTier = platformUserTierService.findTierByStripePriceId(stripePriceId);
        if (platformUserTier == null || platformUser.getTier() == null) {
            return SubscriptionChange.failure(ResponseEntity.badRequest().body(Map.of("message", "Invalid subscription tier.")));
        }

        final Subscription subscription = findActiveSubscriptionByPlatformUserId(platformUser.getId().toString(), stripeClient);
        if (subscription == null) {
            return SubscriptionChange.failure(ResponseEntity.badRequest().body(Map.of("message", "No active Stripe subscription was found.")));
        }

        final SubscriptionItem subscriptionItem = subscription.getItems() == null || subscription.getItems().getData().isEmpty()
                                                  ? null
                                                  : subscription.getItems().getData().getFirst();
        if (subscriptionItem == null) {
            return SubscriptionChange.failure(ResponseEntity.badRequest().body(Map.of("message", "No active Stripe subscription item was found.")));
        }

        return SubscriptionChange.success(platformUser, platformUserTier, subscription, subscriptionItem);
    }

    public static Subscription findActiveSubscriptionByPlatformUserId(final String platformUserId,
                                                                      final StripeClient stripeClient) throws StripeException {
        if (StringUtils.isBlank(platformUserId)) {
            return null;
        }

        final SubscriptionListParams params = SubscriptionListParams.builder()
                                                                   .setStatus(SubscriptionListParams.Status.ACTIVE)
                                                                   .setLimit(100L)
                                                                   .build();

        for (final Subscription subscription : stripeClient.v1().subscriptions().list(params).autoPagingIterable()) {
            if (subscription.getMetadata() != null
                && Objects.equals(subscription.getMetadata().get("platformUserId"), platformUserId)) {
                return subscription;
            }
        }
        return null;
    }

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SubscriptionChange {

        private final PlatformUser platformUser;

        private final PlatformUserTier platformUserTier;

        private final Subscription subscription;

        private final SubscriptionItem subscriptionItem;

        private final ResponseEntity<Map<String, String>> response;

        public static SubscriptionChange success(final PlatformUser platformUser,
                                                 final PlatformUserTier platformUserTier,
                                                 final Subscription subscription,
                                                 final SubscriptionItem subscriptionItem) {
            return SubscriptionChange.builder()
                                     .platformUser(platformUser)
                                     .platformUserTier(platformUserTier)
                                     .subscription(subscription)
                                     .subscriptionItem(subscriptionItem)
                                     .build();
        }

        public static SubscriptionChange failure(final ResponseEntity<Map<String, String>> response) {
            return SubscriptionChange.builder()
                                     .response(response)
                                     .build();
        }

        public boolean failed() {
            return response != null;
        }
    }
}
