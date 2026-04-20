import { useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import { Checkbox, MultiSelect, TagsInput, Tooltip } from "@mantine/core";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import Button from "../components/Button";
import TextInput from "../components/TextInput";
import { createRegisteredClient } from "../types/RegisteredClientDto";
import type {
  RegisteredClientClientSettings,
  RegisteredClientDto,
  RegisteredClientTokenSettings,
} from "../types/RegisteredClientDto";
import type {
  RegisteredClientApiPayload,
  RegisteredClientBaseFields,
  RegisteredClientClientSettingFieldName,
  RegisteredClientFieldName,
  RegisteredClientResponseLike,
  RegisteredClientTokenSettingFieldName,
  RegisteredClientValidationErrors,
} from "../types/RegisteredClientForm";
import {
  clientSettingsFields,
  registeredClientFieldInfo,
  tokenSettingsFields,
} from "../constants/RegisteredClientFields";
import { ClientAuthenticationMethods } from "../constants/ClientAuthenticationMethods";
import { AuthorizationGrants } from "../constants/AuthorizationGrantTypes";
import { ClientValidationMessages } from "../constants/ClientValidationMessages";
import { createClient, updateClient } from "../services/clients";
import { fetchRegisteredClient } from "../services/platform";
import { useSubscription } from "../context/SubscriptionContext";
import "./Clients.css";

const REQUIRED_REDIRECT_GRANTS = new Set(["authorization_code"]);
const ALLOWED_URL_PROTOCOLS = new Set(["http:", "https:"]);
const URL_LIKE_SCHEME_PATTERN = /^[a-z][a-z0-9+.-]*:/i;
const CLIENT_SELECT_CLASSNAMES = {
  input: "client-select-input",
  section: "client-select-section",
  dropdown: "client-select-dropdown",
  option: "client-select-option",
  pill: "client-select-pill",
  pillsList: "client-select-pills-list",
  inputField: "client-select-input-field",
};
const SPRING_CLIENT_SETTINGS_KEYS = {
  requireProofKey: "settings.client.require-proof-key",
  requireAuthorizationConsent: "settings.client.require-authorization-consent",
} as const;
type CreateClientLocationState = {
  registeredClient?: RegisteredClientResponseLike | null;
} | null;

function getErrorMessage(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback;
}

function InfoLabel({ label, info }: { label: string; info: string }) {
  return (
    <span className="client-label-with-info">
      <span>{label}</span>
      <Tooltip
        label={info}
        withArrow
        multiline
        w={320}
        position="top-start"
        openDelay={120}
        transitionProps={{ transition: "fade-up", duration: 120 }}
        classNames={{
          tooltip: "client-info-tooltip",
          arrow: "client-info-tooltip-arrow",
        }}
      >
        <button
          type="button"
          className="client-label-info-trigger"
          aria-label={`More info about ${label}`}
        >
          ?
        </button>
      </Tooltip>
    </span>
  );
}

function normalizeOrigin(originInput: string): string {
  const trimmed = originInput.trim();

  if (!trimmed) {
    return "";
  }

  const candidate = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;

  try {
    return new URL(candidate).origin;
  } catch {
    return "";
  }
}

function isValidAbsoluteUrl(value: string): boolean {
  try {
    const parsed = new URL(value);
    return ALLOWED_URL_PROTOCOLS.has(parsed.protocol);
  } catch {
    return false;
  }
}

function isValidOrigin(value: string): boolean {
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

function hasWhitespace(value: string): boolean {
  return /\s/.test(value);
}

function validateOriginInput(originInput: string): string {
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
    const hasExtraPath = parsed.pathname && parsed.pathname !== "/";

    if (!ALLOWED_URL_PROTOCOLS.has(parsed.protocol)) {
      return ClientValidationMessages.baseUrlProtocol;
    }

    if (parsed.username || parsed.password || parsed.search || parsed.hash || hasExtraPath) {
      return ClientValidationMessages.baseUrlOnly;
    }

    return "";
  } catch {
    return ClientValidationMessages.baseUrlRequired;
  }
}

function validateUriInput(rawValue: string, origin: string): string {
  const trimmed = rawValue.trim();

  if (!trimmed) {
    return ClientValidationMessages.urlEmpty;
  }

  if (hasWhitespace(trimmed)) {
    return ClientValidationMessages.urlNoSpaces;
  }

  if (/^https?:\/\//i.test(trimmed)) {
    return isValidAbsoluteUrl(trimmed) ? "" : ClientValidationMessages.urlAbsolute;
  }

  if (URL_LIKE_SCHEME_PATTERN.test(trimmed)) {
    return ClientValidationMessages.urlHttpOnly;
  }

  if (!trimmed.startsWith("/")) {
    return ClientValidationMessages.urlPathOrAbsolute;
  }

  const builtValue = buildUri(origin, trimmed);
  return isValidAbsoluteUrl(builtValue) ? "" : ClientValidationMessages.urlResolvable;
}

function validateUriList(
  rawValues: string[],
  resolvedValues: string[],
  fieldLabel: string,
  origin: string
): string {
  if (rawValues.length !== resolvedValues.length) {
    return ClientValidationMessages.invalidEntries(fieldLabel);
  }

  const invalidValues = rawValues
    .map((value) => ({ value, error: validateUriInput(value, origin) }))
    .filter((result) => result.error);

  if (invalidValues.length === 0) {
    return "";
  }

  const [{ value, error }] = invalidValues;
  return ClientValidationMessages.invalidEntryDetails(fieldLabel, error, value);
}

function buildUri(origin: string, value: string): string {
  const trimmed = value.trim();

  if (!trimmed) {
    return "";
  }

  if (/^https?:\/\//i.test(trimmed)) {
    return trimmed;
  }

  if (!origin) {
    return trimmed;
  }

  const normalizedPath = trimmed.startsWith("/") ? trimmed : `/${trimmed}`;
  return `${origin}${normalizedPath}`;
}

function sanitizeTagValues(values: string[] | null | undefined): string[] {
  return [...new Set(normalizeArray(values).map((value) => value.trim()).filter(Boolean))];
}

function sanitizeRoleValues(values: string[] | null | undefined): string[] {
  return sanitizeTagValues(values).map((value) => {
    const normalizedValue = value.toUpperCase();
    return normalizedValue.startsWith("ROLE_") ? normalizedValue : `ROLE_${normalizedValue}`;
  });
}

function trimToLimit(
  values: string[],
  maxItems: number,
  sanitizer: (values: string[] | null | undefined) => string[] = sanitizeTagValues
): string[] {
  const normalizedValues = sanitizer(values);
  return Number.isFinite(maxItems) ? normalizedValues.slice(0, Math.max(0, maxItems)) : normalizedValues;
}

function normalizeArray<T>(value: T[] | null | undefined): T[] {
  return Array.isArray(value) ? value : [];
}

function toBoolean(value: unknown, fallback = false): boolean {
  return typeof value === "boolean" ? value : fallback;
}

function toStringNumber(value: unknown, fallback: string): string {
  const parsed = Number.parseInt(String(value ?? ""), 10);
  return Number.isFinite(parsed) ? String(parsed) : fallback;
}

function durationToMinuteString(value: unknown, fallback: string): string {
  const normalized = String(value ?? "").trim();
  if (!normalized) {
    return fallback;
  }

  if (/^\d+$/.test(normalized)) {
    return normalized;
  }

  const match = normalized.match(/^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?$/i);
  if (!match) {
    return fallback;
  }

  const [, days = "0", hours = "0", minutes = "0", seconds = "0"] = match;
  const totalMinutes =
    Number.parseInt(days, 10) * 1440 +
    Number.parseInt(hours, 10) * 60 +
    Number.parseInt(minutes, 10) +
    Math.floor(Number.parseInt(seconds, 10) / 60);

  return String(totalMinutes);
}

function extractOriginFromUris(values: string[] | null | undefined): string {
  for (const value of normalizeArray(values)) {
    try {
      const parsed = new URL(String(value ?? "").trim());
      if (ALLOWED_URL_PROTOCOLS.has(parsed.protocol)) {
        return parsed.origin;
      }
    } catch {
      continue;
    }
  }

  return "";
}

function toEditableUri(value: unknown, origin: string): string {
  const trimmed = String(value ?? "").trim();
  if (!trimmed) {
    return "";
  }

  try {
    const parsed = new URL(trimmed);
    if (origin && parsed.origin === origin) {
      const path = `${parsed.pathname || ""}${parsed.search || ""}${parsed.hash || ""}`;
      return path || "/";
    }
    return parsed.toString();
  } catch {
    return trimmed;
  }
}

function deserializeRegisteredClientToForm(
  client: RegisteredClientResponseLike | null | undefined
): RegisteredClientDto {
  const defaultClient = createRegisteredClient();
  const authorities = sanitizeTagValues(client?.authorities);
  const roles = sanitizeRoleValues(client?.roles);
  const authorityScopeSet = new Set([...authorities, ...roles].map((value) => value.toUpperCase()));
  const redirectUris = sanitizeTagValues(client?.redirectUris);
  const postLogoutRedirectUris = sanitizeTagValues(client?.postLogoutRedirectUris);
  const clientOrigin =
    extractOriginFromUris(redirectUris) || extractOriginFromUris(postLogoutRedirectUris);
  const clientSettings = (client?.clientSettings ?? {}) as Partial<RegisteredClientClientSettings> & Record<string, unknown>;
  const tokenSettings = (client?.tokenSettings ?? {}) as Partial<RegisteredClientTokenSettings> & Record<string, unknown>;

  return {
    clientName: String(client?.clientName ?? "").trim(),
    clientOrigin,
    clientAuthenticationMethods: sanitizeTagValues(client?.clientAuthenticationMethods),
    authorizationGrantTypes: sanitizeTagValues(client?.authorizationGrantTypes),
    authorities,
    roles,
    redirectUris: redirectUris.map((value) => toEditableUri(value, clientOrigin)),
    postLogoutRedirectUris: postLogoutRedirectUris.map((value) => toEditableUri(value, clientOrigin)),
    scopes: sanitizeTagValues(client?.scopes).filter((scope) => !authorityScopeSet.has(scope.toUpperCase())),
    tokenSettings: {
      accessTokenTimeToLive: durationToMinuteString(
        tokenSettings.accessTokenTimeToLive ?? tokenSettings.accessTokenTimeToLiveMinutes,
        defaultClient.tokenSettings.accessTokenTimeToLive
      ),
      refreshTokenTimeToLive: durationToMinuteString(
        tokenSettings.refreshTokenTimeToLive ?? tokenSettings.refreshTokenTimeToLiveMinutes,
        defaultClient.tokenSettings.refreshTokenTimeToLive
      ),
      authorizationCodeTimeToLive: durationToMinuteString(
        tokenSettings.authorizationCodeTimeToLive ?? tokenSettings.authorizationCodeTimeToLiveMinutes,
        defaultClient.tokenSettings.authorizationCodeTimeToLive
      ),
      reuseRefreshTokens: toBoolean(
        tokenSettings.reuseRefreshTokens,
        defaultClient.tokenSettings.reuseRefreshTokens
      ),
    },
    clientSettings: {
      requireProofKey: toBoolean(
        clientSettings.requireProofKey ??
          clientSettings[SPRING_CLIENT_SETTINGS_KEYS.requireProofKey],
        defaultClient.clientSettings.requireProofKey
      ),
      requireAuthorizationConsent: toBoolean(
        clientSettings.requireAuthorizationConsent ??
          clientSettings[SPRING_CLIENT_SETTINGS_KEYS.requireAuthorizationConsent],
        defaultClient.clientSettings.requireAuthorizationConsent
      ),
    },
  };
}

function buildSelectOptions(options: Record<string, { value: string; label: string }>) {
  return Object.values(options).map((option) => ({
    value: option.value,
    label: option.label,
  }));
}

function buildPayload(client: RegisteredClientDto): RegisteredClientApiPayload {
  const normalizedOrigin = normalizeOrigin(client.clientOrigin);
  const redirectUris = sanitizeTagValues(client.redirectUris).map((value) => buildUri(normalizedOrigin, value));
  const postLogoutRedirectUris = sanitizeTagValues(client.postLogoutRedirectUris).map((value) => buildUri(normalizedOrigin, value));

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
    authorities: sanitizeTagValues(client.authorities),
    roles: sanitizeRoleValues(client.roles),
    clientSettings: {
      requireProofKey: client.clientSettings.requireProofKey,
      requireAuthorizationConsent: client.clientSettings.requireAuthorizationConsent,
    },
    tokenSettings: {
      accessTokenTimeToLive: Number(client.tokenSettings.accessTokenTimeToLive),
      refreshTokenTimeToLive: Number(client.tokenSettings.refreshTokenTimeToLive),
      authorizationCodeTimeToLive: Number(client.tokenSettings.authorizationCodeTimeToLive),
      reuseRefreshTokens: client.tokenSettings.reuseRefreshTokens,
    },
  };
}

