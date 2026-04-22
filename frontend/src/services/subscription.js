import { getValidAccessToken, authenticatedFetch } from "../auth/session";
import { APP_ENDPOINTS } from "../config/endpoints";
import { isDevAuthBypassed } from "../auth/devAuth";
import { fetchDashboard, fetchPlatformUserTiers } from "./platform";

export const DEFAULT_TIER = "free";
export const PAID_TIER = "paid";

const DEV_SUBSCRIPTION_TIER_KEY = "local-dev-subscription-tier";
const DEV_SUBSCRIPTION_EVENT = "codex:dev-subscription-tier-changed";
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

function buildDevTiers() {
  return Object.entries(TIER_LIMITS_BY_KEY).map(([tierKey, tierLimits]) => ({
    id: tierKey,
    stripePriceId: tierKey === "paid" ? "dev-price-id" : "",
    name: tierKey.charAt(0).toUpperCase() + tierKey.slice(1),
    price: tierKey === "paid" ? 1 : 0,
    description: `${tierKey.charAt(0).toUpperCase() + tierKey.slice(1)} tier for local development mode.`,
    tierOrder: tierKey === "paid" ? 1 : 0,
    allowedNumberOfRegisteredClients: tierLimits.allowedNumberOfRegisteredClients,
    allowedNumberOfGlobalUsers: tierLimits.allowedNumberOfGlobalUsers,
    allowedNumberOfGlobalScopes: 0,
    allowedNumberOfGlobalAuthorities: 0,
  }));
}

let cachedTierResult = null;
let cachedTierResultAt = 0;
let cachedTierError = null;
let cachedTierErrorAt = 0;
let inFlightTierRequest = null;

function isLocalDevHost() {
  return typeof window !== "undefined" && (
    window.location.hostname === "localhost" ||
    window.location.hostname === "127.0.0.1"
  );
}

function normalizeTier(value) {
  if (typeof value !== "string") {
    return DEFAULT_TIER;
  }

  const normalizedValue = value.trim().toLowerCase();
  return normalizedValue || DEFAULT_TIER;
}

