import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  fetchDashboard,
  fetchRegisteredClientUsers,
} from "../services/platform";

function sortClientUsers(users) {
  return [...users].sort((left, right) => (left.email || "").localeCompare(right.email || ""));
}

function formatList(values = []) {
  return values.length > 0 ? values.join(", ") : "None";
}

function buildClientUserRouteState(registeredClient, user) {
  return {
    registeredClient: {
      id: registeredClient?.id ?? null,
      clientId: registeredClient?.clientId ?? "",
      clientName: registeredClient?.clientName ?? "",
      scopes: registeredClient?.scopes ?? [],
      authorities: registeredClient?.authorities ?? [],
      roles: registeredClient?.roles ?? [],
    },
    clientUser: {
      ...user,
      authorities: user?.authorities ?? [],
      roles: user?.roles ?? [],
    },
  };
}

function buildClientEditRouteState(registeredClient) {
  return {
    registeredClient: {
      ...registeredClient,
      id: registeredClient?.id ?? null,
      clientId: registeredClient?.clientId ?? "",
      clientName: registeredClient?.clientName ?? "",
      clientAuthenticationMethods: registeredClient?.clientAuthenticationMethods ?? [],
      authorizationGrantTypes: registeredClient?.authorizationGrantTypes ?? [],
      redirectUris: registeredClient?.redirectUris ?? [],
      postLogoutRedirectUris: registeredClient?.postLogoutRedirectUris ?? [],
      scopes: registeredClient?.scopes ?? [],
      authorities: registeredClient?.authorities ?? [],
      roles: registeredClient?.roles ?? [],
      clientSettings: registeredClient?.clientSettings ?? {},
      tokenSettings: registeredClient?.tokenSettings ?? {},
    },
  };
}

function isClientUserActive(user) {
  if (!user) {
    return false;
  }
  return !user.locked && !user.expired && !user.credentialsExpired;
}