function validateClient(
  client: RegisteredClientDto
): { errors: RegisteredClientValidationErrors; payload: RegisteredClientApiPayload } {
  const payload = buildPayload(client);
  const normalizedOrigin = normalizeOrigin(client.clientOrigin);
  const errors: RegisteredClientValidationErrors = {};

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

  if (payload.authorizationGrantTypes.some((grant) => REQUIRED_REDIRECT_GRANTS.has(grant)) && payload.redirectUris.length === 0) {
    errors.redirectUris = ClientValidationMessages.redirectRequired;
  }

  const redirectUriError = validateUriList(client.redirectUris, payload.redirectUris, "Redirect URLs", normalizedOrigin);
  if (redirectUriError) {
    errors.redirectUris = redirectUriError;
  }

  const postLogoutRedirectUriError = validateUriList(
    client.postLogoutRedirectUris,
    payload.postLogoutRedirectUris,
    "Sign-out redirect URLs",
    normalizedOrigin
  );
  if (postLogoutRedirectUriError) {
    errors.postLogoutRedirectUris = postLogoutRedirectUriError;
  }

  if (payload.authorizationGrantTypes.includes("client_credentials") && payload.clientAuthenticationMethods.includes("none")) {
    errors.clientAuthenticationMethods = ClientValidationMessages.clientCredentialsNone;
  }

  if (payload.clientAuthenticationMethods.includes("none") && !payload.clientSettings.requireProofKey) {
    errors.requireProofKey = ClientValidationMessages.publicClientPkce;
  }

  for (const [name, value] of Object.entries(payload.tokenSettings) as Array<
    [keyof RegisteredClientApiPayload["tokenSettings"], RegisteredClientApiPayload["tokenSettings"][keyof RegisteredClientApiPayload["tokenSettings"]]]
  >) {
    if (typeof value === "number" && (!Number.isFinite(value) || value < 1)) {
      errors[name] = ClientValidationMessages.positiveWholeNumber;
    }
  }

  return { errors, payload };
}

