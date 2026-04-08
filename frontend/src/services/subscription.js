import { userManager } from "../auth/oidc";
import { isDevAuthBypassed } from "../auth/devAuth";

export const DEFAULT_TIER = "free";
export const PAID_TIER = "paid";
export const SUBSCRIPTION_TIER_ENDPOINT =
  "http://localhost:8080/platform/register-client/tier-status";
export const TOTAL_USER_COUNT_ENDPOINT =
  "http://localhost:8080/platform/register-client/total-user-count";
export const TOTAL_CLIENT_COUNT_ENDPOINT =
  "http://localhost:8080/platform/register-client/total-client-count";
export const REGISTERED_CLIENTS_ENDPOINT =
  "http://localhost:8080/platform/register-client";
export const REGISTERED_CLIENT_USERS_ENDPOINT =
  "http://localhost:8080/platform/register-client/users";
export const REGISTERED_CLIENT_TOKENS_ENDPOINT =
  "http://localhost:8080/platform/register-client/tokens";
const DEV_SUBSCRIPTION_TIER_KEY = "local-dev-subscription-tier";
const DEV_SUBSCRIPTION_EVENT = "codex:dev-subscription-tier-changed";
const SUBSCRIPTION_SUCCESS_CACHE_MS = 30_000;
const SUBSCRIPTION_FAILURE_CACHE_MS = 10_000;

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

export function isDevSubscriptionOverrideAvailable() {
  return Boolean(import.meta.env.DEV && isLocalDevHost());
}

export function getDevSubscriptionOverrideTier() {
  if (!isDevSubscriptionOverrideAvailable() || typeof window === "undefined") {
    return "";
  }

  return normalizeTier(window.localStorage.getItem(DEV_SUBSCRIPTION_TIER_KEY) || "");
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

function normalizeTier(value) {
  if (typeof value !== "string") {
    return DEFAULT_TIER;
  }

  const normalizedValue = value.trim().toLowerCase();
  return normalizedValue || DEFAULT_TIER;
}

function extractTier(payload) {
  if (typeof payload === "string") {
    return normalizeTier(payload);
  }

  if (!payload || typeof payload !== "object") {
    return DEFAULT_TIER;
  }

  return normalizeTier(
    payload.tier ||
    payload.subscriptionTier ||
    payload.plan ||
    payload.level
  );
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

export function clearSubscriptionTierCache() {
  cachedTierResult = null;
  cachedTierResultAt = 0;
  cachedTierError = null;
  cachedTierErrorAt = 0;
  inFlightTierRequest = null;
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
  if (SUBSCRIPTION_TIER_ENDPOINT === "PASTE_YOUR_AUTH_SERVER_TIER_ENDPOINT_HERE") {
    const result = {
      tier: DEFAULT_TIER,
      source: "placeholder",
    };
    cacheTierResult(result);
    return result;
  }

  const user = await userManager.getUser();

  if (!user?.access_token) {
    const devOverrideTier = getDevSubscriptionOverrideTier();
    if (devOverrideTier) {
      const result = {
        tier: devOverrideTier,
        source: "dev-override",
      };
      cacheTierResult(result);
      return result;
    }

    if (isDevAuthBypassed()) {
      const result = {
        tier: DEFAULT_TIER,
        source: "dev-bypass",
      };
      cacheTierResult(result);
      return result;
    }

    const error = new Error("Missing access token for subscription lookup.");
    cacheTierError(error);
    throw error;
  }

  const response = await fetch(SUBSCRIPTION_TIER_ENDPOINT, {
    method: "GET",
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${user.access_token}`,
    },
  });

  if (!response.ok) {
    const error = new Error(`Subscription lookup failed with status ${response.status}.`);
    cacheTierError(error);
    throw error;
  }

  const rawPayload = await response.text();
  let payload;

  try {
    payload = JSON.parse(rawPayload);
  } catch {
    payload = rawPayload;
  }

  const result = {
    tier: extractTier(payload),
    source: "api",
  };
  cacheTierResult(result);
  return result;
  })();

  try {
    return await inFlightTierRequest;
  } finally {
    inFlightTierRequest = null;
  }
}

export async function fetchTotalUserCount() {
  const user = await userManager.getUser();

  if (!user?.access_token) {
    throw new Error("Missing access token for user count lookup.");
  }

  const response = await fetch(TOTAL_USER_COUNT_ENDPOINT, {
    method: "GET",
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${user.access_token}`,
    },
  });

  if (!response.ok) {
    throw new Error(`User count lookup failed with status ${response.status}.`);
  }

  const rawPayload = await response.text();
  const parsedCount = Number.parseInt(rawPayload, 10);

  if (Number.isFinite(parsedCount)) {
    return parsedCount;
  }

  throw new Error("User count lookup returned an invalid response.");
}

export async function fetchTotalClientCount() {
  const user = await userManager.getUser();

  if (!user?.access_token) {
    throw new Error("Missing access token for client count lookup.");
  }

  const response = await fetch(TOTAL_CLIENT_COUNT_ENDPOINT, {
    method: "GET",
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${user.access_token}`,
    },
  });

  if (!response.ok) {
    throw new Error(`Client count lookup failed with status ${response.status}.`);
  }

  const rawPayload = await response.text();
  const parsedCount = Number.parseInt(rawPayload, 10);

  if (Number.isFinite(parsedCount)) {
    return parsedCount;
  }

  throw new Error("Client count lookup returned an invalid response.");
}

export async function fetchRegisteredClients() {
  const user = await userManager.getUser();

  if (!user?.access_token) {
    throw new Error("Missing access token for registered clients lookup.");
  }

  const response = await fetch(REGISTERED_CLIENTS_ENDPOINT, {
    method: "GET",
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${user.access_token}`,
    },
  });

  if (!response.ok) {
    throw new Error(`Registered clients lookup failed with status ${response.status}.`);
  }

  return response.json();
}

