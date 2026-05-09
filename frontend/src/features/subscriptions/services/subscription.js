import { getValidAccessToken, authenticatedFetch } from "../../auth/services/session";
import { APP_ENDPOINTS } from "../../../shared/config/endpoints";
import { fetchDashboard, fetchPlatformUserTiers } from "../../clients/services/platform";

export const DEFAULT_TIER = "free";

const PENDING_SUBSCRIPTION_CHECKOUT_KEY = "pending-subscription-checkout";
const SUBSCRIPTION_SUCCESS_CACHE_MS = 120_000;
const SUBSCRIPTION_FAILURE_CACHE_MS = 10_000;
const TIER_LIMITS_BY_KEY = {
  free: {
    allowedNumberOfRegisteredClients: 0,
    allowedNumberOfGlobalUsers: 0,
  },
  paid: {
    allowedNumberOfRegisteredClients: 5,
    allowedNumberOfGlobalUsers: 500,
  },
};

async function readErrorMessage(response, fallbackMessage) {
  const rawBody = (await response.text()).trim();
  if (!rawBody) {
    return fallbackMessage;
  }

  try {
    const parsedBody = JSON.parse(rawBody);
    if (typeof parsedBody?.message === "string" && parsedBody.message.trim().length > 0) {
      return parsedBody.message.trim();
    }
  } catch {
    // Fall through to the raw response body.
  }

  return rawBody;
}

let cachedTierResult = null;
let cachedTierResultAt = 0;
let cachedTierError = null;
let cachedTierErrorAt = 0;
let inFlightTierRequest = null;

function getTierLimitsByKey(tierKey) {
  return TIER_LIMITS_BY_KEY[tierKey] || TIER_LIMITS_BY_KEY[DEFAULT_TIER];
}

function normalizeTierKey(value) {
  return String(value ?? "").trim().toLowerCase();
}

function buildSubscriptionSnapshot({
  tierKey,
  tierName,
  source,
  tiers,
  isDemoUser,
  isAdmin,
  allowedNumberOfRegisteredClients,
  allowedNumberOfGlobalUsers,
  allowedNumberOfGlobalScopes,
  allowedNumberOfGlobalAuthorities,
  totalRegisteredClients,
  totalUsers,
  totalScopes,
  totalAuthorities,
  totalRoles,
}) {
  return {
    tier: tierKey,
    tierName: tierName,
    source,
    tiers: Array.isArray(tiers) ? tiers : [],
    isDemoUser: Boolean(isDemoUser),
    isAdmin: Boolean(isAdmin),
    allowedNumberOfRegisteredClients: allowedNumberOfRegisteredClients,
    allowedNumberOfGlobalUsers: allowedNumberOfGlobalUsers,
    allowedNumberOfGlobalScopes: allowedNumberOfGlobalScopes,
    allowedNumberOfGlobalAuthorities: allowedNumberOfGlobalAuthorities,
    totalRegisteredClients: totalRegisteredClients,
    totalUsers: totalUsers,
    totalScopes: totalScopes,
    totalAuthorities: totalAuthorities,
    totalRoles: totalRoles
  };
}

function buildSubscriptionFromDashboardAndTiers(dashboardPayload, tiersPayload) {
  const dashboardTier = dashboardPayload?.tier;
  const tierKey = normalizeTierKey(dashboardTier?.name || DEFAULT_TIER);
  const tiers = Array.isArray(tiersPayload) ? tiersPayload : [];
  const fallbackLimits = getTierLimitsByKey(tierKey);
  const matchingTier = tiers.find((tier) => normalizeTierKey(tier?.name) === tierKey);
  const currentTier = matchingTier ?? dashboardTier ?? null;
  const snapshotTiers = currentTier && !matchingTier
    ? [currentTier, ...tiers]
    : tiers;

  return buildSubscriptionSnapshot({
    tierKey,
    tierName: currentTier?.name ?? tierKey,
    source: matchingTier ? "tiers" : "dashboard",
    tiers: snapshotTiers,
    isDemoUser: Boolean(dashboardPayload?.isDemoUser),
    isAdmin: Boolean(dashboardPayload?.isAdmin),
    allowedNumberOfRegisteredClients: currentTier?.allowedNumberOfRegisteredClients ??
      fallbackLimits.allowedNumberOfRegisteredClients,
    allowedNumberOfGlobalUsers: currentTier?.allowedNumberOfGlobalUsers ??
      fallbackLimits.allowedNumberOfGlobalUsers,
    allowedNumberOfGlobalScopes: currentTier?.allowedNumberOfGlobalScopes ?? 0,
    allowedNumberOfGlobalAuthorities: currentTier?.allowedNumberOfGlobalAuthorities ?? 0,
    totalRegisteredClients: dashboardPayload?.totalRegisteredClients,
    totalUsers: dashboardPayload?.totalUsers,
    totalScopes: dashboardPayload?.totalScopes,
    totalAuthorities: dashboardPayload?.totalAuthorities,
    totalRoles: dashboardPayload?.totalRoles,
  });
}

function getCachedTierResult() {
  if (!cachedTierResult) {
    return null;
  }

  if (Date.now() - cachedTierResultAt > SUBSCRIPTION_SUCCESS_CACHE_MS) {
    cachedTierResult = null;
    cachedTierResultAt = 0;
    return null;
  }

  return cachedTierResult;
}

function getCachedTierError() {
  if (!cachedTierError) {
    return null;
  }

  if (Date.now() - cachedTierErrorAt > SUBSCRIPTION_FAILURE_CACHE_MS) {
    cachedTierError = null;
    cachedTierErrorAt = 0;
    return null;
  }

  return cachedTierError;
}