export default function CreateClient() {
  const { registeredClientId = "" } = useParams();
  const location = useLocation();
  const locationState = location.state as CreateClientLocationState;
  const navigate = useNavigate();
  const isEditMode = Boolean(registeredClientId);
  const seededRegisteredClient = useMemo<RegisteredClientResponseLike | null>(() => {
    const candidate = locationState?.registeredClient;
    if (!candidate || typeof candidate !== "object") {
      return null;
    }

    return String(candidate.id ?? "") === registeredClientId ? candidate : null;
  }, [locationState, registeredClientId]);
  const [client, setClient] = useState<RegisteredClientDto>(createRegisteredClient);
  const [initialClient, setInitialClient] = useState<RegisteredClientDto>(createRegisteredClient);
  const [errors, setErrors] = useState<RegisteredClientValidationErrors>({});
  const [isLoadingClient, setIsLoadingClient] = useState(isEditMode);
  const [loadError, setLoadError] = useState("");
  const [isReady, setIsReady] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const { limits, usage } = useSubscription();
  const requiresRedirectUris = client.authorizationGrantTypes.some((grant) => REQUIRED_REDIRECT_GRANTS.has(grant));
  const authMethodOptions = buildSelectOptions(ClientAuthenticationMethods);
  const grantTypeOptions = buildSelectOptions(AuthorizationGrants);
  const baselineScopeCount = initialClient.scopes.length;
  const baselineAuthorityCount = initialClient.authorities.length;
  const baselineRoleCount = initialClient.roles.length;
  const otherScopeUsage = Math.max(0, Number(usage.globalScopes || 0) - baselineScopeCount);
  const scopeLimit = Number(limits.globalScopes || 0);
  const maxCurrentScopeCount = scopeLimit > 0 ? Math.max(0, scopeLimit - otherScopeUsage) : Number.POSITIVE_INFINITY;
  const currentScopeTotal = otherScopeUsage + client.scopes.length;
  const otherAccessUsage = Math.max(
    0,
    Number(usage.globalAuthorities || 0) + Number(usage.roles || 0) - baselineAuthorityCount - baselineRoleCount
  );
  const accessLimit = Number(limits.globalAuthorities || 0);
  const maxCurrentAccessCount = accessLimit > 0 ? Math.max(0, accessLimit - otherAccessUsage) : Number.POSITIVE_INFINITY;
  const currentAccessTotal = otherAccessUsage + client.authorities.length + client.roles.length;
  const maxAuthorityCount = Number.isFinite(maxCurrentAccessCount)
    ? Math.max(0, maxCurrentAccessCount - client.roles.length)
    : Number.POSITIVE_INFINITY;
  const maxRoleCount = Number.isFinite(maxCurrentAccessCount)
    ? Math.max(0, maxCurrentAccessCount - client.authorities.length)
    : Number.POSITIVE_INFINITY;
  const hasRegisteredClientCapacity =
    isEditMode || Number(limits.registeredClients || 0) > Number(usage.registeredClients || 0);
  const createFormLockedByTier = !isEditMode && !hasRegisteredClientCapacity;
  const noScopeSlotsRemaining = Number.isFinite(maxCurrentScopeCount) && maxCurrentScopeCount === 0;
  const noAccessSlotsRemaining = Number.isFinite(maxCurrentAccessCount) && maxCurrentAccessCount === 0;
  const scopeLimitReached = Number.isFinite(maxCurrentScopeCount) && currentScopeTotal >= maxCurrentScopeCount;
  const accessLimitReached = Number.isFinite(maxCurrentAccessCount) && currentAccessTotal >= maxCurrentAccessCount;
  const scopeFieldDisabled = createFormLockedByTier || (noScopeSlotsRemaining && client.scopes.length === 0);
  const authorityFieldDisabled = createFormLockedByTier || (noAccessSlotsRemaining && client.authorities.length === 0);
  const roleFieldDisabled = createFormLockedByTier || (noAccessSlotsRemaining && client.roles.length === 0);

  useEffect(() => {
    let isMounted = true;

    if (!isEditMode) {
      const emptyClient = createRegisteredClient();
      setClient(emptyClient);
      setInitialClient(emptyClient);
      setIsLoadingClient(false);
      setLoadError("");
      return () => {
        isMounted = false;
      };
    }

    const hydrateClient = async () => {
      setIsLoadingClient(true);
      setLoadError("");

      try {
        const response = seededRegisteredClient ?? await fetchRegisteredClient(registeredClientId);
        if (!isMounted) {
          return;
        }

        const nextClient = deserializeRegisteredClientToForm(response);
        setClient(nextClient);
        setInitialClient(nextClient);
      } catch (error) {
        if (!isMounted) {
          return;
        }

        setLoadError(getErrorMessage(error, "Unable to load the registered client."));
      } finally {
        if (isMounted) {
          setIsLoadingClient(false);
        }
      }
    };

    hydrateClient();

    return () => {
      isMounted = false;
    };
  }, [isEditMode, registeredClientId, seededRegisteredClient]);

  function updateField<K extends RegisteredClientFieldName>(
    fieldName: K,
    value: RegisteredClientBaseFields[K]
  ) {
    setIsReady(false);
    setSubmitError("");
    setClient((prev) => ({
      ...prev,
      [fieldName]: value,
    }));
  }

  function updateTokenSetting<K extends RegisteredClientTokenSettingFieldName>(
    fieldName: K,
    value: RegisteredClientTokenSettings[K]
  ) {
    setIsReady(false);
    setSubmitError("");
    setClient((prev) => ({
      ...prev,
      tokenSettings: {
        ...prev.tokenSettings,
        [fieldName]: value,
      },
    }));
  }

  function updateClientSetting<K extends RegisteredClientClientSettingFieldName>(
    fieldName: K,
    value: RegisteredClientClientSettings[K]
  ) {
    setIsReady(false);
    setSubmitError("");
    setClient((prev) => ({
      ...prev,
      clientSettings: {
        ...prev.clientSettings,
        [fieldName]: value,
      },
    }));
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const result = validateClient(client);
    setErrors(result.errors);
    setSubmitError("");

    if (Object.keys(result.errors).length > 0) {
      setIsReady(false);
      return;
    }

    if (!isReady) {
      setIsReady(true);
      return;
    }

    setIsSubmitting(true);
    const action = isEditMode
      ? updateClient(registeredClientId, result.payload)
      : createClient(result.payload);

    action
      .then((response) => {
        const nextClientId = response?.clientId ?? "";
        const nextClientSecret = response?.clientSecret ?? "";

        if (isEditMode) {
          const nextClient = deserializeRegisteredClientToForm(response);
          setClient(nextClient);
          setInitialClient(nextClient);
          navigate("/success", {
            replace: true,
            state: {
              kicker: "Client Updated",
              title: response?.clientName || "Registered client",
              identifier: nextClientId,
              message: "The registered client has been updated and the registry cache has been refreshed.",
              redirectTo: "/",
              returnLabel: "Go to dashboard",
            },
          });
          return;
        }

        navigate("/success", {
          replace: true,
          state: {
            kicker: "Client Created",
            title: response?.clientName || "Registered client",
            identifier: nextClientId,
            detailLabel: nextClientSecret ? "Client Secret" : "",
            detailValue: nextClientSecret,
            message: nextClientSecret
              ? "Save the client ID and secret now. The secret is only shown at creation time."
              : "The registered client has been created successfully.",
            redirectTo: "/",
            returnLabel: "Go to dashboard",
          },
        });
      })
      .catch((error: unknown) => {
        setIsReady(false);
        setSubmitError(getErrorMessage(error, `Unable to ${isEditMode ? "update" : "create"} client.`));
      })
      .finally(() => {
        setIsSubmitting(false);
      });
  }

  function handleReset() {
    setClient(isEditMode ? initialClient : createRegisteredClient());
    setErrors({});
    setIsReady(false);
    setSubmitError("");
  }

  if (isLoadingClient) {
    return (
      <div className="client-empty-state">Loading registered client...</div>
    );
  }

  if (loadError) {
    return (
      <div className="client-submit-message">{loadError}</div>
    );
  }

  return (
    <form className="clients-form-layout" onSubmit={handleSubmit}>
      {createFormLockedByTier ? (
        <div className="client-submit-message client-submit-message-warning">
          Your current tier has no remaining registered client slots, so new client creation is locked until the plan is upgraded or a client is removed.
        </div>
      ) : null}

      <div className="clients-workspace">
            <section className="client-card client-card-primary">
              <div className="client-card-header">
                <div>
                  <div className="client-card-kicker">{isEditMode ? "Edit" : "Register"}</div>
                  <div className="client-card-title">
                    {isEditMode ? "Update registered client" : "Create a new client"}
                  </div>
                </div>
                <div className="client-card-caption">
                  {isEditMode
                    ? "Update sign-in settings, callback URLs, and client access with the current values already loaded."
                    : "Name the app, choose its sign-in method, and set its URLs and permissions."}
                </div>
              </div>
              <div className="client-card-contents">
                <TextInput
                  placeholder="e.g. frontend-spa"
                  label={<InfoLabel label="Client Name" info={registeredClientFieldInfo.clientName} />}
                  value={client.clientName}
                  error={errors.clientName}
                  disabled={createFormLockedByTier}
                  onChange={(value) => updateField("clientName", value)}
                />

                <TextInput
                  placeholder="e.g. https://app.example.com or http://localhost:5173"
                  label={<InfoLabel label="Client Origin" info={registeredClientFieldInfo.clientOrigin} />}
                  value={client.clientOrigin}
                  error={errors.clientOrigin}
                  disabled={createFormLockedByTier}
                  onChange={(value) => updateField("clientOrigin", value)}
                />

                <div className="client-field-stack">
                  <MultiSelect
                    placeholder="Choose auth methods"
                    data={authMethodOptions}
                    label={
                      <InfoLabel
                        label="Client Authentication Methods"
                        info={registeredClientFieldInfo.clientAuthenticationMethods}
                      />
                    }
                    classNames={CLIENT_SELECT_CLASSNAMES}
                    clearable
                    searchable
                    disabled={isEditMode || createFormLockedByTier}
                    clearButtonProps={{ className: "client-select-clear" }}
                    value={client.clientAuthenticationMethods}
                    error={errors.clientAuthenticationMethods}
                    onChange={(values) => updateField("clientAuthenticationMethods", values)}
                  />
                  {isEditMode ? (
                    <div className="client-field-inline-note client-field-inline-note-warning">
                      Updating client authentication methods is not currently supported.
                    </div>
                  ) : null}
                </div>

                <div className="client-field-stack">
                  <MultiSelect
                    placeholder="Choose grant types"
                    data={grantTypeOptions}
                    label={
                      <InfoLabel
                        label="Authorization Grant Types"
                        info={registeredClientFieldInfo.authorizationGrantTypes}
                      />
                    }
                    classNames={CLIENT_SELECT_CLASSNAMES}
                    clearable
                    searchable
                    disabled={isEditMode || createFormLockedByTier}
                    clearButtonProps={{ className: "client-select-clear" }}
                    value={client.authorizationGrantTypes}
                    error={errors.authorizationGrantTypes}
                    onChange={(values) => updateField("authorizationGrantTypes", values)}
                  />
                  {isEditMode ? (
                    <div className="client-field-inline-note client-field-inline-note-warning">
                      Updating authorization grant types is not currently supported.
                    </div>
                  ) : null}
                </div>

                <div className="client-field-full">
                  <TagsInput
                    placeholder="e.g. CLIENTS_PAID, USERS_MANAGE"
                    label={<InfoLabel label="Authorities" info={registeredClientFieldInfo.authorities} />}
                    classNames={CLIENT_SELECT_CLASSNAMES}
                    clearButtonProps={{ className: "client-select-clear" }}
                    value={client.authorities}
                    disabled={authorityFieldDisabled}
                    onChange={(values) => updateField("authorities", trimToLimit(values, maxAuthorityCount))}
                  />
                  <div className="client-field-help">
                    Add authority names like CLIENTS_PAID or USERS_MANAGE.
                  </div>
                  {!createFormLockedByTier && accessLimitReached ? (
                    <div className="client-field-inline-note client-field-inline-note-warning">
                      No remaining access slots in your current tier. Roles and authorities share this allowance.
                    </div>
                  ) : null}
                </div>

                <div className="client-field-full">
                  <TagsInput
                    placeholder="e.g. PLATFORM_USER, CLIENT_ADMIN"
                    label={<InfoLabel label="Roles" info={registeredClientFieldInfo.roles} />}
                    classNames={CLIENT_SELECT_CLASSNAMES}
                    clearButtonProps={{ className: "client-select-clear" }}
                    value={client.roles}
                    disabled={roleFieldDisabled}
                    onChange={(values) => updateField("roles", trimToLimit(values, maxRoleCount, sanitizeRoleValues))}
                  />
                  <div className="client-field-help">
                    Roles are stored with a <code>ROLE_</code> prefix.
                  </div>
                  {!createFormLockedByTier && accessLimitReached ? (
                    <div className="client-field-inline-note client-field-inline-note-warning">
                      You have reached the maximum number of role/authority entries your tier allows. Remove one to add another.
                    </div>
                  ) : null}
                </div>

                <div className="client-field-full">
                  <TagsInput
                    placeholder="e.g. users:write, users:read"
                    label={<InfoLabel label="Registered Client Allowed Scopes" info={registeredClientFieldInfo.scopes} />}
                    classNames={CLIENT_SELECT_CLASSNAMES}
                    clearButtonProps={{ className: "client-select-clear" }}
                    value={client.scopes}
                    error={errors.scopes}
                    disabled={scopeFieldDisabled}
                    onChange={(values) => updateField("scopes", trimToLimit(values, maxCurrentScopeCount))}
                  />
                  <div className="client-field-help">
                    Add the permissions this app should be allowed to ask for.
                  </div>
                  {!createFormLockedByTier && scopeLimitReached ? (
                    <div className="client-field-inline-note client-field-inline-note-warning">
                      You have reached the maximum number of scopes your tier allows. Remove one to add another.
                    </div>
                  ) : null}
                </div>

                <div className="client-field-full">
                  <TagsInput
                    placeholder="e.g. /callback"
                    label={<InfoLabel label="Redirect URIs" info={registeredClientFieldInfo.redirectUris} />}
                    classNames={CLIENT_SELECT_CLASSNAMES}
                    clearButtonProps={{ className: "client-select-clear" }}
                    value={client.redirectUris}
                    error={errors.redirectUris}
                    disabled={createFormLockedByTier}
                    onChange={(values) => updateField("redirectUris", sanitizeTagValues(values))}
                  />
                  <div className="client-field-help">
                    Add full URLs or paths like /callback. Paths will be combined with the base URL.
                  </div>
                </div>

                <div className="client-field-full">
                  <TagsInput
                    placeholder="e.g. /logout-callback"
                    label={<InfoLabel label="Post Logout Redirect URIs" info={registeredClientFieldInfo.postLogoutRedirectUris} />}
                    classNames={CLIENT_SELECT_CLASSNAMES}
                    clearButtonProps={{ className: "client-select-clear" }}
                    value={client.postLogoutRedirectUris}
                    error={errors.postLogoutRedirectUris}
                    disabled={createFormLockedByTier}
                    onChange={(values) => updateField("postLogoutRedirectUris", sanitizeTagValues(values))}
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
                    Choose sign-in rules and token lifetimes.
                  </div>
                </div>
                <div className="client-card-contents">
                  {clientSettingsFields.map((setting) => (
                    <label
                      className={`client-checkbox${createFormLockedByTier ? " is-disabled" : ""}`}
                      key={setting.name}
                    >
                      <Checkbox
                      checked={client.clientSettings[setting.name]}
                        disabled={createFormLockedByTier}
                        onChange={(event) => updateClientSetting(setting.name, event.currentTarget.checked)}
                      />
                      <div>
                        <div className="client-checkbox-label">
                          <InfoLabel label={setting.label} info={registeredClientFieldInfo[setting.name]} />
                        </div>
                        <div className="client-checkbox-description">{setting.description}</div>
                        {errors[setting.name] ? <div className="client-checkbox-error">{errors[setting.name]}</div> : null}
                      </div>
                    </label>
                  ))}

                  {tokenSettingsFields.map((field) => (
                    <TextInput
                      key={field.name}
                      placeholder={field.placeholder}
                      label={<InfoLabel label={field.label} info={registeredClientFieldInfo[field.name]} />}
                      type="number"
                      inputMode="numeric"
                      min={field.min}
                      step="1"
                      value={client.tokenSettings[field.name]}
                      error={errors[field.name]}
                      disabled={createFormLockedByTier}
                      onChange={(value) => updateTokenSetting(field.name, value)}
                    />
                  ))}

                  <label className={`client-checkbox${createFormLockedByTier ? " is-disabled" : ""}`}>
                    <Checkbox
                      checked={client.tokenSettings.reuseRefreshTokens}
                      disabled={createFormLockedByTier}
                      onChange={(event) => updateTokenSetting("reuseRefreshTokens", event.currentTarget.checked)}
                    />
                    <div>
                      <div className="client-checkbox-label">
                        <InfoLabel label="Reuse refresh tokens" info={registeredClientFieldInfo.reuseRefreshTokens} />
                      </div>
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
        <Button type="submit" disabled={createFormLockedByTier}>
          {isSubmitting
            ? (isEditMode ? "Updating..." : "Sending...")
            : isReady
              ? (isEditMode ? "Save client" : "Submit client")
              : "Check details"}
        </Button>
      </div>

      {submitError ? <div className="client-submit-message">{submitError}</div> : null}

    </form>
  );
}
