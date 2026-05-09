import { authenticatedFetch, getValidAccessToken } from "../../auth/services/session";
import { APP_ENDPOINTS } from "../../../shared/config/endpoints";

const REGISTERED_CLIENT_ENDPOINTS = APP_ENDPOINTS.platform.registeredClients;

const PLATFORM_API_CACHE_MS = 120_000;
const PLATFORM_API_FAILURE_CACHE_MS = 10_000;
const SESSION_PLATFORM_CACHE_KEY = "platform-api-cache";

const platformApiCache = new Map();

async function getAccessToken(errorMessage) {
  return getValidAccessToken(errorMessage);
}

function loadSessionJson(key) {
    const value = window.sessionStorage.getItem(key);
    return value ? JSON.parse(value) : null;
}

function saveSessionJson(key, value) {
    window.sessionStorage.setItem(key, JSON.stringify(value));
}

function removeSessionJson(key) {
    window.sessionStorage.removeItem(key);
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
  if (storedEntry?.kind === "success" && Date.now() - Number(storedEntry.at || 0) <= PLATFORM_API_CACHE_MS) {
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

export function clearPlatformApiCache() {
  clearPlatformCacheEntries();
}

export async function fetchDashboard(options = {}) {
  const { force = false } = options;

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

      return await response.json();
    },
  });
}

export async function fetchRegisteredClients() {
  const dashboard = await fetchDashboard();
  return dashboard.registeredClientResponses;
}

export async function fetchPlatformUserTiers(options = {}) {
  const { force = false } = options;

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

      return await response.json();
    },
  });
}

export async function fetchAdminDashboard(options = {}) {
  const { force = false } = options;

  return fetchWithPlatformCache({
    force,
    cacheKey: getPlatformCacheKey("admin-dashboard"),
    request: async () => {
      const accessToken = await getAccessToken("Missing access token for admin dashboard lookup.");

      const response = await authenticatedFetch(REGISTERED_CLIENT_ENDPOINTS.adminDashboard, {
        method: "POST",
        headers: {
          Accept: "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Admin dashboard lookup failed with status ${response.status}.`);
      }

      return await response.json();
    },
  });
}

export async function fetchRegisteredClient(registeredClientId) {
  if (!registeredClientId) {
    throw new Error("Registered client ID is required.");
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

      return await response.json();
    },
  });
}

export async function fetchRegisteredClientUsers(registeredClientIds) {
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

  return normalizeArray(await response.json());
}

export async function fetchRegisteredClientTokens(registeredClientIds) {
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

  return normalizeArray(await response.json());
}

export async function fetchRecentUserActivity(clientIds) {
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
    logins: normalizeArray(payload?.logins),
    signups: normalizeArray(payload?.signups),
  };
}

export async function updateRegisteredClientUser(clientUserId, updates) {
  if (!clientUserId) {
    throw new Error("Client user ID is required.");
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
  return await response.json();
}

export async function invalidateRegisteredClientToken(authTokenId) {
  if (!authTokenId) {
    throw new Error("Auth token ID is required.");
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
