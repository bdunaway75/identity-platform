package io.github.blakedunaway.authserver.business.service;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import io.github.blakedunaway.authserver.business.model.user.PlatformUser;
import io.github.blakedunaway.authserver.business.model.user.PlatformUserTier;
import io.github.blakedunaway.authserver.util.StripeUtility;
import io.github.blakedunaway.authserver.util.StripeUtility.SubscriptionChange;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeBillingService {

    private static final String PURCHASE_EMAIL_TEMPLATE = "subscription-purchase.ftlh";

    private static final String DOWNGRADE_EMAIL_TEMPLATE = "subscription-downgrade.ftlh";

    private static final String PAYMENT_FAILED_EMAIL_TEMPLATE = "subscription-payment-failed.ftlh";

    private static final String PURCHASE_EMAIL_SUBJECT = "Your subscription is active";

    private static final String DOWNGRADE_EMAIL_SUBJECT = "Your subscription has been downgraded";

    private static final String PAYMENT_FAILED_EMAIL_SUBJECT = "Action required: subscription payment failed";

    private static final DateTimeFormatter BILLING_EMAIL_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a z");

    private final StripeClient stripeClient;

    private final UserService userService;

    private final PlatformUserTierService platformUserTierService;

    private final EmailService emailService;

    @Value("${auth-server.frontend.origin}")
    private String frontendOrigin;

    @Value("${stripe.billing-portal.configuration-id}")
    private String billingPortalConfigurationId;

    @Value("${auth-server.support-email}")
    private String supportEmail;

    @PostConstruct
    void initializePortalConfiguration() {
        if (StringUtils.isBlank(billingPortalConfigurationId)) {
            throw new IllegalStateException("stripe.billing-portal.configuration-id must be configured.");
        }
        try {
            stripeClient.billingPortal()
                        .configurations()
                        .retrieve(billingPortalConfigurationId);
        } catch (StripeException e) {
            log.error("Failed to validate Stripe portal configuration at startup.", e);
            throw new IllegalStateException("Failed to validate Stripe portal configuration.", e);
        }
    }

    public String createCheckoutSession(final PlatformUser platformUser,
                                        final PlatformUserTier platformUserTier) throws StripeException {
        final com.stripe.param.checkout.SessionCreateParams params = com.stripe.param.checkout.SessionCreateParams.builder()
                                                                                                                  .setMode(com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION)
                                                                                                                  .setSuccessUrl(frontendOrigin + "/subscriptions/checkout?session_id={CHECKOUT_SESSION_ID}")
                                                                                                                  .setCancelUrl(frontendOrigin + "/subscriptions/cancel")
                                                                                                                  .addLineItem(
                                                                                                                          com.stripe.param.checkout.SessionCreateParams.LineItem.builder()
                                                                                                                                                                                .setPrice(
                                                                                                                                                                                        platformUserTier.getStripePriceId())
                                                                                                                                                                                .setQuantity(
                                                                                                                                                                                        1L)
                                                                                                                                                                                .build()
                                                                                                                  )
                                                                                                                  .putMetadata("tierId",
                                                                                                                               platformUserTier.getId()
                                                                                                                                               .toString())
                                                                                                                  .putMetadata("platformUserId",
                                                                                                                               platformUser.getId()
                                                                                                                                           .toString())
                                                                                                                  .setSubscriptionData(
                                                                                                                          com.stripe.param.checkout.SessionCreateParams.SubscriptionData.builder()
                                                                                                                                                                                        .putMetadata(
                                                                                                                                                                                                "platformUserId",
                                                                                                                                                                                                platformUser.getId()
                                                                                                                                                                                                            .toString())
                                                                                                                                                                                        .putMetadata(
                                                                                                                                                                                                "tierId",
                                                                                                                                                                                                platformUserTier.getId()
                                                                                                                                                                                                                .toString())
                                                                                                                                                                                        .build()
                                                                                                                  )
                                                                                                                  .build();

        final com.stripe.model.checkout.Session session = stripeClient.v1().checkout().sessions().create(params);
        return session.getUrl();
    }

    public PortalSession createSubscriptionPortalSession(final SubscriptionChange request) throws StripeException {
        if (request == null
            || request.getSubscription() == null
            || request.getSubscriptionItem() == null
            || request.getPlatformUserTier() == null
            || StringUtils.isBlank(request.getSubscription().getCustomer())
            || StringUtils.isBlank(request.getSubscription().getId())
            || StringUtils.isBlank(request.getSubscriptionItem().getId())
            || StringUtils.isBlank(request.getPlatformUserTier().getStripePriceId())) {
            return null;
        }

        final String portalTrackingId = UUID.randomUUID().toString();
        final String portalReturnUrl = frontendOrigin + "/subscriptions/checkout?session_id=" + portalTrackingId;
        final com.stripe.param.billingportal.SessionCreateParams params = com.stripe.param.billingportal.SessionCreateParams.builder()
                                                                                                                            .setCustomer(request.getSubscription()
                                                                                                                                                .getCustomer())
                                                                                                                            .setConfiguration(
                                                                                                                                    billingPortalConfigurationId)
                                                                                                                            .setReturnUrl(portalReturnUrl)
                                                                                                                            .setFlowData(
                                                                                                                                    com.stripe.param.billingportal.SessionCreateParams.FlowData.builder()
                                                                                                                                                                                               .setType(
                                                                                                                                                                                                       com.stripe.param.billingportal.SessionCreateParams.FlowData.Type.SUBSCRIPTION_UPDATE_CONFIRM)
                                                                                                                                                                                               .setAfterCompletion(
                                                                                                                                                                                                       com.stripe.param.billingportal.SessionCreateParams.FlowData.AfterCompletion.builder()
                                                                                                                                                                                                                                                                                  .setType(
                                                                                                                                                                                                                                                                                          com.stripe.param.billingportal.SessionCreateParams.FlowData.AfterCompletion.Type.REDIRECT)
                                                                                                                                                                                                                                                                                  .setRedirect(
                                                                                                                                                                                                                                                                                          com.stripe.param.billingportal.SessionCreateParams.FlowData.AfterCompletion.Redirect.builder()
                                                                                                                                                                                                                                                                                                                                                                            .setReturnUrl(
                                                                                                                                                                                                                                                                                                                                                                                    portalReturnUrl)
                                                                                                                                                                                                                                                                                                                                                                            .build())
                                                                                                                                                                                                                                                                                  .build())
                                                                                                                                                                                               .setSubscriptionUpdateConfirm(
                                                                                                                                                                                                       com.stripe.param.billingportal.SessionCreateParams.FlowData.SubscriptionUpdateConfirm.builder()
                                                                                                                                                                                                                                                                                            .setSubscription(
                                                                                                                                                                                                                                                                                                    request.getSubscription()
                                                                                                                                                                                                                                                                                                           .getId())
                                                                                                                                                                                                                                                                                            .addItem(
                                                                                                                                                                                                                                                                                                    com.stripe.param.billingportal.SessionCreateParams.FlowData.SubscriptionUpdateConfirm.Item.builder()
                                                                                                                                                                                                                                                                                                                                                                                              .setId(
                                                                                                                                                                                                                                                                                                                                                                                                      request.getSubscriptionItem()
                                                                                                                                                                                                                                                                                                                                                                                                             .getId())
                                                                                                                                                                                                                                                                                                                                                                                              .setPrice(
                                                                                                                                                                                                                                                                                                                                                                                                      request.getPlatformUserTier()
                                                                                                                                                                                                                                                                                                                                                                                                             .getStripePriceId())
                                                                                                                                                                                                                                                                                                                                                                                              .setQuantity(
                                                                                                                                                                                                                                                                                                                                                                                                      request.getSubscriptionItem()
                                                                                                                                                                                                                                                                                                                                                                                                             .getQuantity() == null
                                                                                                                                                                                                                                                                                                                                                                                                      ? 1L
                                                                                                                                                                                                                                                                                                                                                                                                      : request.getSubscriptionItem()
                                                                                                                                                                                                                                                                                                                                                                                                               .getQuantity())
                                                                                                                                                                                                                                                                                                                                                                                              .build()
                                                                                                                                                                                                                                                                                            )
                                                                                                                                                                                                                                                                                            .build()
                                                                                                                                                                                               )
                                                                                                                                                                                               .build()
                                                                                                                            )
                                                                                                                            .build();

        return new PortalSession(stripeClient.billingPortal()
                                            .sessions()
                                            .create(params),
                                 portalTrackingId,
                                 request.getSubscription().getId());
    }

    public record PortalSession(com.stripe.model.billingportal.Session session, String trackingId, String subscriptionId) {}

    public boolean syncPlatformUserTier(final String platformUserId,
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

        final PlatformUserTier previousTier = platformUser.getTier();
        if (previousTier.getTierOrder() == tier.getTierOrder()) {
            return true;
        }
        userService.savePlatformUser(PlatformUser.from(platformUser).tier(tier).build());
        sendTierChangeEmailIfNeeded(platformUser, previousTier, tier);
        return true;
    }

    public boolean handleRecurringInvoicePaymentFailed(final Event event) throws StripeException {
        final Invoice invoice = (Invoice) event.getDataObjectDeserializer().deserializeUnsafe();
        if (invoice == null) {
            return false;
        }

        final String billingReason = String.valueOf(invoice.getBillingReason());
        if (!StripeUtility.BILLING_REASON_SUBSCRIPTION_CYCLE.equalsIgnoreCase(billingReason)) {
            log.info("Stripe invoice payment failed event {} ignored because billing reason was {}.",
                     event.getId(),
                     billingReason);
            return true;
        }

        final String subscriptionId = resolveInvoiceSubscriptionId(invoice);
        if (StringUtils.isBlank(subscriptionId)) {
            log.warn("Stripe invoice payment failed event {} could not be processed because no subscription id was present.",
                     event.getId());
            return false;
        }

        final Subscription subscription = stripeClient.v1().subscriptions().retrieve(subscriptionId);
        if (subscription == null || subscription.getMetadata() == null) {
            log.warn("Stripe invoice payment failed event {} could not resolve subscription metadata for subscription {}.",
                     event.getId(),
                     subscriptionId);
            return false;
        }

        return performPaymentFailedOperations(subscription.getMetadata().get("platformUserId"));
    }

    private boolean performPaymentFailedOperations(final String platformUserId) {
        if (StringUtils.isBlank(platformUserId)) {
            return false;
        }

        final PlatformUser platformUser = userService.loadPlatformUserById(UUID.fromString(platformUserId));
        final PlatformUserTier starterTier = resolveStarterTier();
        if (platformUser == null) {
            return false;
        }

        final PlatformUserTier previousTier = platformUser.getTier();
        userService.savePlatformUser(PlatformUser.from(platformUser).tier(starterTier).build());
        sendPaymentFailedEmail(platformUser, previousTier, starterTier);
        return true;
    }

    private PlatformUserTier resolvePlatformUserTier(final String tierId,
                                                     final Subscription subscription) {
        if (subscription != null && subscription.getItems() != null && !subscription.getItems().getData().isEmpty()) {
            final SubscriptionItem subscriptionItem = subscription.getItems().getData().getFirst();
            if (subscriptionItem != null && subscriptionItem.getPrice() != null && subscriptionItem.getPrice().getId() != null) {
                final PlatformUserTier tier = platformUserTierService.findTierByStripePriceId(subscriptionItem.getPrice().getId());
                if (tier != null) {
                    return tier;
                }
            }
        }

        return StringUtils.isBlank(tierId) ? null : platformUserTierService.findTierById(tierId);
    }

    private String resolveInvoiceSubscriptionId(final Invoice invoice) {
        if (invoice == null || invoice.getParent() == null || invoice.getParent().getSubscriptionDetails() == null) {
            return null;
        }

        return invoice.getParent()
                      .getSubscriptionDetails()
                      .getSubscription();
    }

    private PlatformUserTier resolveStarterTier() {
        return platformUserTierService.findAllTiers()
                                      .stream()
                                      .filter(Objects::nonNull)
                                      .min(Comparator.comparingInt(PlatformUserTier::getTierOrder))
                                      .orElseThrow(() -> new IllegalStateException("No tier found to downgrade to"));
    }

    private void sendTierChangeEmailIfNeeded(final PlatformUser platformUser,
                                             final PlatformUserTier previousTier,
                                             final PlatformUserTier currentTier) {
        if (previousTier == null || currentTier.isHigherThan(previousTier)) {
            emailService.sendTemplateEmail(platformUser.getEmail(),
                                           PURCHASE_EMAIL_SUBJECT,
                                           PURCHASE_EMAIL_TEMPLATE,
                                           Map.of("emailAddress", platformUser.getEmail(),
                                                  "currentTierName", currentTier.getName(),
                                                  "previousTierName", previousTier == null ? "None" : previousTier.getName(),
                                                  "manageSubscriptionUrl", frontendOrigin + "/subscriptions",
                                                  "supportEmail", supportEmail));
        }

        emailService.sendTemplateEmail(platformUser.getEmail(),
                                       DOWNGRADE_EMAIL_SUBJECT,
                                       DOWNGRADE_EMAIL_TEMPLATE,
                                       Map.of("emailAddress", platformUser.getEmail(),
                                              "currentTierName", currentTier.getName(),
                                              "previousTierName", previousTier.getName(),
                                              "manageSubscriptionUrl", frontendOrigin + "/subscriptions",
                                              "supportEmail", supportEmail));

    }

    private void sendPaymentFailedEmail(final PlatformUser platformUser,
                                        final PlatformUserTier previousTier,
                                        final PlatformUserTier downgradedTier) {
        if (platformUser == null || downgradedTier == null || StringUtils.isBlank(platformUser.getEmail())) {
            return;
        }

        final ZonedDateTime purgeDeadline = ZonedDateTime.now(ZoneId.systemDefault()).plusDays(2);
        emailService.sendTemplateEmail(platformUser.getEmail(),
                                       PAYMENT_FAILED_EMAIL_SUBJECT,
                                       PAYMENT_FAILED_EMAIL_TEMPLATE,
                                       Map.of("emailAddress", platformUser.getEmail(),
                                              "previousTierName", previousTier == null ? "your previous tier" : previousTier.getName(),
                                              "currentTierName", downgradedTier.getName(),
                                              "purgeDeadline", BILLING_EMAIL_DATE_FORMATTER.format(purgeDeadline),
                                              "manageSubscriptionUrl", frontendOrigin + "/subscriptions",
                                              "supportEmail", supportEmail,
                                              "allowedRegisteredClients", downgradedTier.getAllowedNumberOfRegisteredClients(),
                                              "allowedGlobalUsers", downgradedTier.getAllowedNumberOfGlobalUsers(),
                                              "allowedGlobalScopes", downgradedTier.getAllowedNumberOfGlobalScopes(),
                                              "allowedGlobalAuthorities", downgradedTier.getAllowedNumberOfGlobalAuthorities()));
    }

}
