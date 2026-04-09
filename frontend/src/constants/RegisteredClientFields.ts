import type {
    RegisteredClientClientSettings,
    RegisteredClientTokenSettings,
} from "../types/RegisteredClientDto";

export type RegisteredClientFieldInfoKey =
    | "clientName"
    | "clientOrigin"
    | "clientAuthenticationMethods"
    | "authorizationGrantTypes"
    | "authorities"
    | "roles"
    | "scopes"
    | "redirectUris"
    | "postLogoutRedirectUris"
    | keyof RegisteredClientClientSettings
    | keyof RegisteredClientTokenSettings;

type RegisteredClientTokenField = {
    name: Exclude<keyof RegisteredClientTokenSettings, "reuseRefreshTokens">;
    label: string;
    placeholder: string;
    type: "number";
    min: number;
};

type RegisteredClientClientSettingField = {
    name: keyof RegisteredClientClientSettings;
    label: string;
    description: string;
};

export const registeredClientFieldInfo: Record<RegisteredClientFieldInfoKey, string> = {
    clientName:
        "The display name for this app in your auth server and consent/login flows.",
    clientOrigin:
        "The app base URL. Short paths like /callback are built from this origin.",
    clientAuthenticationMethods:
        "How this app proves its identity to your auth server at token exchange time.",
    authorizationGrantTypes:
        "Which OAuth flows this app is allowed to run, like authorization code or client credentials.",
    authorities:
        "Permission strings your app/API can check, for example CLIENTS_PAID or USERS_MANAGE.",
    roles:
        "Role strings for role-based checks. Press Enter and ROLE_ is added automatically.",
    scopes:
        "Permissions this app is allowed to request from users during sign-in.",
    redirectUris:
        "Where your auth server sends the browser after login/authorization. Must match your app callback URLs.",
    postLogoutRedirectUris:
        "Where users are sent after logout is complete.",
    requireProofKey:
        "Require PKCE for code flow. Recommended for browser/mobile apps for better security.",
    requireAuthorizationConsent:
        "Show a consent screen before granting requested permissions.",
    accessTokenTimeToLive:
        "How long issued access tokens remain valid.",
    refreshTokenTimeToLive:
        "How long refresh tokens remain valid.",
    authorizationCodeTimeToLive:
        "How long an authorization code can be exchanged before it expires.",
    reuseRefreshTokens:
        "If off, refresh tokens rotate after each use instead of being reused.",
};

export const tokenSettingsFields: RegisteredClientTokenField[] = [
    {
        name: "accessTokenTimeToLive",
        label: "Access Token TTL (minutes)",
        placeholder: "30",
        type: "number",
        min: 1,
    },
    {
        name: "refreshTokenTimeToLive",
        label: "Refresh Token TTL (minutes)",
        placeholder: "43200",
        type: "number",
        min: 1,
    },
    {
        name: "authorizationCodeTimeToLive",
        label: "Authorization Code TTL (minutes)",
        placeholder: "5",
        type: "number",
        min: 1,
    },
];

export const clientSettingsFields: RegisteredClientClientSettingField[] = [
    {
        name: "requireProofKey",
        label: "Require PKCE",
        description: "Recommended for public or browser-based clients.",
    },
    {
        name: "requireAuthorizationConsent",
        label: "Require Consent",
        description: "Prompt the user to approve requested scopes.",
    },
];