export default function ClientRegistrySection({ refreshKey = 0 }) {
  const [registeredClients, setRegisteredClients] = useState([]);
  const [clientsError, setClientsError] = useState("");
  const [isLoadingClients, setIsLoadingClients] = useState(true);
  const [dashboardTotals, setDashboardTotals] = useState({
    totalUsers: 0,
    totalScopes: 0,
    totalAuthorities: 0,
  });
  const [expandedClientIds, setExpandedClientIds] = useState([]);
  const [clientUsersById, setClientUsersById] = useState({});
  const [clientUsersStatus, setClientUsersStatus] = useState({});

  useEffect(() => {
    let isMounted = true;
    setIsLoadingClients(true);

    fetchDashboard()
      .then((dashboardResponse) => {
        if (!isMounted) {
          return;
        }

        const clients = dashboardResponse.registeredClientResponses;
        setRegisteredClients(clients);
        setDashboardTotals({
          totalUsers: dashboardResponse.totalUsers,
          totalScopes: dashboardResponse.totalScopes,
          totalAuthorities: dashboardResponse.totalAuthorities,
        });
        setClientsError("");
      })
      .catch((error) => {
        if (!isMounted) {
          return;
        }

        setRegisteredClients([]);
        setDashboardTotals({
          totalUsers: 0,
          totalScopes: 0,
          totalAuthorities: 0,
        });
        setClientsError(error.message || "Unable to load registered clients.");
      })
      .finally(() => {
        if (isMounted) {
          setIsLoadingClients(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [refreshKey]);

  const registrySummary = useMemo(() => {
    return {
      totalClients: registeredClients.length,
      totalUsers: dashboardTotals.totalUsers,
      totalScopes: dashboardTotals.totalScopes,
      totalAuthorities: dashboardTotals.totalAuthorities,
    };
  }, [dashboardTotals, registeredClients]);

  const toggleClientUsers = async (registeredClientId) => {
    const isExpanded = expandedClientIds.includes(registeredClientId);
    if (isExpanded) {
      setExpandedClientIds((prev) => prev.filter((id) => id !== registeredClientId));
      return;
    }

    if (!registeredClientId) {
      setClientUsersStatus((prev) => ({
        ...prev,
        [registeredClientId ?? "unknown"]: "Registered client is missing an id.",
      }));
      return;
    }

    setExpandedClientIds((prev) => [...prev, registeredClientId]);

    if (clientUsersById[registeredClientId] || clientUsersStatus[registeredClientId] === "loading") {
      return;
    }

    setClientUsersStatus((prev) => ({ ...prev, [registeredClientId]: "loading" }));
    try {
      const response = await fetchRegisteredClientUsers([registeredClientId]);
      setClientUsersById((prev) => ({
        ...prev,
        [registeredClientId]: sortClientUsers(response),
      }));
      setClientUsersStatus((prev) => ({ ...prev, [registeredClientId]: "ready" }));
    } catch (error) {
      setClientUsersStatus((prev) => ({
        ...prev,
        [registeredClientId]: error.message || "Unable to load users.",
      }));
    }
  };

  return (
    <section className="client-card client-card-primary client-card-registry">
      <div className="client-card-header">
        <div>
          <div className="client-card-kicker">Registry</div>
          <div className="client-card-title">Registered clients</div>
        </div>
        <div className="client-card-caption">
          Expand a client row to inspect attached users and jump into user-level token details.
        </div>
      </div>
      <div className="client-card-contents client-registry-contents">
        <div className="client-summary-grid">
          <div className="client-summary-tile">
            <div className="client-summary-label">Clients</div>
            <div className="client-summary-value">
              {isLoadingClients ? "Loading..." : registrySummary.totalClients}
            </div>
          </div>
          <div className="client-summary-tile">
            <div className="client-summary-label">Total Users</div>
            <div className="client-summary-value">
              {registrySummary.totalUsers == null ? "Loading..." : registrySummary.totalUsers}
            </div>
          </div>
          <div className="client-summary-tile">
            <div className="client-summary-label">Total Scopes</div>
            <div className="client-summary-value">
              {isLoadingClients ? "Loading..." : registrySummary.totalScopes}
            </div>
          </div>
          <div className="client-summary-tile">
            <div className="client-summary-label">Authorities</div>
            <div className="client-summary-value">
              {isLoadingClients ? "Loading..." : registrySummary.totalAuthorities}
            </div>
          </div>
        </div>

        {clientsError ? <div className="client-submit-message">{clientsError}</div> : null}

        {isLoadingClients ? (
          <div className="client-empty-state">Loading registered clients...</div>
        ) : registeredClients.length === 0 ? (
          <div className="client-empty-state">No clients are registered to this account yet.</div>
        ) : (
          <div className="client-registry-list">
            {registeredClients.map((registeredClient) => {
              const isExpanded = expandedClientIds.includes(registeredClient.id);
              const usersStatus = clientUsersStatus[registeredClient.id];
              const attachedUsers = clientUsersById[registeredClient.id] || [];
              const authorityCount = registeredClient.authorities.length;

              return (
                <article className="client-registry-item" key={registeredClient.id}>
                  <div className="client-registry-row">
                    <div className="client-registry-main">
                      <div className="client-registry-kicker">Client name</div>
                      <div className="client-registry-name">
                        {registeredClient.clientName || "Unnamed client"}
                      </div>
                      <div className="client-registry-id">{registeredClient.clientId || "No client ID available"}</div>
                      <div className="client-registry-meta">
                        <span>Auth: {formatList(registeredClient.clientAuthenticationMethods)}</span>
                        <span>Grants: {formatList(registeredClient.authorizationGrantTypes)}</span>
                        <span>Scopes: {registeredClient.scopes.length}</span>
                        <span>Authorities: {authorityCount}</span>
                      </div>
                    </div>

                    <div className="client-registry-actions">
                      {registeredClient.id ? (
                        <>
                          <Link
                            className="client-registry-action client-registry-action-edit"
                            to={`/clients/${registeredClient.id}/edit`}
                            state={buildClientEditRouteState(registeredClient)}
                          >
                            Edit
                          </Link>
                          <button
                            type="button"
                            className={`client-registry-action client-registry-action-users${isExpanded ? " is-active" : ""}`}
                            onClick={() => toggleClientUsers(registeredClient.id)}
                          >
                            {isExpanded ? "Hide users" : "View users"}
                          </button>
                        </>
                      ) : (
                        <span className="client-registry-action-muted">Client unavailable</span>
                      )}
                    </div>
                  </div>

                  {isExpanded ? (
                    <div className="client-users-panel">
                      {usersStatus === "loading" ? (
                        <div className="client-users-empty">Loading attached users...</div>
                      ) : typeof usersStatus === "string" && usersStatus !== "ready" ? (
                        <div className="client-users-empty">{usersStatus}</div>
                      ) : attachedUsers.length === 0 ? (
                        <div className="client-users-empty">No users are attached to this client yet.</div>
                      ) : (
                        <div className="client-users-list">
                          {attachedUsers.map((user) => (
                            <Link
                              className="client-user-link"
                              key={user.id}
                              to={`/clients/${registeredClient.id}/users/${user.id}`}
                              state={buildClientUserRouteState(registeredClient, user)}
                            >
                              <div>
                                <div className="client-user-email">
                                  <span
                                    className={`client-user-status-dot${isClientUserActive(user) ? " is-active" : ""}`}
                                    aria-hidden="true"
                                  />
                                  <span>{user.email}</span>
                                </div>
                                <div className="client-user-meta">
                                  {user.verified ? "Verified" : "Unverified"} · {formatList(user.authorities)}
                                </div>
                              </div>
                              <span className="client-user-open">Open</span>
                            </Link>
                          ))}
                        </div>
                      )}
                    </div>
                  ) : null}
                </article>
              );
            })}
          </div>
        )}
      </div>
    </section>
  );
}
