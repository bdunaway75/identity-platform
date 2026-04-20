import { useEffect, useMemo, useState } from "react";
import { MultiSelect } from "@mantine/core";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import Button from "../components/Button";
import TextInput from "../components/TextInput";
import {
  fetchRegisteredClient,
  fetchRegisteredClientTokens,
  fetchRegisteredClientUsers,
  invalidateRegisteredClientToken,
  updateRegisteredClientUser,
} from "../services/platform";
import "./Clients.css";
import "./ClientUserDetail.css";

function normalizeArray(value) {
  return Array.isArray(value) ? value : [];
}

function sanitizeTagValues(values) {
  return [...new Set(normalizeArray(values).map((value) => String(value ?? "").trim()).filter(Boolean))];
}

function sanitizeRoleValues(values) {
  return sanitizeTagValues(values).map((value) =>
    value.startsWith("ROLE_") ? value : `ROLE_${value}`
  );
}

const CLIENT_SELECT_CLASSNAMES = {
  input: "client-select-input",
  section: "client-select-section",
  dropdown: "client-select-dropdown",
  option: "client-select-option",
  pill: "client-select-pill",
  pillsList: "client-select-pills-list",
  inputField: "client-select-input-field",
};

function sortTokens(tokens) {
  return [...tokens].sort((left, right) => String(right.issuedAt || "").localeCompare(String(left.issuedAt || "")));
}

function formatTimestamp(value) {
  if (!value) {
    return "N/A";
  }

  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "N/A" : date.toLocaleString();
}

function isClientUserActive(user) {
  if (!user || typeof user !== "object") {
    return false;
  }

  if (typeof user.active === "boolean") {
    return user.active;
  }

  return !user.locked && !user.expired && !user.credentialsExpired;
}

function getClientUserStateLabel(user) {
  if (isClientUserActive(user)) {
    return "Active";
  }

  if (user?.locked) {
    return "Locked";
  }

  if (user?.credentialsExpired) {
    return "Credentials expired";
  }

  if (user?.expired) {
    return "Expired";
  }

  return "Needs attention";
}