export async function fetchRegisteredClient(registeredClientId) {
  if (!registeredClientId) {
    throw new Error("Registered client ID is required.");
  }

  const user = await userManager.getUser();

  if (!user?.access_token) {
    throw new Error("Missing access token for registered client lookup.");
  }

  const response = await fetch(`${REGISTERED_CLIENTS_ENDPOINT}/${registeredClientId}`, {
    method: "GET",
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${user.access_token}`,
    },
  });

  if (!response.ok) {
    throw new Error(`Registered client lookup failed with status ${response.status}.`);
  }

  return response.json();
}

export async function fetchRegisteredClientUsers(registeredClientIds) {
  const user = await userManager.getUser();

  if (!user?.access_token) {
    throw new Error("Missing access token for registered client users lookup.");
  }

  const response = await fetch(REGISTERED_CLIENT_USERS_ENDPOINT, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      Authorization: `Bearer ${user.access_token}`,
    },
    body: JSON.stringify({
      registeredClientIds: Array.isArray(registeredClientIds) ? registeredClientIds : [],
    }),
  });

  if (!response.ok) {
    throw new Error(`Registered client users lookup failed with status ${response.status}.`);
  }

  return response.json();
}

export async function updateRegisteredClientUser(clientUserId, updates) {
  if (!clientUserId) {
    throw new Error("Client user ID is required.");
  }

  const user = await userManager.getUser();

  if (!user?.access_token) {
    throw new Error("Missing access token for client user update.");
  }

  const response = await fetch(`${REGISTERED_CLIENT_USERS_ENDPOINT}/${clientUserId}`, {
    method: "PATCH",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      Authorization: `Bearer ${user.access_token}`,
    },
    body: JSON.stringify(updates ?? {}),
  });

  if (!response.ok) {
    throw new Error(`Client user update failed with status ${response.status}.`);
  }

  return response.json();
}

export async function fetchRegisteredClientTokens(registeredClientIds) {
  const user = await userManager.getUser();

  if (!user?.access_token) {
    throw new Error("Missing access token for registered client tokens lookup.");
  }

  const response = await fetch(REGISTERED_CLIENT_TOKENS_ENDPOINT, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      Authorization: `Bearer ${user.access_token}`,
    },
    body: JSON.stringify({
      registeredClientIds: Array.isArray(registeredClientIds) ? registeredClientIds : [],
    }),
  });

  if (!response.ok) {
    throw new Error(`Registered client tokens lookup failed with status ${response.status}.`);
  }

  return response.json();
}

export async function invalidateRegisteredClientToken(authTokenId) {
  if (!authTokenId) {
    throw new Error("Auth token ID is required.");
  }

  const user = await userManager.getUser();

  if (!user?.access_token) {
    throw new Error("Missing access token for auth token invalidation.");
  }

  const response = await fetch(`${REGISTERED_CLIENT_TOKENS_ENDPOINT}/${authTokenId}/invalidate`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${user.access_token}`,
    },
  });

  if (!response.ok) {
    throw new Error(`Auth token invalidation failed with status ${response.status}.`);
  }
}

export async function invalidateAllRegisteredClientTokens(registeredClientId) {
  if (!registeredClientId) {
    throw new Error("Registered client ID is required.");
  }

  const user = await userManager.getUser();

  if (!user?.access_token) {
    throw new Error("Missing access token for registered client token invalidation.");
  }

  const response = await fetch(`${REGISTERED_CLIENTS_ENDPOINT}/${registeredClientId}/tokens/invalidate`, {
    method: "POST",
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${user.access_token}`,
    },
  });

  if (!response.ok) {
    throw new Error(`Registered client token invalidation failed with status ${response.status}.`);
  }

  const rawPayload = await response.text();
  const parsedCount = Number.parseInt(rawPayload, 10);

  if (Number.isFinite(parsedCount)) {
    return parsedCount;
  }

  throw new Error("Registered client token invalidation returned an invalid response.");
}
