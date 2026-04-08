import { useState } from 'react';
import { Checkbox, MultiSelect, TagsInput } from '@mantine/core';
import Button from '../components/Button.jsx';
import TextInput from '../components/TextInput.jsx';
import { useSubscription } from "../context/SubscriptionContext";
import {
    clientSettingsFields,
    createRegisteredClient,
    tokenSettingsFields,
} from '../types/RegisteredClientDto';
import { ClientAuthenticationMethods } from '../constants/ClientAuthenticationMethods';
import { AuthorizationGrants } from '../constants/AuthorizationGrantTypes';
import { ClientValidationMessages } from '../constants/ClientValidationMessages';
import { createClient, serializeRegisteredClientPayload } from '../services/clients';
import './Clients.css';

const REQUIRED_REDIRECT_GRANTS = new Set(['authorization_code']);
const ALLOWED_URL_PROTOCOLS = new Set(['http:', 'https:']);
const URL_LIKE_SCHEME_PATTERN = /^[a-z][a-z0-9+.-]*:/i;
const CLIENT_SELECT_CLASSNAMES = {
    input: 'client-select-input',
    section: 'client-select-section',
    dropdown: 'client-select-dropdown',
    option: 'client-select-option',
    pill: 'client-select-pill',
    pillsList: 'client-select-pills-list',
    inputField: 'client-select-input-field',
};

function normalizeOrigin(originInput) {
    const trimmed = originInput.trim();

    if (!trimmed) {
        return '';
    }

    const candidate = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;

    try {
        return new URL(candidate).origin;
    } catch {
        return '';
    }
}

function isValidAbsoluteUrl(value) {
    try {
        const parsed = new URL(value);
        return ALLOWED_URL_PROTOCOLS.has(parsed.protocol);
    } catch {
        return false;
    }
}

function isValidOrigin(value) {
    if (!value) {
        return false;
    }

    try {
        const parsed = new URL(value);
        return ALLOWED_URL_PROTOCOLS.has(parsed.protocol) && parsed.origin === value;
    } catch {
        return false;
    }
}

function hasWhitespace(value) {
    return /\s/.test(value);
}

function validateOriginInput(originInput) {
    const trimmed = originInput.trim();

    if (!trimmed) {
        return ClientValidationMessages.baseUrlRequired;
    }

    if (hasWhitespace(trimmed)) {
        return ClientValidationMessages.baseUrlNoSpaces;
    }

    const candidate = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;

    try {
        const parsed = new URL(candidate);
        const hasExtraPath = parsed.pathname && parsed.pathname !== '/';

        if (!ALLOWED_URL_PROTOCOLS.has(parsed.protocol)) {
            return ClientValidationMessages.baseUrlProtocol;
        }

        if (parsed.username || parsed.password || parsed.search || parsed.hash || hasExtraPath) {
            return ClientValidationMessages.baseUrlOnly;
        }

        return '';
    } catch {
        return ClientValidationMessages.baseUrlRequired;
    }
}

function validateUriInput(rawValue, origin) {
    const trimmed = rawValue.trim();

    if (!trimmed) {
        return ClientValidationMessages.urlEmpty;
    }

    if (hasWhitespace(trimmed)) {
        return ClientValidationMessages.urlNoSpaces;
    }

    if (/^https?:\/\//i.test(trimmed)) {
        return isValidAbsoluteUrl(trimmed) ? '' : ClientValidationMessages.urlAbsolute;
    }

    if (URL_LIKE_SCHEME_PATTERN.test(trimmed)) {
        return ClientValidationMessages.urlHttpOnly;
    }

    if (!trimmed.startsWith('/')) {
        return ClientValidationMessages.urlPathOrAbsolute;
    }

    const builtValue = buildUri(origin, trimmed);
    return isValidAbsoluteUrl(builtValue) ? '' : ClientValidationMessages.urlResolvable;
}

