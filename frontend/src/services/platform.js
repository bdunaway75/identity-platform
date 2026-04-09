import { isDevAuthBypassed } from "../auth/devAuth";
import { authenticatedFetch, getValidAccessToken } from "../auth/session";
import { APP_ENDPOINTS } from "../config/endpoints";

const REGISTERED_CLIENT_ENDPOINTS = APP_ENDPOINTS.platform.registeredClients;

const PLATFORM_API_CACHE_MS = 120_000;
const PLATFORM_API_FAILURE_CACHE_MS = 10_000;
const SESSION_PLATFORM_CACHE_KEY = "platform-api-cache";
const DEV_SUBSCRIPTION_TIER_KEY = "local-dev-subscription-tier";
const SENSITIVE_CACHE_PREFIXES = [
  "registered-client-users",
  "registered-client-tokens",
  "recent-user-activity",
];
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

const platformApiCache = new Map();

async function getAccessToken(errorMessage) {
  return getValidAccessToken(errorMessage);
}

function getLocalDevTierKey() {
  if (typeof window === "undefined") {
    return "paid";
  }

  const rawTier = String(window.localStorage.getItem(DEV_SUBSCRIPTION_TIER_KEY) ?? "").trim().toLowerCase();
  return TIER_LIMITS_BY_KEY[rawTier] ? rawTier : "paid";
}

function buildLocalDevDashboardPayload() {
  const tierKey = getLocalDevTierKey();
  const tierLimits = TIER_LIMITS_BY_KEY[tierKey] || TIER_LIMITS_BY_KEY.free;
  const tierName = tierKey.charAt(0).toUpperCase() + tierKey.slice(1);

  return normalizeDashboardPayload({
    tier: {
      name: tierName,
      allowedNumberOfRegisteredClients: tierLimits.allowedNumberOfRegisteredClients,
      allowedNumberOfGlobalUsers: tierLimits.allowedNumberOfGlobalUsers,
    },
    clientIds: [],
    registeredClientResponses: [],
    totalUsers: 0,
    totalRegisteredClients: 0,
    totalScopes: 0,
    totalAuthorities: 0,
    totalRoles: 0,
  });
}

