import { useSubscription } from "../context/SubscriptionContext";
import {
  createSubscriptionCheckoutSession,
  DEFAULT_TIER,
  PAID_TIER,
  downgradeSubscription,
  isDevSubscriptionOverrideAvailable,
  setDevSubscriptionOverrideTier,
  upgradeSubscription,
} from "../services/subscription";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./Subscriptions.css";

function normalizeTierKey(value) {
  return String(value ?? "").trim().toLowerCase();
}

function formatPrice(price) {
  const numericPrice = Number(price || 0);
  return numericPrice <= 0 ? "Free" : `$${numericPrice}/mo`;
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
  const navigate = useNavigate();
  const {
    tierName,
    tiers,
    limits,
    usage,
    status,
    error,
    source,
    refreshTier,
  } = useSubscription();
  const [checkoutError, setCheckoutError] = useState("");
  const [isRedirectingToCheckout, setIsRedirectingToCheckout] = useState(false);
  const canUseDevTierOverride = isDevSubscriptionOverrideAvailable();
  const currentTierKey = normalizeTierKey(tierName);
  const shouldHighlightCurrentPlan = source !== "dev-override" && source !== "dev-bypass";
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
  const currentTierOrder = Number(
    currentTier?.tierOrder ?? currentTier?.price ?? 0
  );

  const handleSwitchToFree = () => {
    setDevSubscriptionOverrideTier(DEFAULT_TIER);
  };

  const handleSwitchToPaid = () => {
    setDevSubscriptionOverrideTier(PAID_TIER);
  };

  const handleSelectPlan = async (plan) => {
    if (!plan || isRedirectingToCheckout) {
      return;
    }
    const planKey = normalizeTierKey(plan?.name);
    const isCurrentPlan = planKey === currentTierKey;
    const canChangePlan = String(plan?.stripePriceId ?? "").trim().length > 0;
    const planTierOrder = Number(plan?.tierOrder ?? plan?.price ?? 0);
    const isDowngrade = planTierOrder < currentTierOrder;
    const downgradeConsequences = buildDowngradeConsequences(plan, usage);

    if (isCurrentPlan || !canChangePlan) {
      return;
    }

    if (isDowngrade) {
      const warningMessage = downgradeConsequences.length > 0
        ? `Switching to ${plan?.name} puts this workspace over the new limit for ${downgradeConsequences.join(", ")}.\n\nThat can require deleting data to fit the selected tier.\n\nDo you want to continue?`
        : `Switching to ${plan?.name} is a downgrade.\n\nIf your workspace later exceeds that tier, you may need to delete data to stay within the new limits.\n\nDo you want to continue?`;

      if (!window.confirm(warningMessage)) {
        return;
      }
    }

    setCheckoutError("");
    setIsRedirectingToCheckout(true);

    try {
      const isInitialPaidCheckout = !currentTier || Number(currentTier?.price || 0) <= 0;

      if (isDowngrade) {
        await downgradeSubscription(plan);
        navigate("/subscriptions/success", {
          state: {
            mode: "downgrade",
            tierName: plan?.name,
            title: "Downgrade scheduled",
            message: `Your subscription will change to ${plan?.name || "the selected tier"} at the end of your current billing period.`,
          },
        });
      } else if (!isInitialPaidCheckout) {
        await upgradeSubscription(plan);
        navigate("/subscriptions/success", {
          state: {
            mode: "upgrade",
            tierName: plan?.name,
            title: "Upgrade submitted",
            message: `${plan?.name || "Your selected tier"} has been submitted successfully. Your workspace will refresh after Stripe finishes syncing the subscription update.`,
          },
        });
      } else {
        const checkoutUrl = await createSubscriptionCheckoutSession(plan);
        window.location.assign(checkoutUrl);
        return;
      }
    } catch (error) {
      setCheckoutError(error?.message || "Unable to start subscription checkout.");
    } finally {
      setIsRedirectingToCheckout(false);
    }
  };

  return (
    <div className="subscriptions-page">
      <section className="subscriptions-intro-card">
        <div className="subscriptions-kicker">Plans</div>
        <h1>Workspace tiers</h1>
        <p>
          Each plan below shows the full client, user, scope, and authority limits for the workspace.
        </p>
        {error ? <div className="subscriptions-error">{error}</div> : null}
        {checkoutError ? <div className="subscriptions-error">{checkoutError}</div> : null}
      </section>

      <section className="subscriptions-tier-grid">
        {sortedTiers.map((plan) => {
          const isCurrentPlan = shouldHighlightCurrentPlan && normalizeTierKey(plan?.name) === currentTierKey;
          const isSelectedPlan = normalizeTierKey(plan?.name) === currentTierKey;
          const planTierOrder = Number(plan?.tierOrder ?? plan?.price ?? 0);
          const isHigherTier = planTierOrder > currentTierOrder;
          const isLowerTier = planTierOrder < currentTierOrder;
          const canCheckoutPlan = !isSelectedPlan && String(plan?.stripePriceId ?? "").trim().length > 0;
          const cardHoverable = !isSelectedPlan;
          const cardClickable = canCheckoutPlan;
          const cardActionLabel = isHigherTier ? "Upgrade plan" : isLowerTier ? "Downgrade plan" : "Select plan";
          const downgradeConsequences = isLowerTier ? buildDowngradeConsequences(plan, usage) : [];
          return (
            <article
              className={`subscriptions-tier-card${isCurrentPlan ? " is-current" : ""}${isLowerTier ? " is-downgrade-option" : ""}${cardHoverable ? " is-hoverable" : ""}${cardClickable ? " is-clickable" : ""}${isRedirectingToCheckout ? " is-busy" : ""}`}
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
              aria-disabled={cardClickable ? isRedirectingToCheckout : undefined}
            >
              <div className="subscriptions-tier-top">
                <div>
                  <div className="subscriptions-tier-name-row">
                    <h2 className="subscriptions-tier-name">{plan?.name}</h2>
                    {isCurrentPlan ? (
                      <span className="subscriptions-tier-current-badge">Current plan</span>
                    ) : isLowerTier ? (
                      <span className="subscriptions-tier-downgrade-badge">
                        {downgradeConsequences.length > 0 ? "Cleanup warning" : "Lower tier"}
                      </span>
                    ) : cardClickable ? (
                      <span className="subscriptions-tier-action-badge">
                        {isRedirectingToCheckout ? "Opening checkout..." : cardActionLabel}
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
                    : "This lower tier may require cleanup if your workspace later exceeds its limits."}
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

      <div className="subscriptions-actions">
        {canUseDevTierOverride ? (
          <>
            <button
              type="button"
              className="client-registry-action client-registry-action-edit"
              onClick={refreshTier}
            >
              Refresh plans
            </button>
            <button
              type="button"
              className="client-registry-action client-registry-action-edit"
              onClick={handleSwitchToFree}
            >
              Switch to free
            </button>
            <button
              type="button"
              className="client-registry-action client-registry-action-edit"
              onClick={handleSwitchToPaid}
            >
              Switch to paid demo
            </button>
          </>
        ) : null}
      </div>
    </div>
  );
}
