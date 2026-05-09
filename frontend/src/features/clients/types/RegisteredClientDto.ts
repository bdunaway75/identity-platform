import { ClientAuthenticationMethods } from "../constants/ClientAuthenticationMethods";
import { AuthorizationGrants } from "../constants/AuthorizationGrantTypes";

export interface RegisteredClientTokenSettings {
    accessTokenTimeToLive: string;
    refreshTokenTimeToLive: string;
    authorizationCodeTimeToLive: string;
    reuseRefreshTokens: boolean;
}

export interface RegisteredClientClientSettings {
    requireProofKey: boolean;
    requireAuthorizationConsent: boolean;
}

export interface RegisteredClientDto {
    clientName: string;
    clientOrigin: string;
    clientAuthenticationMethods: string[];
    authorizationGrantTypes: string[];
    authorities: string[];
    roles: string[];
    redirectUris: string[];
    postLogoutRedirectUris: string[];
    scopes: string[];
    tokenSettings: RegisteredClientTokenSettings;
    clientSettings: RegisteredClientClientSettings;
}

export function createRegisteredClient(): RegisteredClientDto {
    return {
        clientName: '',
        clientOrigin: '',
        clientAuthenticationMethods: [ClientAuthenticationMethods.NONE.value],
        authorizationGrantTypes: [AuthorizationGrants.AUTHORIZATION_CODE.value],
        authorities: [],
        roles: [],
        redirectUris: [],
        postLogoutRedirectUris: [],
        scopes: [],
        tokenSettings: {
            accessTokenTimeToLive: '30',
            refreshTokenTimeToLive: '43200',
            authorizationCodeTimeToLive: '5',
            reuseRefreshTokens: true,
        },
        clientSettings: {
            requireProofKey: true,
            requireAuthorizationConsent: false,
        },
    };
}

