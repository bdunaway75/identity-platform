import { authenticatedFetch, getValidAccessToken } from "../auth/session";
import { APP_ENDPOINTS } from "../config/endpoints";
import { clearSubscriptionTierCache } from "./subscription";
import { clearPlatformApiCache, normalizeRegisteredClientPayload } from "./platform";

const REGISTERED_CLIENT_ENDPOINTS = APP_ENDPOINTS.platform.registeredClients;
const SPRING_CLIENT_SETTINGS_KEYS = {
  requireProofKey: "settings.client.require-proof-key",
  requireAuthorizationConsent: "settings.client.require-authorization-consent",
};

function formatClientValidationMessage(message, fallback) {
  const normalizedMessage = String(message ?? "").trim();
  if (!normalizedMessage) {
    return fallback;
  }

  if (normalizedMessage.startsWith("Platform user tier validation failed:")) {
    const tierReason = normalizedMessage.replace("Platform user tier validation failed:", "").trim();
    return tierReason ? `Tier limit reached: ${tierReason}.` : fallback;
  }

  if (normalizedMessage.startsWith("Client validation failed with errors:")) {
    return normalizedMessage.replace("Client validation failed with errors:", "Client validation failed:").trim();
  }

  return normalizedMessage;
}

async function readClientErrorMessage(response, fallback) {
  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    try {
      const body = await response.json();
      return formatClientValidationMessage(body?.message ?? body?.error ?? body?.errors, fallback);
    } catch {
      return fallback;
    }
  }

  return formatClientValidationMessage(await response.text(), fallback);
}

function toDurationString(value) {
  if (value == null) {
    return undefined;
  }

  const normalized = String(value).trim();
  if (!normalized) {
    return undefined;
  }

  if (/^P(T.*)?$/i.test(normalized)) {
    return normalized.toUpperCase();
  }

  return `PT${normalized}M`;
}

function serializeClientSettings(clientSettings) {
  return {
    [SPRING_CLIENT_SETTINGS_KEYS.requireProofKey]: Boolean(clientSettings?.requireProofKey),
    [SPRING_CLIENT_SETTINGS_KEYS.requireAuthorizationConsent]: Boolean(clientSettings?.requireAuthorizationConsent),
  };
}

function serializeTokenSettings(tokenSettings) {
  return {
    accessTokenTimeToLive: toDurationString(tokenSettings?.accessTokenTimeToLive ?? tokenSettings?.accessTokenTimeToLiveMinutes),
    refreshTokenTimeToLive: toDurationString(tokenSettings?.refreshTokenTimeToLive ?? tokenSettings?.refreshTokenTimeToLiveMinutes),
    authorizationCodeTimeToLive: toDurationString(
      tokenSettings?.authorizationCodeTimeToLive ?? tokenSettings?.authorizationCodeTimeToLiveMinutes
    ),
    reuseRefreshTokens: Boolean(tokenSettings?.reuseRefreshTokens),
  };
}

export function serializeRegisteredClientPayload(payload) {
  return {
    ...payload,
    clientAuthenticationMethods: [...(payload.clientAuthenticationMethods ?? [])],
    authorizationGrantTypes: [...(payload.authorizationGrantTypes ?? [])],
    redirectUris: [...(payload.redirectUris ?? [])],
    postLogoutRedirectUris: [...(payload.postLogoutRedirectUris ?? [])],
    scopes: [...(payload.scopes ?? [])],
    clientSettings: serializeClientSettings(payload.clientSettings),
    tokenSettings: serializeTokenSettings(payload.tokenSettings),
  };
}

export async function createClient(payload) {
  const accessToken = await getValidAccessToken("Missing access token for client creation.");

  const requestPayload = serializeRegisteredClientPayload(payload);

  const response = await authenticatedFetch(REGISTERED_CLIENT_ENDPOINTS.create, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(requestPayload),
  });

  if (!response.ok) {
    const message = await readClientErrorMessage(response, `Client creation failed with status ${response.status}.`);
    throw new Error(message);
  }

  clearPlatformApiCache();
  clearSubscriptionTierCache();
  return normalizeRegisteredClientPayload(await response.json());
}

export async function updateClient(registeredClientId, payload) {
  if (!registeredClientId) {
    throw new Error("Registered client ID is required for updates.");
  }

  const accessToken = await getValidAccessToken("Missing access token for client update.");

  const requestPayload = serializeRegisteredClientPayload(payload);

  const response = await authenticatedFetch(`${REGISTERED_CLIENT_ENDPOINTS.base}/${registeredClientId}/update`, {
    method: "PATCH",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(requestPayload),
  });

  if (!response.ok) {
    const message = await readClientErrorMessage(response, `Client update failed with status ${response.status}.`);
    throw new Error(message);
  }

  clearPlatformApiCache();
  clearSubscriptionTierCache();
  return normalizeRegisteredClientPayload(await response.json());
}