function cacheTierResult(result) {
  cachedTierResult = result;
  cachedTierResultAt = Date.now();
  cachedTierError = null;
  cachedTierErrorAt = 0;
}

function cacheTierError(error) {
  cachedTierError = error;
  cachedTierErrorAt = Date.now();
}

function canUseSessionStorage() {
  return typeof window !== "undefined" && typeof window.sessionStorage !== "undefined";
}

export function setPendingSubscriptionCheckout(checkout) {
  if (!canUseSessionStorage()) {
    return;
  }

  try {
    window.sessionStorage.setItem(PENDING_SUBSCRIPTION_CHECKOUT_KEY, JSON.stringify({
      tierId: checkout?.tierId ?? null,
      tierName: String(checkout?.tierName ?? "").trim(),
      price: Number(checkout?.price ?? 0),
      startedAt: Date.now(),
    }));
  } catch {
    // Ignore storage failures.
  }
}

export function getPendingSubscriptionCheckout() {
  if (!canUseSessionStorage()) {
    return null;
  }

  try {
    const rawValue = window.sessionStorage.getItem(PENDING_SUBSCRIPTION_CHECKOUT_KEY);
    return rawValue ? JSON.parse(rawValue) : null;
  } catch {
    return null;
  }
}

export function clearPendingSubscriptionCheckout() {
  if (!canUseSessionStorage()) {
    return;
  }

  try {
    window.sessionStorage.removeItem(PENDING_SUBSCRIPTION_CHECKOUT_KEY);
  } catch {
    // Ignore storage cleanup failures.
  }
}

export function clearSubscriptionTierCache() {
  cachedTierResult = null;
  cachedTierResultAt = 0;
  cachedTierError = null;
  cachedTierErrorAt = 0;
  inFlightTierRequest = null;
}

export async function fetchSubscriptionCheckoutStatus(sessionId) {
  const normalizedSessionId = String(sessionId ?? "").trim();
  if (!normalizedSessionId) {
    throw new Error("Checkout session id is required.");
  }

  const response = await fetch(
    `${APP_ENDPOINTS.platform.subscriptionStatus}?session_id=${encodeURIComponent(normalizedSessionId)}`,
    {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
    }
  );

  if (!response.ok) {
    throw new Error(`Subscription status lookup failed with status ${response.status}.`);
  }

  const payload = await response.json();
  return String(payload?.status ?? "").trim().toLowerCase() || "pending";
}

export async function createSubscriptionCheckoutSession(platformUserTier) {
  if (!platformUserTier?.id || !platformUserTier?.stripePriceId) {
    throw new Error("A billable subscription tier is required.");
  }

  const accessToken = await getValidAccessToken("Missing access token for subscription checkout.");
  const response = await authenticatedFetch(APP_ENDPOINTS.platform.subscription, {
    method: "POST",
    headers: {
      Accept: "text/plain",
      "Content-Type": "text/plain",
      Authorization: `Bearer ${accessToken}`,
    },
    body: platformUserTier.stripePriceId,
  });

  if (!response.ok) {
    const message = await readErrorMessage(
      response,
      `Subscription checkout failed with status ${response.status}.`
    );
    throw new Error(message || `Subscription checkout failed with status ${response.status}.`);
  }

  const checkoutUrl = (await response.text()).trim();
  if (!checkoutUrl) {
    throw new Error("Subscription checkout did not return a Stripe checkout URL.");
  }

  setPendingSubscriptionCheckout({
    tierId: platformUserTier.id,
    tierName: platformUserTier.name,
    price: platformUserTier.price,
  });
  return checkoutUrl;
}

async function changeExistingSubscription(endpoint, platformUserTier, defaultErrorMessage) {
  if (!platformUserTier?.id || !platformUserTier?.stripePriceId) {
    throw new Error("A billable subscription tier is required.");
  }

  const accessToken = await getValidAccessToken("Missing access token for subscription change.");
  const response = await authenticatedFetch(endpoint, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "text/plain",
      Authorization: `Bearer ${accessToken}`,
    },
    body: platformUserTier.stripePriceId,
  });

  if (!response.ok) {
    const message = await readErrorMessage(
      response,
      `${defaultErrorMessage} failed with status ${response.status}.`
    );
    throw new Error(message || `${defaultErrorMessage} failed with status ${response.status}.`);
  }

  clearSubscriptionTierCache();
  return response.status === 204 ? {} : response.json().catch(() => ({}));
}

export async function upgradeSubscription(platformUserTier) {
  return changeExistingSubscription(
    APP_ENDPOINTS.platform.subscriptionUpgrade,
    platformUserTier,
    "Subscription upgrade"
  );
}

export async function downgradeSubscription(platformUserTier) {
  return changeExistingSubscription(
    APP_ENDPOINTS.platform.subscriptionDowngrade,
    platformUserTier,
    "Subscription downgrade"
  );
}

export async function fetchSubscriptionTier(options = {}) {
  const { force = false } = options;
  if (!force) {
    const cachedResult = getCachedTierResult();
    if (cachedResult) {
      return cachedResult;
    }

    const cachedError = getCachedTierError();
    if (cachedError) {
      throw cachedError;
    }

    if (inFlightTierRequest) {
      return inFlightTierRequest;
    }
  }

  inFlightTierRequest = (async () => {
    try {
      const [dashboardPayload, tiersPayload] = await Promise.all([
        fetchDashboard({ force }),
        fetchPlatformUserTiers({ force }),
      ]);
      const result = buildSubscriptionFromDashboardAndTiers(dashboardPayload, tiersPayload);
      cacheTierResult(result);
      return result;
    } catch (error) {
      cacheTierError(error);
      throw error;
    }
  })();

  try {
    return await inFlightTierRequest;
  } finally {
    inFlightTierRequest = null;
  }
}