export default function ClientUserDetail() {
  const { registeredClientId, clientUserId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [registeredClient, setRegisteredClient] = useState(null);
  const [clientUser, setClientUser] = useState(null);
  const [tokens, setTokens] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [saveState, setSaveState] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [isRefreshingTokens, setIsRefreshingTokens] = useState(false);
  const [formState, setFormState] = useState({
    email: "",
    verified: false,
    locked: false,
    expired: false,
    credentialsExpired: false,
    authorities: [],
    roles: [],
  });
  const seededRegisteredClient = useMemo(() => {
    const fromRouteState = location.state?.registeredClient;
    if (!fromRouteState) {
      return null;
    }
    return String(fromRouteState.id ?? "") === String(registeredClientId ?? "")
      ? fromRouteState
      : null;
  }, [location.state, registeredClientId]);
  const seededClientUser = useMemo(() => {
    const fromRouteState = location.state?.clientUser;
    if (!fromRouteState) {
      return null;
    }
    return String(fromRouteState.id ?? "") === String(clientUserId ?? "")
      ? fromRouteState
      : null;
  }, [location.state, clientUserId]);

  const loadPage = async ({ refreshTokensOnly = false } = {}) => {
    if (!registeredClientId || !clientUserId) {
      return;
    }

    if (refreshTokensOnly) {
      setIsRefreshingTokens(true);
    } else {
      if (seededRegisteredClient) {
        setRegisteredClient(seededRegisteredClient);
      }
      if (seededClientUser) {
        setClientUser(seededClientUser);
      }
      setIsLoading(true);
    }

    try {
      if (refreshTokensOnly) {
        const tokensResponse = await fetchRegisteredClientTokens([registeredClientId]);
        const targetEmail = String(clientUser?.email ?? seededClientUser?.email ?? "").trim();
        setTokens(
          sortTokens(tokensResponse).filter(
            (token) => !targetEmail || token.subject === targetEmail
          )
        );
        setError("");
        return;
      }

      const [clientResponse, usersResponse, tokensResponse] = await Promise.all([
        seededRegisteredClient
          ? Promise.resolve(seededRegisteredClient)
          : fetchRegisteredClient(registeredClientId),
        fetchRegisteredClientUsers([registeredClientId]),
        fetchRegisteredClientTokens([registeredClientId]),
      ]);

      const fetchedUser = usersResponse.find((user) => user?.id === clientUserId);
      const resolvedUser = fetchedUser ?? seededClientUser;
      const targetEmail = String(resolvedUser?.email ?? "").trim();

      setRegisteredClient(clientResponse ?? seededRegisteredClient ?? null);
      setClientUser(resolvedUser ?? null);
      setTokens(
        sortTokens(tokensResponse).filter(
          (token) => !targetEmail || token.subject === targetEmail
        )
      );
      setError("");

      if (resolvedUser) {
        setFormState({
          email: resolvedUser.email || "",
          verified: Boolean(resolvedUser.verified),
          locked: Boolean(resolvedUser.locked),
          expired: Boolean(resolvedUser.expired),
          credentialsExpired: Boolean(resolvedUser.credentialsExpired),
          authorities: sanitizeTagValues(resolvedUser.authorities),
          roles: sanitizeRoleValues(resolvedUser.roles),
        });
      }
    } catch (loadError) {
      setError(loadError.message || "Unable to load the client user details.");
    } finally {
      setIsLoading(false);
      setIsRefreshingTokens(false);
    }
  };

  useEffect(() => {
    loadPage();
  }, [registeredClientId, clientUserId]);

  const activeTokenCount = useMemo(
    () => tokens.filter((token) => !token.revokedAt).length,
    [tokens]
  );
  const userStateLabel = getClientUserStateLabel(clientUser);
  const authorityOptions = useMemo(
    () =>
      normalizeArray(registeredClient?.authorities).map((value) => ({
        value,
        label: value,
      })),
    [registeredClient]
  );
  const roleOptions = useMemo(
    () =>
      normalizeArray(registeredClient?.roles).map((value) => ({
        value,
        label: value,
      })),
    [registeredClient]
  );

  const handleFieldChange = (fieldName, value) => {
    setSaveState("");
    setFormState((prev) => ({
      ...prev,
      [fieldName]: value,
    }));
  };

  const handleSave = async () => {
    setIsSaving(true);
    setSaveState("");

    try {
      const normalizedAuthorities = sanitizeTagValues(formState.authorities);
      const normalizedRoles = sanitizeRoleValues(formState.roles);

      const updatedUser = await updateRegisteredClientUser(clientUserId, {
        email: formState.email.trim(),
        verified: formState.verified,
        locked: formState.locked,
        expired: formState.expired,
        credentialsExpired: formState.credentialsExpired,
        authorities: sanitizeTagValues([
          ...normalizedAuthorities,
          ...normalizedRoles,
        ]),
      });

      setClientUser(updatedUser);
      setFormState({
        email: updatedUser?.email || "",
        verified: Boolean(updatedUser?.verified),
        locked: Boolean(updatedUser?.locked),
        expired: Boolean(updatedUser?.expired),
        credentialsExpired: Boolean(updatedUser?.credentialsExpired),
        authorities: sanitizeTagValues(updatedUser?.authorities),
        roles: sanitizeRoleValues(updatedUser?.roles),
      });
      navigate("/success", {
        replace: true,
        state: {
          kicker: "User Updated",
          title: updatedUser?.email || "Client user",
          identifier: registeredClient?.clientName || registeredClient?.clientId || "",
          message: "The client user has been updated successfully.",
          redirectTo: "/",
          returnLabel: "Go to dashboard",
        },
      });
    } catch (saveError) {
      setSaveState(saveError.message || "Unable to save client user changes.");
    } finally {
      setIsSaving(false);
    }
  };

  const handleInvalidateToken = async (authTokenId) => {
    try {
      await invalidateRegisteredClientToken(authTokenId);
      navigate("/success", {
        replace: true,
        state: {
          kicker: "Token Invalidated",
          title: clientUser?.email || "Client user token",
          identifier: authTokenId,
          message: "The selected token was invalidated successfully.",
          redirectTo: "/",
          returnLabel: "Go to dashboard",
        },
      });
    } catch (tokenError) {
      setSaveState(tokenError.message || "Unable to invalidate token.");
    }
  };

  if (isLoading) {
    return (
      <div className="client-user-page">
        <div className="client-user-empty">Loading client user details...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="client-user-page">
        <div className="client-user-empty">{error}</div>
      </div>
    );
  }

  if (!clientUser) {
    return (
      <div className="client-user-page">
        <div className="client-user-empty">That client user could not be found.</div>
      </div>
    );
  }

  return (
    <div className="client-user-page">
      <div className="client-user-header">
        <div className="client-user-hero">
          <div className="client-user-kicker">Client User</div>
          <div className="client-user-hero-main">
            <div className="client-user-title-row">
              <h1>{clientUser.email}</h1>
              <span className={`client-user-status-pill${isClientUserActive(clientUser) ? " is-active" : ""}`}>
                <span className="client-user-status-pill-dot" aria-hidden="true" />
                <span>{userStateLabel}</span>
              </span>
            </div>
            <p>
              Attached to <strong>{registeredClient?.clientName || registeredClient?.clientId}</strong>. Review account status, update roles and authorities, and check token activity here.
            </p>
          </div>
          <div className="client-user-summary-strip">
            <div className="client-user-summary-chip">
              <span className="client-user-summary-label">Authorities</span>
              <span className="client-user-summary-value">{normalizeArray(clientUser.authorities).length}</span>
            </div>
            <div className="client-user-summary-chip">
              <span className="client-user-summary-label">Roles</span>
              <span className="client-user-summary-value">{normalizeArray(clientUser.roles).length}</span>
            </div>
            <div className="client-user-summary-chip">
              <span className="client-user-summary-label">Active Tokens</span>
              <span className="client-user-summary-value">{activeTokenCount}</span>
            </div>
            <div className="client-user-summary-chip">
              <span className="client-user-summary-label">Total Tokens</span>
              <span className="client-user-summary-value">{tokens.length}</span>
            </div>
          </div>
        </div>
      </div>

      <div className="client-user-layout">
        <section className="client-user-card">
          <div className="client-user-card-header">
            <div>
              <div className="client-user-kicker">Edit</div>
              <div className="client-user-card-title">User settings</div>
            </div>
          </div>
          <div className="client-user-card-body">
            <TextInput
              label="Email"
              value={formState.email}
              onChange={(value) => handleFieldChange("email", value)}
            />

            <div className="client-user-authority-editor">
              <div className="client-user-authority-header">
                <div className="client-user-authority-title">Authorities</div>
                <div className="client-user-authority-caption">
                  Assign authorities from the registered client catalog.
                </div>
              </div>

              {authorityOptions.length === 0 ? (
                <div className="client-user-authority-empty">
                  No authorities are defined for this registered client yet.
                </div>
              ) : (
                <MultiSelect
                  placeholder="Select authorities"
                  data={authorityOptions}
                  classNames={CLIENT_SELECT_CLASSNAMES}
                  clearable
                  searchable
                  clearButtonProps={{ className: "client-select-clear" }}
                  value={formState.authorities}
                  onChange={(values) => handleFieldChange("authorities", sanitizeTagValues(values))}
                />
              )}
            </div>

            <div className="client-user-authority-editor">
              <div className="client-user-authority-header">
                <div className="client-user-authority-title">Roles</div>
                <div className="client-user-authority-caption">
                  Assign roles from the registered client catalog.
                </div>
              </div>

              {roleOptions.length === 0 ? (
                <div className="client-user-authority-empty">
                  No roles are defined for this registered client yet.
                </div>
              ) : (
                <MultiSelect
                  placeholder="Select roles"
                  data={roleOptions}
                  classNames={CLIENT_SELECT_CLASSNAMES}
                  clearable
                  searchable
                  clearButtonProps={{ className: "client-select-clear" }}
                  value={formState.roles}
                  onChange={(values) => handleFieldChange("roles", sanitizeRoleValues(values))}
                />
              )}
            </div>

            <div className="client-user-toggle-grid">
              <label className={`client-user-toggle${formState.verified ? " is-selected" : ""}`}>
                <input
                  type="checkbox"
                  checked={formState.verified}
                  onChange={(event) => handleFieldChange("verified", event.currentTarget.checked)}
                />
                <span className="client-user-toggle-switch" aria-hidden="true" />
                <span className="client-user-toggle-copy">
                  <span className="client-user-toggle-title">Verified</span>
                  <span className="client-user-toggle-description">Marks the account as verified.</span>
                </span>
              </label>

              <label className={`client-user-toggle${formState.locked ? " is-selected" : ""}`}>
                <input
                  type="checkbox"
                  checked={formState.locked}
                  onChange={(event) => handleFieldChange("locked", event.currentTarget.checked)}
                />
                <span className="client-user-toggle-switch" aria-hidden="true" />
                <span className="client-user-toggle-copy">
                  <span className="client-user-toggle-title">Locked</span>
                  <span className="client-user-toggle-description">Prevents the user from signing in until unlocked.</span>
                </span>
              </label>

              <label className={`client-user-toggle${formState.expired ? " is-selected" : ""}`}>
                <input
                  type="checkbox"
                  checked={formState.expired}
                  onChange={(event) => handleFieldChange("expired", event.currentTarget.checked)}
                />
                <span className="client-user-toggle-switch" aria-hidden="true" />
                <span className="client-user-toggle-copy">
                  <span className="client-user-toggle-title">Expired</span>
                  <span className="client-user-toggle-description">Flags the account as no longer valid for use.</span>
                </span>
              </label>

              <label className={`client-user-toggle${formState.credentialsExpired ? " is-selected" : ""}`}>
                <input
                  type="checkbox"
                  checked={formState.credentialsExpired}
                  onChange={(event) => handleFieldChange("credentialsExpired", event.currentTarget.checked)}
                />
                <span className="client-user-toggle-switch" aria-hidden="true" />
                <span className="client-user-toggle-copy">
                  <span className="client-user-toggle-title">Credentials expired</span>
                  <span className="client-user-toggle-description">Requires the user to refresh or rotate credentials.</span>
                </span>
              </label>
            </div>

            {saveState ? <div className="client-user-save-state">{saveState}</div> : null}

            <div className="client-user-actions">
              <Button type="button" onClick={handleSave}>
                {isSaving ? "Saving..." : "Save changes"}
              </Button>
            </div>
          </div>
        </section>

        <section className="client-user-card">
          <div className="client-user-card-header">
            <div>
              <div className="client-user-kicker">Tokens</div>
              <div className="client-user-card-title">Issued tokens</div>
            </div>
            <div className="client-user-card-caption">
              Active: {activeTokenCount} · Total: {tokens.length}
            </div>
          </div>
          <div className="client-user-card-body">
            {isRefreshingTokens ? <div className="client-user-empty">Refreshing tokens...</div> : null}

            {tokens.length === 0 ? (
              <div className="client-user-empty">No tokens have been issued for this user yet.</div>
            ) : (
              <div className="client-token-list">
                {tokens.map((token) => (
                  <article className="client-token-item" key={token.id}>
                    <div className="client-token-top">
                      <div>
                        <div className="client-token-type">{token.tokenType}</div>
                        <div className="client-token-subject">{token.subject}</div>
                      </div>
                      {!token.revokedAt ? (
                        <Button type="button" variant="secondary" onClick={() => handleInvalidateToken(token.id)}>
                          Invalidate
                        </Button>
                      ) : (
                        <span className="client-token-revoked">Revoked</span>
                      )}
                    </div>
                    <div className="client-token-meta">
                      <span>Issued: {formatTimestamp(token.issuedAt)}</span>
                      <span>Expires: {formatTimestamp(token.expiresAt)}</span>
                      <span>Revoked: {formatTimestamp(token.revokedAt)}</span>
                      <span>KID: {token.kid || "N/A"}</span>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}