function toSafeInteger(value, fallback = 0) {
  const parsed = Number.parseInt(String(value ?? ""), 10);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function getTierLimitsByKey(tierKey) {
  return TIER_LIMITS_BY_KEY[tierKey] || TIER_LIMITS_BY_KEY[DEFAULT_TIER];
}

function buildSubscriptionSnapshot({
  tierKey,
  tierName,
  source,
  tiers,
  isDemoUser,
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
    tier: normalizeTier(tierKey),
    tierName: typeof tierName === "string" && tierName.trim().length > 0
      ? tierName.trim()
      : normalizeTier(tierKey),
    source,
    tiers: Array.isArray(tiers) ? tiers : [],
    isDemoUser: Boolean(isDemoUser),
    allowedNumberOfRegisteredClients: toSafeInteger(allowedNumberOfRegisteredClients, 0),
    allowedNumberOfGlobalUsers: toSafeInteger(allowedNumberOfGlobalUsers, 0),
    allowedNumberOfGlobalScopes: toSafeInteger(allowedNumberOfGlobalScopes, 0),
    allowedNumberOfGlobalAuthorities: toSafeInteger(allowedNumberOfGlobalAuthorities, 0),
    totalRegisteredClients: toSafeInteger(totalRegisteredClients, 0),
    totalUsers: toSafeInteger(totalUsers, 0),
    totalScopes: toSafeInteger(totalScopes, 0),
    totalAuthorities: toSafeInteger(totalAuthorities, 0),
    totalRoles: toSafeInteger(totalRoles, 0),
  };
}

function buildSubscriptionFromDashboardAndTiers(dashboardPayload, tiersPayload) {
  const dashboardTier = dashboardPayload?.tier;
  const tierKey = normalizeTier(dashboardTier?.name);
  const fallbackLimits = getTierLimitsByKey(tierKey);
  const matchingTier = Array.isArray(tiersPayload)
    ? tiersPayload.find((tier) => normalizeTier(tier?.name) === tierKey) ?? null
    : null;

  return buildSubscriptionSnapshot({
    tierKey,
    tierName:
      typeof matchingTier?.name === "string" && matchingTier.name.trim().length > 0
        ? matchingTier.name
        : typeof dashboardTier?.name === "string"
          ? dashboardTier.name
        : tierKey,
    source: matchingTier ? "tiers" : "dashboard",
    tiers: tiersPayload,
    isDemoUser: Boolean(dashboardPayload?.isDemoUser),
    allowedNumberOfRegisteredClients: matchingTier?.allowedNumberOfRegisteredClients ??
      fallbackLimits.allowedNumberOfRegisteredClients,
    allowedNumberOfGlobalUsers: matchingTier?.allowedNumberOfGlobalUsers ??
      fallbackLimits.allowedNumberOfGlobalUsers,
    allowedNumberOfGlobalScopes: matchingTier?.allowedNumberOfGlobalScopes ?? 0,
    allowedNumberOfGlobalAuthorities: matchingTier?.allowedNumberOfGlobalAuthorities ?? 0,
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

export function isDevSubscriptionOverrideAvailable() {
  return Boolean(import.meta.env.DEV && isLocalDevHost());
}

export function getDevSubscriptionOverrideTier() {
  if (!isDevSubscriptionOverrideAvailable() || typeof window === "undefined") {
    return "";
  }

  const rawOverride = window.localStorage.getItem(DEV_SUBSCRIPTION_TIER_KEY);
  if (typeof rawOverride !== "string" || rawOverride.trim().length === 0) {
    return "";
  }

  return normalizeTier(rawOverride);
}

export function setDevSubscriptionOverrideTier(tier) {
  if (!isDevSubscriptionOverrideAvailable() || typeof window === "undefined") {
    return;
  }

  const normalizedTier = normalizeTier(tier);
  window.localStorage.setItem(DEV_SUBSCRIPTION_TIER_KEY, normalizedTier);
  window.dispatchEvent(new CustomEvent(DEV_SUBSCRIPTION_EVENT, { detail: { tier: normalizedTier } }));
}

export function clearDevSubscriptionOverrideTier() {
  if (!isDevSubscriptionOverrideAvailable() || typeof window === "undefined") {
    return;
  }

  window.localStorage.removeItem(DEV_SUBSCRIPTION_TIER_KEY);
  window.dispatchEvent(new CustomEvent(DEV_SUBSCRIPTION_EVENT, { detail: { tier: "" } }));
}

export function subscribeToSubscriptionTierChanges(callback) {
  if (typeof window === "undefined") {
    return () => {};
  }

  const handleChange = () => {
    callback();
  };

  const handleStorage = (event) => {
    if (event.key === DEV_SUBSCRIPTION_TIER_KEY) {
      callback();
    }
  };

  window.addEventListener(DEV_SUBSCRIPTION_EVENT, handleChange);
  window.addEventListener("storage", handleStorage);

  return () => {
    window.removeEventListener(DEV_SUBSCRIPTION_EVENT, handleChange);
    window.removeEventListener("storage", handleStorage);
  };
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
  if (isDevAuthBypassed()) {
    throw new Error("Subscription checkout is unavailable in local dev mode.");
  }

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
  if (isDevAuthBypassed()) {
    throw new Error("Subscription changes are unavailable in local dev mode.");
  }

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
  const devOverrideTier = getDevSubscriptionOverrideTier();
  if (devOverrideTier) {
    const tierLimits = getTierLimitsByKey(devOverrideTier);
    const overrideResult = buildSubscriptionSnapshot({
      tierKey: devOverrideTier,
      tierName: devOverrideTier,
      source: "dev-override",
      tiers: buildDevTiers(),
      isDemoUser: false,
      allowedNumberOfRegisteredClients: tierLimits.allowedNumberOfRegisteredClients,
      allowedNumberOfGlobalUsers: tierLimits.allowedNumberOfGlobalUsers,
      allowedNumberOfGlobalScopes: 0,
      allowedNumberOfGlobalAuthorities: 0,
      totalRegisteredClients: 0,
      totalUsers: 0,
      totalScopes: 0,
      totalAuthorities: 0,
      totalRoles: 0,
    });
    cacheTierResult(overrideResult);
    return overrideResult;
  }

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
    if (isDevAuthBypassed()) {
      const tierLimits = getTierLimitsByKey(DEFAULT_TIER);
      const bypassResult = buildSubscriptionSnapshot({
        tierKey: DEFAULT_TIER,
        tierName: DEFAULT_TIER,
        source: "dev-bypass",
        tiers: buildDevTiers(),
        isDemoUser: false,
        allowedNumberOfRegisteredClients: tierLimits.allowedNumberOfRegisteredClients,
        allowedNumberOfGlobalUsers: tierLimits.allowedNumberOfGlobalUsers,
        allowedNumberOfGlobalScopes: 0,
        allowedNumberOfGlobalAuthorities: 0,
        totalRegisteredClients: 0,
        totalUsers: 0,
        totalScopes: 0,
        totalAuthorities: 0,
        totalRoles: 0,
      });
      cacheTierResult(bypassResult);
      return bypassResult;
    }

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
