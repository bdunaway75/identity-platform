import { ClientAuthenticationMethods } from "../constants/ClientAuthenticationMethods";
import { AuthorizationGrants } from "../constants/AuthorizationGrantTypes";

export const tokenSettingsFields = [
    {
        name: 'accessTokenTimeToLiveMinutes',
        label: 'Access Token TTL (minutes)',
        placeholder: '30',
        type: 'number',
        min: 1,
    },
    {
        name: 'refreshTokenTimeToLiveMinutes',
        label: 'Refresh Token TTL (minutes)',
        placeholder: '43200',
        type: 'number',
        min: 1,
    },
    {
        name: 'authorizationCodeTimeToLiveMinutes',
        label: 'Authorization Code TTL (minutes)',
        placeholder: '5',
        type: 'number',
        min: 1,
    },
];

export const clientSettingsFields = [
    {
        name: 'requireProofKey',
        label: 'Require PKCE',
        description: 'Recommended for public or browser-based clients.',
    },
    {
        name: 'requireAuthorizationConsent',
        label: 'Require Consent',
        description: 'Prompt the user to approve requested scopes.',
    },
];

export function createRegisteredClient() {
    return {
        clientName: '',
        clientOrigin: '',
        clientAuthenticationMethods: [ClientAuthenticationMethods.NONE.value],
        authorizationGrantTypes: [AuthorizationGrants.AUTHORIZATION_CODE.value],
        redirectUris: [],
        postLogoutRedirectUris: [],
        scopes: [],
        tokenSettings: {
            accessTokenTimeToLiveMinutes: '30',
            refreshTokenTimeToLiveMinutes: '43200',
            authorizationCodeTimeToLiveMinutes: '5',
            reuseRefreshTokens: true,
        },
        clientSettings: {
            requireProofKey: true,
            requireAuthorizationConsent: false,
        },
    };
}

