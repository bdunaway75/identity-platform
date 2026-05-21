import { useSubscription } from "../context/SubscriptionContext";
import {
  createSubscriptionCheckoutSession,
  downgradeSubscription,
  upgradeSubscription,
} from "../services/subscription";
import { useState } from "react";
import "../../clients/styles/Clients.css";
import "../styles/Subscriptions.css";

function normalizeTierKey(value) {
  return String(value ?? "").trim().toLowerCase();
}

function formatPrice(price) {
  const numericPrice = Number(price || 0);
  return numericPrice <= 0 ? "$0/mo" : `$${numericPrice}/mo`;
}

function formatLimitValue(value) {
  const numericValue = Number(value || 0);
  return numericValue.toLocaleString();
}

function buildTierDescription(tier) {
  const description = String(tier?.description ?? "").trim();
  return description || `${tier?.name || "This tier"} supports ${formatLimitValue(tier?.allowedNumberOfRegisteredClients)} registered clients and ${formatLimitValue(tier?.allowedNumberOfGlobalUsers)} global users.`;
}

function buildTierHighlights(tier) {
  return [
    {
      label: "Registered clients",
      value: formatLimitValue(tier?.allowedNumberOfRegisteredClients),
    },
    {
      label: "Global users",
      value: formatLimitValue(tier?.allowedNumberOfGlobalUsers),
    },
    {
      label: "Global scopes",
      value: formatLimitValue(tier?.allowedNumberOfGlobalScopes),
    },
    {
      label: "Global authorities",
      value: formatLimitValue(tier?.allowedNumberOfGlobalAuthorities),
    },
  ];
}

function buildDowngradeConsequences(plan, usage) {
  const consequences = [];
  const totalClients = Number(usage?.registeredClients || 0);
  const totalUsers = Number(usage?.globalUsers || 0);
  const totalScopes = Number(usage?.globalScopes || 0);
  const totalAuthorities = Number(usage?.globalAuthorities || 0);

  if (totalClients > Number(plan?.allowedNumberOfRegisteredClients || 0)) {
    consequences.push(`registered clients (${totalClients}/${formatLimitValue(plan?.allowedNumberOfRegisteredClients)})`);
  }
  if (totalUsers > Number(plan?.allowedNumberOfGlobalUsers || 0)) {
    consequences.push(`users (${totalUsers}/${formatLimitValue(plan?.allowedNumberOfGlobalUsers)})`);
  }
  if (totalScopes > Number(plan?.allowedNumberOfGlobalScopes || 0)) {
    consequences.push(`scopes (${totalScopes}/${formatLimitValue(plan?.allowedNumberOfGlobalScopes)})`);
  }
  if (totalAuthorities > Number(plan?.allowedNumberOfGlobalAuthorities || 0)) {
    consequences.push(`authorities/roles (${totalAuthorities}/${formatLimitValue(plan?.allowedNumberOfGlobalAuthorities)})`);
  }

  return consequences;
}