function validateUriList(rawValues, resolvedValues, fieldLabel, origin) {
    if (rawValues.length !== resolvedValues.length) {
        return ClientValidationMessages.invalidEntries(fieldLabel);
    }

    const invalidValues = rawValues
        .map(value => ({ value, error: validateUriInput(value, origin) }))
        .filter(result => result.error);

    if (invalidValues.length === 0) {
        return '';
    }

    const [{ value, error }] = invalidValues;
    return ClientValidationMessages.invalidEntryDetails(fieldLabel, error, value);
}

function buildUri(origin, value) {
    const trimmed = value.trim();

    if (!trimmed) {
        return '';
    }

    if (/^https?:\/\//i.test(trimmed)) {
        return trimmed;
    }

    if (!origin) {
        return trimmed;
    }

    const normalizedPath = trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
    return `${origin}${normalizedPath}`;
}

function sanitizeTagValues(values) {
    return [...new Set(values.map(value => value.trim()).filter(Boolean))];
}

function buildSelectOptions(options) {
    return Object.values(options).map(option => ({
        value: option.value,
        label: option.label,
    }));
}

function buildPayload(client) {
    const normalizedOrigin = normalizeOrigin(client.clientOrigin);
    const redirectUris = sanitizeTagValues(client.redirectUris).map(value => buildUri(normalizedOrigin, value));
    const postLogoutRedirectUris = sanitizeTagValues(client.postLogoutRedirectUris).map(value => buildUri(normalizedOrigin, value));

    return {
        clientId: null,
        clientIdIssuedAt: null,
        clientSecret: null,
        clientSecretExpiresAt: null,
        clientName: client.clientName.trim(),
        clientAuthenticationMethods: sanitizeTagValues(client.clientAuthenticationMethods),
        authorizationGrantTypes: sanitizeTagValues(client.authorizationGrantTypes),
        redirectUris,
        postLogoutRedirectUris,
        scopes: sanitizeTagValues(client.scopes),
        clientSettings: {
            requireProofKey: client.clientSettings.requireProofKey,
            requireAuthorizationConsent: client.clientSettings.requireAuthorizationConsent,
        },
        tokenSettings: {
            accessTokenTimeToLiveMinutes: Number(client.tokenSettings.accessTokenTimeToLiveMinutes),
            refreshTokenTimeToLiveMinutes: Number(client.tokenSettings.refreshTokenTimeToLiveMinutes),
            authorizationCodeTimeToLiveMinutes: Number(client.tokenSettings.authorizationCodeTimeToLiveMinutes),
            reuseRefreshTokens: client.tokenSettings.reuseRefreshTokens,
        },
    };
}

function validateClient(client) {
    const payload = buildPayload(client);
    const normalizedOrigin = normalizeOrigin(client.clientOrigin);
    const errors = {};

    if (!payload.clientName) {
        errors.clientName = ClientValidationMessages.clientNameRequired;
    }

    const clientOriginError = validateOriginInput(client.clientOrigin);
    if (clientOriginError || !normalizedOrigin || !isValidOrigin(normalizedOrigin)) {
        errors.clientOrigin = clientOriginError || ClientValidationMessages.baseUrlOnly;
    }

    if (payload.clientAuthenticationMethods.length === 0) {
        errors.clientAuthenticationMethods = ClientValidationMessages.authMethodRequired;
    }

    if (payload.authorizationGrantTypes.length === 0) {
        errors.authorizationGrantTypes = ClientValidationMessages.grantTypeRequired;
    }

    if (payload.scopes.length === 0) {
        errors.scopes = ClientValidationMessages.scopesRequired;
    }

    if (payload.authorizationGrantTypes.some(grant => REQUIRED_REDIRECT_GRANTS.has(grant)) && payload.redirectUris.length === 0) {
        errors.redirectUris = ClientValidationMessages.redirectRequired;
    }

    const redirectUriError = validateUriList(client.redirectUris, payload.redirectUris, 'Redirect URLs', normalizedOrigin);
    if (redirectUriError) {
        errors.redirectUris = redirectUriError;
    }

    const postLogoutRedirectUriError = validateUriList(
        client.postLogoutRedirectUris,
        payload.postLogoutRedirectUris,
        'Sign-out redirect URLs',
        normalizedOrigin
    );
    if (postLogoutRedirectUriError) {
        errors.postLogoutRedirectUris = postLogoutRedirectUriError;
    }

    if (payload.authorizationGrantTypes.includes('client_credentials') && payload.clientAuthenticationMethods.includes('none')) {
        errors.clientAuthenticationMethods = ClientValidationMessages.clientCredentialsNone;
    }

    if (payload.clientAuthenticationMethods.includes('none') && !payload.clientSettings.requireProofKey) {
        errors.requireProofKey = ClientValidationMessages.publicClientPkce;
    }

    for (const [name, value] of Object.entries(payload.tokenSettings)) {
        if (typeof value === 'number' && (!Number.isFinite(value) || value < 1)) {
            errors[name] = ClientValidationMessages.positiveWholeNumber;
        }
    }

    return { errors, payload };
}

