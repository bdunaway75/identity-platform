export const ClientAuthenticationMethods = {
    CLIENT_SECRET_BASIC: {
        value: 'client_secret_basic',
        label: 'Client Secret (Basic)',
    },
    CLIENT_SECRET_POST: {
        value: 'client_secret_post',
        label: 'Client Secret (Post)',
    },
    CLIENT_SECRET_JWT: {
        value: 'client_secret_jwt',
        label: 'Client Secret (JWT)',
    },
    PRIVATE_KEY_JWT: {
        value: 'private_key_jwt',
        label: 'Private Key JWT',
    },
    TLS_CLIENT_AUTH: {
        value: 'tls_client_auth',
        label: 'Transport-Layer-Security Client Auth',
    },
    SELF_SIGNED_TLS_CLIENT_AUTH: {
        value: 'self_signed_tls_client_auth',
        label: 'Self Signed Transport-Layer-Security Client Auth',
    },
    NONE: {
        value: 'none',
        label: 'None',
    },
};
