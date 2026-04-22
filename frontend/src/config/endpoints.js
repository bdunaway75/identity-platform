const APP_ENDPOINT_DEFAULTS = {
  authServerOrigin: "https://identificationplatform.com",
  frontendOrigin: "https://identificationplatform.com",
  oidcClientId: "identity-platform",
};

function readEnvString(value) {
  return String(value ?? "").trim();
}

function normalizeBaseUrl(value) {
  return readEnvString(value).replace(/\/+$/, "");
}

function normalizeUrl(value) {
  return readEnvString(value);
}

function firstEnvValue(...values) {
  return values.map(readEnvString).find(Boolean) || "";
}

function resolveEndpointUrl(path, override) {
  const normalizedOverride = readEnvString(override);
  return normalizedOverride ? normalizedOverride.replace(/\/+$/, "") : path;
}

const authServerOrigin = normalizeBaseUrl(
  import.meta.env.VITE_AUTH_SERVER_ORIGIN ?? APP_ENDPOINT_DEFAULTS.authServerOrigin,
);
const frontendOrigin = normalizeBaseUrl(
  import.meta.env.VITE_FRONTEND_ORIGIN ?? APP_ENDPOINT_DEFAULTS.frontendOrigin
);
const platformBaseEndpoint = `${authServerOrigin}/platform`;
const registeredClientBaseEndpoint = resolveEndpointUrl(
  `${platformBaseEndpoint}/api`,
  firstEnvValue(
    import.meta.env.VITE_PLATFORM_API_ENDPOINT,
    import.meta.env.VITE_PLATFORM_REGISTERED_CLIENTS_ENDPOINT
  )
);

export const APP_ENDPOINTS = {
  origins: {
    authServer: authServerOrigin,
    frontend: frontendOrigin,
  },
  oidc: {
    authority: normalizeBaseUrl(
      import.meta.env.VITE_OIDC_AUTHORITY ?? authServerOrigin
    ),
    clientId: readEnvString(
      import.meta.env.VITE_OIDC_CLIENT_ID ?? APP_ENDPOINT_DEFAULTS.oidcClientId
    ),
    redirectUri: normalizeUrl(
      import.meta.env.VITE_OIDC_REDIRECT_URI ?? `${frontendOrigin}/callback`
    ),
    postLogoutRedirectUri: normalizeUrl(
      import.meta.env.VITE_OIDC_POST_LOGOUT_REDIRECT_URI ?? `${frontendOrigin}/app/login`
    ),
  },
  platform: {
    demoAccessCode: resolveEndpointUrl(
      `${platformBaseEndpoint}/demo-access-code`,
      import.meta.env.VITE_PLATFORM_DEMO_ACCESS_ENDPOINT
    ),
    subscription: resolveEndpointUrl(
      `${platformBaseEndpoint}/subscription`,
      import.meta.env.VITE_PLATFORM_SUBSCRIPTION_ENDPOINT
    ),
    subscriptionUpgrade: resolveEndpointUrl(
      `${platformBaseEndpoint}/subscription/upgrade`,
      import.meta.env.VITE_PLATFORM_SUBSCRIPTION_UPGRADE_ENDPOINT
    ),
    subscriptionDowngrade: resolveEndpointUrl(
      `${platformBaseEndpoint}/subscription/downgrade`,
      import.meta.env.VITE_PLATFORM_SUBSCRIPTION_DOWNGRADE_ENDPOINT
    ),
    subscriptionStatus: resolveEndpointUrl(
      `${platformBaseEndpoint}/subscription-status`,
      import.meta.env.VITE_PLATFORM_SUBSCRIPTION_STATUS_ENDPOINT
    ),
    registeredClients: {
      base: registeredClientBaseEndpoint,
      create: resolveEndpointUrl(
        `${registeredClientBaseEndpoint}/create`,
        firstEnvValue(
          import.meta.env.VITE_PLATFORM_API_CREATE_ENDPOINT,
          import.meta.env.VITE_PLATFORM_REGISTER_CLIENT_CREATE_ENDPOINT
        )
      ),
      users: resolveEndpointUrl(
        `${registeredClientBaseEndpoint}/users`,
        firstEnvValue(
          import.meta.env.VITE_PLATFORM_API_USERS_ENDPOINT,
          import.meta.env.VITE_PLATFORM_REGISTERED_CLIENT_USERS_ENDPOINT
        )
      ),
      tokens: resolveEndpointUrl(
        `${registeredClientBaseEndpoint}/tokens`,
        firstEnvValue(
          import.meta.env.VITE_PLATFORM_API_TOKENS_ENDPOINT,
          import.meta.env.VITE_PLATFORM_REGISTERED_CLIENT_TOKENS_ENDPOINT
        )
      ),
      dashboard: resolveEndpointUrl(
        `${registeredClientBaseEndpoint}/dashboard`,
        firstEnvValue(
          import.meta.env.VITE_PLATFORM_API_DASHBOARD_ENDPOINT,
          import.meta.env.VITE_PLATFORM_REGISTERED_CLIENT_DASHBOARD_ENDPOINT
        )
      ),
      tiers: resolveEndpointUrl(
        `${registeredClientBaseEndpoint}/tiers`,
        firstEnvValue(
          import.meta.env.VITE_PLATFORM_API_TIERS_ENDPOINT,
          import.meta.env.VITE_PLATFORM_REGISTERED_CLIENT_TIERS_ENDPOINT
        )
      ),
      recentActivity: resolveEndpointUrl(
        `${registeredClientBaseEndpoint}/recent-user-activity`,
        firstEnvValue(
          import.meta.env.VITE_PLATFORM_API_RECENT_ACTIVITY_ENDPOINT,
          import.meta.env.VITE_PLATFORM_REGISTERED_CLIENT_RECENT_ACTIVITY_ENDPOINT
        )
      ),
    },
  },
};