export default function Clients() {
    const [client, setClient] = useState(createRegisteredClient);
    const [errors, setErrors] = useState({});
    const [isReady, setIsReady] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState('');
    const [createdClientId, setCreatedClientId] = useState('');
    const [createdClientSecret, setCreatedClientSecret] = useState('');
    const { tier, status } = useSubscription();

    const canAccessClients = tier === 'paid';
    const showPayloadPreview = import.meta.env.DEV;
    const requiresRedirectUris = client.authorizationGrantTypes.some(grant => REQUIRED_REDIRECT_GRANTS.has(grant));
    const payloadPreview = JSON.stringify(serializeRegisteredClientPayload(buildPayload(client)), null, 2);
    const authMethodOptions = buildSelectOptions(ClientAuthenticationMethods);
    const grantTypeOptions = buildSelectOptions(AuthorizationGrants);

    function updateField(fieldName, value) {
        setIsReady(false);
        setSubmitError('');
        setCreatedClientId('');
        setCreatedClientSecret('');
        setClient(prev => ({
            ...prev,
            [fieldName]: value,
        }));
    }

    function updateTokenSetting(fieldName, value) {
        setIsReady(false);
        setSubmitError('');
        setCreatedClientId('');
        setCreatedClientSecret('');
        setClient(prev => ({
            ...prev,
            tokenSettings: {
                ...prev.tokenSettings,
                [fieldName]: value,
            },
        }));
    }

    function updateClientSetting(fieldName, value) {
        setIsReady(false);
        setSubmitError('');
        setCreatedClientId('');
        setCreatedClientSecret('');
        setClient(prev => ({
            ...prev,
            clientSettings: {
                ...prev.clientSettings,
                [fieldName]: value,
            },
        }));
    }

    function handleSubmit(event) {
        event.preventDefault();

        const result = validateClient(client);
        setErrors(result.errors);
        setSubmitError('');
        setCreatedClientId('');
        setCreatedClientSecret('');

        if (Object.keys(result.errors).length > 0) {
            setIsReady(false);
            return;
        }

        if (!isReady) {
            setIsReady(true);
            return;
        }

        setIsSubmitting(true);
        createClient(result.payload)
            .then((response) => {
                setCreatedClientId(String(response?.clientId ?? '').trim());
                setCreatedClientSecret(String(response?.clientSecret ?? '').trim());
            })
            .catch((error) => {
                setIsReady(false);
                setSubmitError(error.message || 'Unable to create client.');
            })
            .finally(() => {
                setIsSubmitting(false);
            });
    }

    function handleReset() {
        setClient(createRegisteredClient());
        setErrors({});
        setIsReady(false);
        setSubmitError('');
        setCreatedClientId('');
        setCreatedClientSecret('');
    }

    if (status === 'loading') {
        return (
            <div className="clients-locked-page">
                <div className="clients-locked-card">
                    <div className="clients-locked-kicker">Checking plan</div>
                    <h1>Loading subscription access</h1>
                    <p className="clients-locked-copy">
                        We are checking whether this account can register clients.
                    </p>
                </div>
            </div>
        );
    }

    if (!canAccessClients) {
        return (
            <div className="clients-locked-page">
                <div className="clients-locked-card">
                    <div className="clients-locked-kicker">Paid Feature</div>
                    <h1>Unlock client registration with a paid plan</h1>
                    <p className="clients-locked-copy">
                        Upgrade your plan to enable secure client registration and application access controls.
                    </p>
                    <button type="button" className="clients-lock-button">
                        Purchase
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="clients-page">
            <div className="client-shell">
                <div className="client-header">
                    <div className="client-header-copy">
                        <div className="client-eyebrow">New Client</div>
                        <div className="client-title">Set up an app people can sign in to</div>
                        <div className="client-subtitle">
                            Fill in the basics, choose how the app signs in, and we will help you catch anything missing along the way.
                        </div>
                    </div>
                </div>

                <form className="clients-layout" onSubmit={handleSubmit}>
                    <div className="clients-workspace">
                        <section className="client-card client-card-primary">
                            <div className="client-card-header">
                                <div>
                                    <div className="client-card-kicker">Basics</div>
                                    <div className="client-card-title">App details and access</div>
                                </div>
                                <div className="client-card-caption">
                                    Name the app, choose how it signs in, and add the URLs and permissions it needs.
                                </div>
                            </div>
                            <div className="client-card-contents">
                                <TextInput
                                    placeholder="e.g. frontend-spa"
                                    label="Client Name"
                                    value={client.clientName}
                                    error={errors.clientName}
                                    onChange={value => updateField('clientName', value)}
                                />

                                <TextInput
                                    placeholder="e.g. https://app.example.com or http://localhost:5173"
                                    label="Client Origin"
                                    value={client.clientOrigin}
                                    error={errors.clientOrigin}
                                    onChange={value => updateField('clientOrigin', value)}
                                />

                                <MultiSelect
                                    placeholder="Choose auth methods"
                                    data={authMethodOptions}
                                    label="Client Authentication Methods"
                                    classNames={CLIENT_SELECT_CLASSNAMES}
                                    clearable
                                    searchable
                                    clearButtonProps={{ className: 'client-select-clear' }}
                                    value={client.clientAuthenticationMethods}
                                    error={errors.clientAuthenticationMethods}
                                    onChange={values => updateField('clientAuthenticationMethods', values)}
                                />

                                <MultiSelect
                                    placeholder="Choose grant types"
                                    data={grantTypeOptions}
                                    label="Authorization Grant Types"
                                    classNames={CLIENT_SELECT_CLASSNAMES}
                                    clearable
                                    searchable
                                    clearButtonProps={{ className: 'client-select-clear' }}
                                    value={client.authorizationGrantTypes}
                                    error={errors.authorizationGrantTypes}
                                    onChange={values => updateField('authorizationGrantTypes', values)}
                                />

                                <div className="client-field-full">
                                    <TagsInput
                                        placeholder="e.g. users:write, users:read"
                                        label="Registered Client Allowed Scopes"
                                        classNames={CLIENT_SELECT_CLASSNAMES}
                                        clearButtonProps={{ className: 'client-select-clear' }}
                                        value={client.scopes}
                                        error={errors.scopes}
                                        onChange={values => updateField('scopes', sanitizeTagValues(values))}
                                    />
                                    <div className="client-field-help">
                                        Add the permissions this app should be allowed to ask for.
                                    </div>
                                </div>

                                <div className="client-field-full">
                                    <TagsInput
                                        placeholder="e.g. /callback"
                                        label="Redirect URIs"
                                        classNames={CLIENT_SELECT_CLASSNAMES}
                                        clearButtonProps={{ className: 'client-select-clear' }}
                                        value={client.redirectUris}
                                        error={errors.redirectUris}
                                        onChange={values => updateField('redirectUris', sanitizeTagValues(values))}
                                    />
                                    <div className="client-field-help">
                                        Add full URLs or paths like /callback. Paths will be combined with the base URL.
                                    </div>
                                </div>

                                <div className="client-field-full">
                                    <TagsInput
                                        placeholder="e.g. /logout-callback"
                                        label="Post Logout Redirect URIs"
                                        classNames={CLIENT_SELECT_CLASSNAMES}
                                        clearButtonProps={{ className: 'client-select-clear' }}
                                        value={client.postLogoutRedirectUris}
                                        error={errors.postLogoutRedirectUris}
                                        onChange={values => updateField('postLogoutRedirectUris', sanitizeTagValues(values))}
                                    />
                                    <div className="client-field-help">
                                        Optional. Add full URLs or paths for where people should land after signing out.
                                    </div>
                                </div>
                            </div>
                        </section>

                        <div className="clients-sidebar">
                            <section className="client-card client-card-secondary">
                                <div className="client-card-header">
                                    <div>
                                        <div className="client-card-kicker">Security</div>
                                        <div className="client-card-title">Sign-in and session settings</div>
                                    </div>
                                    <div className="client-card-caption">
                                        Fine-tune how secure sign-in should be and how long sessions last.
                                    </div>
                                </div>
                                <div className="client-card-contents">
                                    {clientSettingsFields.map(setting => (
                                        <label className="client-checkbox" key={setting.name}>
                                            <Checkbox
                                                checked={client.clientSettings[setting.name]}
                                                onChange={event => updateClientSetting(setting.name, event.currentTarget.checked)}
                                            />
                                            <div>
                                                <div className="client-checkbox-label">{setting.label}</div>
                                                <div className="client-checkbox-description">{setting.description}</div>
                                                {errors[setting.name] ? <div className="client-checkbox-error">{errors[setting.name]}</div> : null}
                                            </div>
                                        </label>
                                    ))}

                                    {tokenSettingsFields.map(field => (
                                        <TextInput
                                            key={field.name}
                                            placeholder={field.placeholder}
                                            label={field.label}
                                            type="number"
                                            inputMode="numeric"
                                            min={field.min}
                                            step="1"
                                            value={client.tokenSettings[field.name]}
                                            error={errors[field.name]}
                                            onChange={value => updateTokenSetting(field.name, value)}
                                        />
                                    ))}

                                    <label className="client-checkbox">
                                        <Checkbox
                                            checked={client.tokenSettings.reuseRefreshTokens}
                                            onChange={event => updateTokenSetting('reuseRefreshTokens', event.currentTarget.checked)}
                                        />
                                        <div>
                                            <div className="client-checkbox-label">Reuse refresh tokens</div>
                                            <div className="client-checkbox-description">
                                                Turn this off if you want refresh tokens to rotate after each use.
                                            </div>
                                        </div>
                                    </label>

                                    {requiresRedirectUris ? (
                                        <div className="client-note">
                                            A callback URL is required right now because this app uses the authorization code flow.
                                        </div>
                                    ) : null}
                                </div>
                            </section>
                        </div>
                    </div>

                    <div className="client-actions">
                        <Button type="button" variant="secondary" onClick={handleReset}>Reset</Button>
                        <Button type="submit">{isSubmitting ? 'Sending...' : isReady ? 'Submit client' : 'Check details'}</Button>
                    </div>

                    {submitError ? <div className="client-submit-message">{submitError}</div> : null}

                    {createdClientId ? (
                        <div className="client-success-card">
                            <div className="client-success-kicker">Client Created</div>
                            <div className="client-success-id">{createdClientId}</div>
                            {createdClientSecret ? (
                                <>
                                    <div className="client-success-kicker">Client Secret</div>
                                    <div className="client-success-id">{createdClientSecret}</div>
                                </>
                            ) : null}
                            <div className="client-success-note">
                                Save this {createdClientSecret ? 'client ID and client secret' : 'client ID'} somewhere safe. {createdClientSecret ? 'The secret is only shown at creation time.' : 'You will need it later when configuring apps against this auth server.'}
                            </div>
                        </div>
                    ) : null}

                    {showPayloadPreview ? (
                        <details className="client-devtools">
                            <summary className="client-devtools-summary">Generated JSON payload</summary>
                            <pre className="client-devtools-code">{payloadPreview}</pre>
                        </details>
                    ) : null}
                </form>
            </div>
        </div>
    );
}