function buildLocalDevTiersPayload() {
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

function canUseSessionStorage() {
  return typeof window !== "undefined" && typeof window.sessionStorage !== "undefined";
}

function loadSessionJson(key) {
  if (!canUseSessionStorage()) {
    return null;
  }

  try {
    const value = window.sessionStorage.getItem(key);
    return value ? JSON.parse(value) : null;
  } catch {
    return null;
  }
}

function saveSessionJson(key, value) {
  if (!canUseSessionStorage()) {
    return;
  }

  try {
    window.sessionStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Ignore storage write failures and fall back to in-memory cache.
  }
}

function removeSessionJson(key) {
  if (!canUseSessionStorage()) {
    return;
  }

  try {
    window.sessionStorage.removeItem(key);
  } catch {
    // Ignore storage cleanup failures.
  }
}

function getPlatformCacheKey(name, payload = null) {
  if (payload == null) {
    return name;
  }

  return `${name}:${JSON.stringify(payload)}`;
}

function getCachedPlatformValue(cacheKey) {
  const cachedEntry = platformApiCache.get(cacheKey);
  if (cachedEntry) {
    if (cachedEntry.kind === "success") {
      if (Date.now() - cachedEntry.at <= PLATFORM_API_CACHE_MS) {
        return cachedEntry;
      }
    } else if (cachedEntry.kind === "error") {
      if (Date.now() - cachedEntry.at <= PLATFORM_API_FAILURE_CACHE_MS) {
        return cachedEntry;
      }
    } else if (cachedEntry.kind === "pending") {
      return cachedEntry;
    }

    platformApiCache.delete(cacheKey);
  }

  const storedEntries = loadSessionJson(SESSION_PLATFORM_CACHE_KEY);
  const storedEntry = storedEntries?.[cacheKey];
  if (
    storedEntry &&
    storedEntry.kind === "success" &&
    Date.now() - Number(storedEntry.at || 0) <= PLATFORM_API_CACHE_MS
  ) {
    const restoredEntry = {
      kind: "success",
      value: storedEntry.value,
      at: Number(storedEntry.at || 0),
    };
    platformApiCache.set(cacheKey, restoredEntry);
    return restoredEntry;
  }

  return null;
}

function cachePlatformSuccess(cacheKey, value) {
  const timestamp = Date.now();

  platformApiCache.set(cacheKey, {
    kind: "success",
    value,
    at: timestamp,
  });

  const storedEntries = loadSessionJson(SESSION_PLATFORM_CACHE_KEY) || {};
  storedEntries[cacheKey] = {
    kind: "success",
    value,
    at: timestamp,
  };
  saveSessionJson(SESSION_PLATFORM_CACHE_KEY, storedEntries);
}

function cachePlatformError(cacheKey, error) {
  platformApiCache.set(cacheKey, {
    kind: "error",
    error,
    at: Date.now(),
  });
}

function setPlatformPending(cacheKey, promise) {
  platformApiCache.set(cacheKey, {
    kind: "pending",
    promise,
    at: Date.now(),
  });
}

function clearPlatformCacheEntries(prefixes = []) {
  if (prefixes.length === 0) {
    platformApiCache.clear();
    removeSessionJson(SESSION_PLATFORM_CACHE_KEY);
    return;
  }

  const storedEntries = loadSessionJson(SESSION_PLATFORM_CACHE_KEY) || {};
  let didRemoveStoredEntry = false;

  for (const cacheKey of platformApiCache.keys()) {
    if (prefixes.some((prefix) => cacheKey === prefix || cacheKey.startsWith(`${prefix}:`))) {
      platformApiCache.delete(cacheKey);
    }
  }

  for (const cacheKey of Object.keys(storedEntries)) {
    if (prefixes.some((prefix) => cacheKey === prefix || cacheKey.startsWith(`${prefix}:`))) {
      delete storedEntries[cacheKey];
      didRemoveStoredEntry = true;
    }
  }

  if (didRemoveStoredEntry) {
    saveSessionJson(SESSION_PLATFORM_CACHE_KEY, storedEntries);
  }
}

if (typeof window !== "undefined") {
  // Remove any previously cached PII-bearing responses from earlier app versions.
  clearPlatformCacheEntries(SENSITIVE_CACHE_PREFIXES);
}

async function fetchWithPlatformCache({
  cacheKey,
  force = false,
  request,
}) {
  if (!force) {
    const cachedEntry = getCachedPlatformValue(cacheKey);
    if (cachedEntry?.kind === "success") {
      return cachedEntry.value;
    }

    if (cachedEntry?.kind === "error") {
      throw cachedEntry.error;
    }

    if (cachedEntry?.kind === "pending") {
      return cachedEntry.promise;
    }
  }

  const promise = (async () => {
    try {
      const value = await request();
      cachePlatformSuccess(cacheKey, value);
      return value;
    } catch (error) {
      cachePlatformError(cacheKey, error);
      throw error;
    }
  })();

  setPlatformPending(cacheKey, promise);

  return promise;
}

function normalizeArray(value) {
  return Array.isArray(value) ? value : [];
}

function toSafeInteger(value, fallback = 0) {
  const parsed = Number.parseInt(String(value ?? ""), 10);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function normalizeStringArray(value) {
  return normalizeArray(value)
    .map((item) => String(item ?? "").trim())
    .filter(Boolean);
}

export function normalizeRegisteredClientPayload(value) {
  if (!value || typeof value !== "object") {
    return null;
  }

  return {
    ...value,
    id: value.id ?? null,
    clientId: String(value.clientId ?? "").trim(),
    clientName: String(value.clientName ?? "").trim(),
    clientAuthenticationMethods: normalizeStringArray(value.clientAuthenticationMethods),
    authorizationGrantTypes: normalizeStringArray(value.authorizationGrantTypes),
    redirectUris: normalizeStringArray(value.redirectUris),
    postLogoutRedirectUris: normalizeStringArray(value.postLogoutRedirectUris),
    scopes: normalizeStringArray(value.scopes),
    authorities: normalizeStringArray(value.authorities),
    roles: normalizeStringArray(value.roles),
    clientSettings:
      value.clientSettings && typeof value.clientSettings === "object" ? value.clientSettings : {},
    tokenSettings:
      value.tokenSettings && typeof value.tokenSettings === "object" ? value.tokenSettings : {},
  };
}

function normalizeClientUserPayload(value) {
  if (!value || typeof value !== "object") {
    return null;
  }

  return {
    ...value,
    id: value.id ?? null,
    clientId: String(value.clientId ?? "").trim(),
    email: String(value.email ?? "").trim(),
    authorities: normalizeStringArray(value.authorities),
    roles: normalizeStringArray(value.roles),
  };
}

function normalizeAuthTokenPayload(value) {
  if (!value || typeof value !== "object") {
    return null;
  }

  return {
    ...value,
    id: value.id ?? null,
    kid: String(value.kid ?? "").trim(),
    subject: String(value.subject ?? "").trim(),
  };
}

function normalizeActivityPayload(value) {
  if (!value || typeof value !== "object") {
    return null;
  }

  return {
    ...value,
    email: String(value.email ?? "").trim(),
  };
}

function normalizeTier(tierValue) {
  if (tierValue && typeof tierValue === "object") {
    const name = typeof tierValue.name === "string" && tierValue.name.trim().length > 0
      ? tierValue.name.trim()
      : "Free";
    const normalizedKey = name.toLowerCase();
    const fallbackLimits = TIER_LIMITS_BY_KEY[normalizedKey] || TIER_LIMITS_BY_KEY.free;

    return {
      key: normalizedKey,
      name,
      allowedNumberOfRegisteredClients: toSafeInteger(
        tierValue.allowedNumberOfRegisteredClients,
        fallbackLimits.allowedNumberOfRegisteredClients
      ),
      allowedNumberOfGlobalUsers: toSafeInteger(
        tierValue.allowedNumberOfGlobalUsers,
        fallbackLimits.allowedNumberOfGlobalUsers
      ),
    };
  }

  if (typeof tierValue === "string" && tierValue.trim().length > 0) {
    const normalized = tierValue.trim().toLowerCase();
    const fallbackLimits = TIER_LIMITS_BY_KEY[normalized] || TIER_LIMITS_BY_KEY.free;
    return {
      key: normalized,
      name: normalized.charAt(0).toUpperCase() + normalized.slice(1),
      allowedNumberOfRegisteredClients: fallbackLimits.allowedNumberOfRegisteredClients,
      allowedNumberOfGlobalUsers: fallbackLimits.allowedNumberOfGlobalUsers,
    };
  }

  return {
    key: "free",
    name: "Free",
    allowedNumberOfRegisteredClients: 0,
    allowedNumberOfGlobalUsers: 0,
  };
}

function normalizeDashboardPayload(payload) {
  if (!payload || typeof payload !== "object") {
    return {
      tier: normalizeTier(null),
      clientIds: [],
      registeredClientResponses: [],
      totalUsers: 0,
      totalRegisteredClients: 0,
      totalScopes: 0,
      totalAuthorities: 0,
      totalRoles: 0,
    };
  }

  const registeredClientResponses = normalizeArray(payload.registeredClientResponses)
    .map(normalizeRegisteredClientPayload)
    .filter(Boolean)
    .sort((left, right) =>
      (left.clientName || left.clientId || "").localeCompare(right.clientName || right.clientId || "")
    );
  const clientIds = normalizeStringArray(payload.clientIds);

  return {
    tier: normalizeTier(payload.tier),
    clientIds,
    registeredClientResponses,
    totalUsers: toSafeInteger(payload.totalUsers, 0),
    totalRegisteredClients: toSafeInteger(payload.totalRegisteredClients, registeredClientResponses.length),
    totalScopes: toSafeInteger(payload.totalScopes, 0),
    totalAuthorities: toSafeInteger(payload.totalAuthorities, 0),
    totalRoles: toSafeInteger(payload.totalRoles, 0),
  };
}

function normalizeTierResponses(payload) {
  return normalizeArray(payload)
    .filter((item) => item && typeof item === "object")
    .map((item) => ({
      id: item.id ?? null,
      stripePriceId: String(item.stripePriceId ?? "").trim(),
      name: String(item.name ?? "").trim(),
      price: toSafeInteger(item.price, 0),
      description: String(item.description ?? "").trim(),
      tierOrder: toSafeInteger(item.tierOrder, 0),
      allowedNumberOfRegisteredClients: toSafeInteger(item.allowedNumberOfRegisteredClients, 0),
      allowedNumberOfGlobalUsers: toSafeInteger(item.allowedNumberOfGlobalUsers, 0),
      allowedNumberOfGlobalScopes: toSafeInteger(item.allowedNumberOfGlobalScopes, 0),
      allowedNumberOfGlobalAuthorities: toSafeInteger(item.allowedNumberOfGlobalAuthorities, 0),
    }))
    .filter((item) => item.name);
}

export function clearPlatformApiCache() {
  clearPlatformCacheEntries();
}

export async function fetchDashboard(options = {}) {
  const { force = false } = options;

  if (isDevAuthBypassed()) {
    return buildLocalDevDashboardPayload();
  }

  return fetchWithPlatformCache({
    force,
    cacheKey: getPlatformCacheKey("dashboard"),
    request: async () => {
      const accessToken = await getAccessToken("Missing access token for dashboard lookup.");

      const response = await authenticatedFetch(REGISTERED_CLIENT_ENDPOINTS.dashboard, {
        method: "GET",
        headers: {
          Accept: "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Dashboard lookup failed with status ${response.status}.`);
      }

      const payload = await response.json();
      return normalizeDashboardPayload(payload);
    },
  });
}

export async function fetchRegisteredClients() {
  const dashboard = await fetchDashboard();
  return dashboard.registeredClientResponses;
}

export async function fetchPlatformUserTiers(options = {}) {
  const { force = false } = options;

  if (isDevAuthBypassed()) {
    return buildLocalDevTiersPayload();
  }

  return fetchWithPlatformCache({
    force,
    cacheKey: getPlatformCacheKey("tiers"),
    request: async () => {
      const accessToken = await getAccessToken("Missing access token for tier lookup.");

      const response = await authenticatedFetch(REGISTERED_CLIENT_ENDPOINTS.tiers, {
        method: "GET",
        headers: {
          Accept: "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Tier lookup failed with status ${response.status}.`);
      }

      return normalizeTierResponses(await response.json());
    },
  });
}

export async function fetchRegisteredClient(registeredClientId) {
  if (!registeredClientId) {
    throw new Error("Registered client ID is required.");
  }

  if (isDevAuthBypassed()) {
    throw new Error("Registered client lookup is unavailable in local dev mode.");
  }

  return fetchWithPlatformCache({
    cacheKey: getPlatformCacheKey("registered-client", registeredClientId),
    request: async () => {
      const accessToken = await getAccessToken("Missing access token for registered client lookup.");

      const response = await authenticatedFetch(`${REGISTERED_CLIENT_ENDPOINTS.base}/${registeredClientId}`, {
        method: "GET",
        headers: {
          Accept: "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Registered client lookup failed with status ${response.status}.`);
      }

      return normalizeRegisteredClientPayload(await response.json());
    },
  });
}

export async function fetchRegisteredClientUsers(registeredClientIds) {
  if (isDevAuthBypassed()) {
    return [];
  }

  const normalizedIds = Array.isArray(registeredClientIds) ? [...registeredClientIds].sort() : [];
  const accessToken = await getAccessToken("Missing access token for registered client users lookup.");

  const response = await authenticatedFetch(REGISTERED_CLIENT_ENDPOINTS.users, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(normalizedIds),
  });

  if (!response.ok) {
    throw new Error(`Registered client users lookup failed with status ${response.status}.`);
  }

  return normalizeArray(await response.json())
    .map(normalizeClientUserPayload)
    .filter(Boolean);
}

export async function fetchRegisteredClientTokens(registeredClientIds) {
  if (isDevAuthBypassed()) {
    return [];
  }

  const normalizedIds = Array.isArray(registeredClientIds) ? [...registeredClientIds].sort() : [];
  const accessToken = await getAccessToken("Missing access token for registered client tokens lookup.");

  const response = await authenticatedFetch(REGISTERED_CLIENT_ENDPOINTS.tokens, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(normalizedIds),
  });

  if (!response.ok) {
    throw new Error(`Registered client tokens lookup failed with status ${response.status}.`);
  }

  return normalizeArray(await response.json())
    .map(normalizeAuthTokenPayload)
    .filter(Boolean);
}

export async function fetchRecentUserActivity(clientIds) {
  if (isDevAuthBypassed()) {
    return {
      logins: [],
      signups: [],
    };
  }

  const normalizedClientIds = Array.isArray(clientIds)
    ? [...new Set(clientIds.map((id) => String(id ?? "").trim()).filter(Boolean))].sort()
    : [];
  const accessToken = await getAccessToken("Missing access token for recent user activity lookup.");

  const response = await authenticatedFetch(REGISTERED_CLIENT_ENDPOINTS.recentActivity, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(normalizedClientIds),
  });

  if (!response.ok) {
    throw new Error(`Recent user activity lookup failed with status ${response.status}.`);
  }

  const payload = await response.json();
  return {
    logins: normalizeArray(payload?.logins).map(normalizeActivityPayload).filter(Boolean),
    signups: normalizeArray(payload?.signups).map(normalizeActivityPayload).filter(Boolean),
  };
}

export async function updateRegisteredClientUser(clientUserId, updates) {
  if (!clientUserId) {
    throw new Error("Client user ID is required.");
  }

  if (isDevAuthBypassed()) {
    throw new Error("Client user updates are unavailable in local dev mode.");
  }

  const accessToken = await getAccessToken("Missing access token for client user update.");

  const response = await authenticatedFetch(`${REGISTERED_CLIENT_ENDPOINTS.base}/users/${clientUserId}`, {
    method: "PATCH",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(updates ?? {}),
  });

  if (!response.ok) {
    throw new Error(`Client user update failed with status ${response.status}.`);
  }

  clearPlatformCacheEntries(["dashboard", "registered-client-users", "recent-user-activity"]);
  return normalizeClientUserPayload(await response.json());
}

export async function invalidateRegisteredClientToken(authTokenId) {
  if (!authTokenId) {
    throw new Error("Auth token ID is required.");
  }

  if (isDevAuthBypassed()) {
    throw new Error("Token invalidation is unavailable in local dev mode.");
  }

  const accessToken = await getAccessToken("Missing access token for auth token invalidation.");

  const response = await authenticatedFetch(`${REGISTERED_CLIENT_ENDPOINTS.tokens}/${authTokenId}/invalidate`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  if (!response.ok) {
    throw new Error(`Auth token invalidation failed with status ${response.status}.`);
  }

  clearPlatformCacheEntries(["registered-client-tokens", "recent-user-activity"]);
}

export async function invalidateAllRegisteredClientTokens(registeredClientId) {
  if (!registeredClientId) {
    throw new Error("Registered client ID is required.");
  }

  if (isDevAuthBypassed()) {
    throw new Error("Bulk token invalidation is unavailable in local dev mode.");
  }

  const accessToken = await getAccessToken("Missing access token for registered client token invalidation.");

  const response = await authenticatedFetch(`${REGISTERED_CLIENT_ENDPOINTS.base}/${registeredClientId}/tokens/invalidate`, {
    method: "POST",
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
  });

  if (!response.ok) {
    throw new Error(`Registered client token invalidation failed with status ${response.status}.`);
  }

  const rawPayload = await response.text();
  const parsedCount = Number.parseInt(rawPayload, 10);

  if (Number.isFinite(parsedCount)) {
    clearPlatformCacheEntries(["registered-client-tokens", "recent-user-activity"]);
    return parsedCount;
  }

  throw new Error("Registered client token invalidation returned an invalid response.");
}
