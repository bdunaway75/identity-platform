import type {
    RegisteredClientClientSettings,
    RegisteredClientDto,
    RegisteredClientTokenSettings,
} from "./RegisteredClientDto";
import type { RegisteredClientFieldInfoKey } from "../constants/RegisteredClientFields";

export type RegisteredClientBaseFields = Omit<RegisteredClientDto, "tokenSettings" | "clientSettings">;

export type RegisteredClientFieldName = keyof RegisteredClientBaseFields;

export type RegisteredClientClientSettingFieldName = keyof RegisteredClientClientSettings;

export type RegisteredClientTokenSettingFieldName = keyof RegisteredClientTokenSettings;

export type RegisteredClientValidationErrors = Partial<Record<RegisteredClientFieldInfoKey, string>>;

export type RegisteredClientResponseLike = Partial<Omit<RegisteredClientDto, "tokenSettings" | "clientSettings">> & {
    id?: string | null;
    clientId?: string | null;
    clientSecret?: string | null;
    clientSettings?: (Partial<RegisteredClientClientSettings> & Record<string, unknown>) | Record<string, unknown>;
    tokenSettings?: (Partial<RegisteredClientTokenSettings> & Record<string, unknown>) | Record<string, unknown>;
};

export type RegisteredClientApiPayload = {
    clientId: null;
    clientIdIssuedAt: null;
    clientSecret: null;
    clientSecretExpiresAt: null;
    clientName: string;
    clientAuthenticationMethods: string[];
    authorizationGrantTypes: string[];
    redirectUris: string[];
    postLogoutRedirectUris: string[];
    scopes: string[];
    authorities: string[];
    roles: string[];
    clientSettings: RegisteredClientClientSettings;
    tokenSettings: {
        accessTokenTimeToLive: number;
        refreshTokenTimeToLive: number;
        authorizationCodeTimeToLive: number;
        reuseRefreshTokens: boolean;
    };
};