export default function Subscriptions() {
  const {
    tierName,
    tiers,
    isDemoUser,
    limits,
    usage,
    status,
    error,
  } = useSubscription();
  const [checkoutError, setCheckoutError] = useState("");
  const [pendingPlanId, setPendingPlanId] = useState("");
  const [pendingActionLabel, setPendingActionLabel] = useState("");
  const currentTierKey = normalizeTierKey(tierName);
  const totalUsers = Number(usage?.globalUsers || 0);
  const totalClients = Number(usage?.registeredClients || 0);
  const totalScopes = Number(usage?.globalScopes || 0);
  const totalAuthorities = Number(usage?.globalAuthorities || 0);
  const allowedUsers = Number(limits?.globalUsers || 0);
  const allowedClients = Number(limits?.registeredClients || 0);
  const sortedTiers = [...(Array.isArray(tiers) ? tiers : [])].sort((left, right) =>
    Number(left?.tierOrder ?? left?.price ?? 0) - Number(right?.tierOrder ?? right?.price ?? 0)
  );
  const currentTier = sortedTiers.find((tier) => normalizeTierKey(tier?.name) === currentTierKey) ?? null;
  const visibleTiers = sortedTiers;
  const currentTierOrder = Number(
    currentTier?.tierOrder ?? currentTier?.price ?? 0
  );
  const isWorking = pendingPlanId.length > 0;

  if (status === "loading") {
    return (
      <div className="subscriptions-loading-state" aria-live="polite">
        <div className="subscriptions-loading-spinner" aria-hidden="true" />
        <div className="subscriptions-loading-copy">Loading plans...</div>
      </div>
    );
  }

  const handleSelectPlan = async (plan) => {
    if (!plan || isWorking || isDemoUser) {
      return;
    }
    const planId = String(plan?.id ?? plan?.name ?? "").trim();
    const planKey = normalizeTierKey(plan?.name);
    const isCurrentPlan = planKey === currentTierKey;
    const canChangePlan = String(plan?.stripePriceId ?? "").trim().length > 0;
    const planTierOrder = Number(plan?.tierOrder ?? plan?.price ?? 0);
    const isDowngrade = planTierOrder < currentTierOrder;
    const isInitialPaidCheckout = !currentTier || Number(currentTier?.price || 0) <= 0;

    if (isCurrentPlan || !canChangePlan) {
      return;
    }

    setCheckoutError("");
    setPendingPlanId(planId);
    setPendingActionLabel(
      isInitialPaidCheckout
        ? "Opening checkout..."
        : "Opening Stripe..."
    );

    try {
      if (isDowngrade) {
        const portalUrl = await downgradeSubscription(plan);
        window.location.assign(portalUrl);
        return;
      } else if (!isInitialPaidCheckout) {
        const portalUrl = await upgradeSubscription(plan);
        window.location.assign(portalUrl);
        return;
      } else {
        const checkoutUrl = await createSubscriptionCheckoutSession(plan);
        window.location.assign(checkoutUrl);
        return;
      }
    } catch (error) {
      setCheckoutError(error?.message || "Unable to start subscription checkout.");
    } finally {
      setPendingPlanId("");
      setPendingActionLabel("");
    }
  };

  return (
    <div className="subscriptions-page">
      <div className="client-shell">
        <div className="client-header subscriptions-header">
          <div className="client-header-copy">
            <div className="client-title">Subscriptions</div>
            <div className="client-subtitle">
              {isDemoUser
                ? "Demo accounts stay on their assigned plan. Subscription changes are disabled."
                : "Compare plan limits and manage subscription access for your platform."}
            </div>
          </div>
        </div>

        {(error || checkoutError) ? (
          <section className="subscriptions-alert-stack" aria-live="polite">
            {error ? <div className="subscriptions-error">{error}</div> : null}
            {checkoutError ? <div className="subscriptions-error">{checkoutError}</div> : null}
          </section>
        ) : null}

        <section className="subscriptions-tier-grid">
          {visibleTiers.map((plan) => {
            const planId = String(plan?.id ?? plan?.name ?? "").trim();
            const isPendingPlan = planId.length > 0 && planId === pendingPlanId;
            const isCurrentPlan = normalizeTierKey(plan?.name) === currentTierKey;
            const isSelectedPlan = normalizeTierKey(plan?.name) === currentTierKey;
            const planTierOrder = Number(plan?.tierOrder ?? plan?.price ?? 0);
            const isHigherTier = planTierOrder > currentTierOrder;
            const isLowerTier = planTierOrder < currentTierOrder;
            const canCheckoutPlan = !isSelectedPlan && String(plan?.stripePriceId ?? "").trim().length > 0;
            const cardHoverable = !isSelectedPlan && !isDemoUser;
            const cardClickable = canCheckoutPlan && !isDemoUser;
            const cardActionLabel = isHigherTier ? "Upgrade plan" : isLowerTier ? "Downgrade plan" : "Select plan";
            const downgradeConsequences = isLowerTier ? buildDowngradeConsequences(plan, usage) : [];
            return (
              <article
                className={`subscriptions-tier-card${isCurrentPlan ? " is-current" : ""}${isLowerTier ? " is-downgrade-option" : ""}${cardHoverable ? " is-hoverable" : ""}${cardClickable ? " is-clickable" : ""}${isPendingPlan ? " is-busy" : ""}${isWorking && !isPendingPlan ? " is-blocked" : ""}`}
                key={plan?.id || plan?.name}
                onClick={() => handleSelectPlan(plan)}
                onKeyDown={(event) => {
                  if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    handleSelectPlan(plan);
                  }
                }}
                role={cardClickable ? "button" : "article"}
                tabIndex={cardClickable ? 0 : -1}
                aria-disabled={cardClickable ? isWorking : undefined}
              >
                <div className="subscriptions-tier-top">
                  <div>
                    <div className="subscriptions-tier-name-row">
                      <h2 className="subscriptions-tier-name">{plan?.name}</h2>
                      {isCurrentPlan ? (
                        <>
                          <span className="subscriptions-tier-current-badge">Current plan</span>
                          {isDemoUser ? (
                            <span className="subscriptions-tier-locked-badge">Locked</span>
                          ) : null}
                        </>
                      ) : isPendingPlan ? (
                        <span className="subscriptions-tier-action-badge is-busy">
                          <span className="subscriptions-tier-action-spinner" aria-hidden="true" />
                          {pendingActionLabel || "Opening checkout..."}
                        </span>
                      ) : isLowerTier ? (
                        <span className="subscriptions-tier-downgrade-badge">
                          {downgradeConsequences.length > 0 ? "Cleanup warning" : "Lower tier"}
                        </span>
                      ) : cardClickable ? (
                        <span className="subscriptions-tier-action-badge">
                          {cardActionLabel}
                        </span>
                      ) : null}
                    </div>
                    <div className="subscriptions-tier-price">{formatPrice(plan?.price)}</div>
                  </div>
                  <p className="subscriptions-tier-description">{buildTierDescription(plan)}</p>
                </div>

                <div className="subscriptions-tier-limits">
                  {buildTierHighlights(plan).map((highlight) => (
                    <div className="subscriptions-tier-limit-row" key={`${plan?.name}-${highlight.label}`}>
                      <span className="subscriptions-tier-limit-label">{highlight.label}</span>
                      <span className="subscriptions-tier-limit-value">{highlight.value}</span>
                    </div>
                  ))}
                </div>

                {isLowerTier ? (
                  <div className="subscriptions-tier-warning">
                    {downgradeConsequences.length > 0
                      ? `This downgrade would leave you over the new limit for ${downgradeConsequences.join(", ")}.`
                      : "This lower tier may require cleanup if your usage later exceeds its limits."}
                  </div>
                ) : null}

                {isCurrentPlan ? (
                  <div className="subscriptions-tier-usage">
                    <div className="subscriptions-tier-usage-row">
                      <span className="subscriptions-tier-usage-label">Clients in use</span>
                      <span className="subscriptions-tier-usage-value">{`${totalClients}/${allowedClients}`}</span>
                    </div>
                    <div className="subscriptions-tier-usage-row">
                      <span className="subscriptions-tier-usage-label">Users in use</span>
                      <span className="subscriptions-tier-usage-value">{`${totalUsers}/${allowedUsers}`}</span>
                    </div>
                    <div className="subscriptions-tier-usage-row">
                      <span className="subscriptions-tier-usage-label">Scopes in use</span>
                      <span className="subscriptions-tier-usage-value">{`${totalScopes}/${formatLimitValue(limits?.globalScopes)}`}</span>
                    </div>
                    <div className="subscriptions-tier-usage-row">
                      <span className="subscriptions-tier-usage-label">Authorities in use</span>
                      <span className="subscriptions-tier-usage-value">{`${totalAuthorities}/${formatLimitValue(limits?.globalAuthorities)}`}</span>
                    </div>
                  </div>
                ) : null}
              </article>
            );
          })}
        </section>
      </div>
    </div>
  );
}
