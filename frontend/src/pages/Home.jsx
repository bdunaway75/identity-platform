import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useSubscription } from "../context/SubscriptionContext";
import {
  fetchDashboard,
  fetchRecentUserActivity,
  fetchRegisteredClientUsers,
} from "../services/platform";
import "./Clients.css";
import "./Home.css";

function normalizeArray(value) {
  return Array.isArray(value) ? value : [];
}

function clampPercentage(value) {
  if (!Number.isFinite(value)) {
    return 0;
  }

  return Math.max(0, Math.min(100, value));
}

function formatLimitCaption(used, limit, noun) {
  if (limit <= 0) {
    return `No ${noun} slots available on this plan.`;
  }

  const remaining = Math.max(limit - used, 0);
  return `${remaining} ${noun} slot${remaining === 1 ? "" : "s"} remaining`;
}

function formatTimestamp(value) {
  if (!value) {
    return "N/A";
  }

  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "N/A" : date.toLocaleString();
}

function buildClientUserRouteState(activityUser) {
  if (!activityUser?.registeredClient || !activityUser?.clientUser) {
    return undefined;
  }

  return {
    registeredClient: {
      id: activityUser.registeredClient.id ?? null,
      clientId: activityUser.registeredClient.clientId ?? "",
      clientName: activityUser.registeredClient.clientName ?? "",
      scopes: normalizeArray(activityUser.registeredClient.scopes),
      authorities: normalizeArray(activityUser.registeredClient.authorities),
      roles: normalizeArray(activityUser.registeredClient.roles),
    },
    clientUser: {
      ...activityUser.clientUser,
      authorities: normalizeArray(activityUser.clientUser.authorities),
      roles: normalizeArray(activityUser.clientUser.roles),
    },
  };
}

export default function Home() {
  const {
    tierName: subscriptionTierName,
    limits,
    status,
    isPaid,
  } = useSubscription();
  const [isLoading, setIsLoading] = useState(true);
  const [totalUsers, setTotalUsers] = useState(0);
  const [totalClients, setTotalClients] = useState(0);
  const [totalScopes, setTotalScopes] = useState(0);
  const [totalAuthorities, setTotalAuthorities] = useState(0);
  const [recentUsers, setRecentUsers] = useState([]);
  const [recentActiveUsers, setRecentActiveUsers] = useState([]);
  const clientLimit = Number(limits?.registeredClients || 0);
  const userLimit = Number(limits?.globalUsers || 0);
  const scopeLimit = Number(limits?.globalScopes || 0);
  const authorityLimit = Number(limits?.globalAuthorities || 0);

  useEffect(() => {
    let isMounted = true;
    setIsLoading(true);

    Promise.all([fetchDashboard().catch(() => null)])
      .then(async ([dashboardResponse]) => {
        if (!isMounted) {
          return;
        }

        const normalizedClients = dashboardResponse?.registeredClientResponses ?? [];
        const effectiveClientIds = dashboardResponse?.clientIds ?? [];

        const totalUsers = dashboardResponse?.totalUsers ?? 0;
        const totalClients = dashboardResponse?.totalRegisteredClients ?? 0;
        const totalScopes = dashboardResponse?.totalScopes ?? 0;
        const totalAuthorities = dashboardResponse?.totalAuthorities ?? 0;
        const hasUsers = totalUsers > 0;

        const usersResponse = normalizedClients.length
          ? await fetchRegisteredClientUsers(normalizedClients.map((client) => client.id).filter(Boolean)).catch(() => [])
          : [];
        const activityResponse = hasUsers && effectiveClientIds.length
          ? await fetchRecentUserActivity(effectiveClientIds).catch(() => ({ logins: [], signups: [] }))
          : { logins: [], signups: [] };
        const normalizedUsers = usersResponse;
        const usersByEmail = new Map();
        for (const user of normalizedUsers) {
          const email = user.email;
          if (!email) {
            continue;
          }
          const matchingClient = normalizedClients.find((client) => client.clientId === user.clientId);
          const hydratedUser = {
            ...user,
            registeredClientId: matchingClient?.id ?? null,
            registeredClient: matchingClient
              ? {
                  id: matchingClient.id ?? null,
                  clientId: matchingClient.clientId ?? "",
                  clientName: matchingClient.clientName ?? "",
                  scopes: normalizeArray(matchingClient.scopes),
                  authorities: normalizeArray(matchingClient.authorities),
                  roles: normalizeArray(matchingClient.roles),
                }
              : null,
            clientUser: {
              ...user,
              authorities: normalizeArray(user?.authorities),
              roles: normalizeArray(user?.roles),
            },
          };
          if (!usersByEmail.has(email) || String(user?.createdAt ?? "") > String(usersByEmail.get(email)?.createdAt ?? "")) {
            usersByEmail.set(email, hydratedUser);
          }
        }

        const recentSignups = activityResponse.signups
          .map((item, index) => {
            const email = item.email;
            if (!email) {
              return null;
            }
            const matchingUser = usersByEmail.get(email);
            return {
              key: `signup-${email}-${String(item.activityTs ?? "")}-${index}`,
              id: matchingUser?.id ?? null,
              registeredClientId: matchingUser?.registeredClientId ?? null,
              email,
              activityTs: item.activityTs ?? matchingUser?.createdAt ?? null,
              registeredClient: matchingUser?.registeredClient ?? null,
              clientUser: matchingUser?.clientUser ?? null,
            };
          })
          .filter(Boolean)
          .sort((left, right) => String(right?.activityTs || "").localeCompare(String(left?.activityTs || "")));

        const activeUsers = activityResponse.logins
          .map((item, index) => {
            const email = item.email;
            if (!email) {
              return null;
            }
            const matchingUser = usersByEmail.get(email);
            return {
              key: `login-${email}-${String(item.activityTs ?? "")}-${index}`,
              id: matchingUser?.id ?? null,
              registeredClientId: matchingUser?.registeredClientId ?? null,
              email,
              issuedAt: item.activityTs ?? null,
              registeredClient: matchingUser?.registeredClient ?? null,
              clientUser: matchingUser?.clientUser ?? null,
            };
          })
          .filter(Boolean)
          .sort((left, right) => String(right?.issuedAt || "").localeCompare(String(left?.issuedAt || "")));

        if (!isMounted) {
          return;
        }

        setTotalUsers(totalUsers);
        setTotalClients(totalClients);
        setTotalScopes(totalScopes);
        setTotalAuthorities(totalAuthorities);
        setRecentUsers(recentSignups);
        setRecentActiveUsers(activeUsers);
      })
      .finally(() => {
        if (isMounted) {
          setIsLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, []);

  const dashboardSummary = useMemo(() => {
    const clientUsage = clientLimit > 0 ? totalClients / clientLimit : 0;
    const userUsage = userLimit > 0 ? totalUsers / userLimit : 0;

    return {
      clientUsagePercent: clampPercentage(clientUsage * 100),
      userUsagePercent: clampPercentage(userUsage * 100),
      scopeUsagePercent: clampPercentage((scopeLimit > 0 ? totalScopes / scopeLimit : 0) * 100),
      authorityUsagePercent: clampPercentage((authorityLimit > 0 ? totalAuthorities / authorityLimit : 0) * 100),
      clientLimitCaption: formatLimitCaption(totalClients, clientLimit, "client"),
      userLimitCaption: formatLimitCaption(totalUsers, userLimit, "user"),
      scopeLimitCaption: formatLimitCaption(totalScopes, scopeLimit, "scope"),
      authorityLimitCaption: formatLimitCaption(totalAuthorities, authorityLimit, "authority"),
      clientsMaxed: clientLimit > 0 && totalClients >= clientLimit,
      usersMaxed: userLimit > 0 && totalUsers >= userLimit,
      scopesMaxed: scopeLimit > 0 && totalScopes >= scopeLimit,
      authoritiesMaxed: authorityLimit > 0 && totalAuthorities >= authorityLimit,
    };
  }, [authorityLimit, clientLimit, scopeLimit, totalAuthorities, totalClients, totalScopes, totalUsers, userLimit]);

  const summaryTiles = [
    {
      label: "Current Tier",
      value: status === "loading" ? "Loading..." : subscriptionTierName,
    },
    {
      label: "Total Scopes",
      value: isLoading ? "Loading..." : totalScopes,
    },
    {
      label: "Total authorities",
      value: isLoading ? "Loading..." : totalAuthorities,
    },
    ...(!isPaid
      ? [
          {
            label: "Client Access",
            value: "Upgrade required",
            className: "dashboard-summary-tile-highlight",
          },
        ]
      : []),
  ];
  // TODO: Replace these placeholder sections with real logout and session-expiry events once the platform activity model exposes them.
  const futureActivitySections = [
    {
      key: "logout",
      heading: "Recently logged out",
      timeLabel: "Logged out",
      emptyMessage: isLoading ? "Loading activity..." : "No logout activity yet.",
    },
    {
      key: "expired",
      heading: "Recently expired sessions",
      timeLabel: "Expired",
      emptyMessage: isLoading ? "Loading activity..." : "No expired sessions yet.",
    },
  ];

  return (
    <div className="clients-page">
      <div className="client-shell">
        <div className="client-header">
          <div className="client-header-copy">
            <div className="client-eyebrow">Dashboard</div>
            <div className="client-title">Identity dashboard</div>
            <div className="client-subtitle">
              See your plan limits, manage clients, and jump back into your account.
            </div>
          </div>
        </div>

        <div className="dashboard-layout">
          <section className="client-card client-card-primary dashboard-overview-card">
            <div className="client-card-header">
              <div>
                <div className="client-card-kicker">Overview</div>
                <div className="client-card-title">Plan and usage</div>
              </div>
              <div className="client-card-caption">
                {status === "loading"
                  ? "Checking plan access..."
                  : `Current tier: ${subscriptionTierName}`}
              </div>
            </div>
            <div className="client-card-contents dashboard-card-contents">
              <div className={`dashboard-usage-card dashboard-usage-card-accent${dashboardSummary.clientsMaxed ? " is-maxed" : ""}`}>
                <div className="dashboard-usage-header">
                  <div>
                    <div className="dashboard-usage-title">Registered clients</div>
                    <div className="dashboard-usage-caption">{dashboardSummary.clientLimitCaption}</div>
                  </div>
                  <div className="dashboard-usage-ratio">
                    {isLoading ? "Loading..." : `${totalClients} / ${clientLimit}`}
                  </div>
                </div>
                <div className="dashboard-progress-track" aria-hidden="true">
                  <div
                    className={`dashboard-progress-fill${dashboardSummary.clientsMaxed ? " dashboard-progress-fill-danger" : ""}`}
                    style={{ width: `${dashboardSummary.clientUsagePercent}%` }}
                  />
                </div>
              </div>

              <div className={`dashboard-usage-card${dashboardSummary.usersMaxed ? " is-maxed" : ""}`}>
                <div className="dashboard-usage-header">
                  <div>
                    <div className="dashboard-usage-title">Client users</div>
                    <div className="dashboard-usage-caption">{dashboardSummary.userLimitCaption}</div>
                  </div>
                  <div className="dashboard-usage-ratio">
                    {isLoading ? "Loading..." : `${totalUsers} / ${userLimit}`}
                  </div>
                </div>
                <div className="dashboard-progress-track" aria-hidden="true">
                  <div
                    className={`dashboard-progress-fill dashboard-progress-fill-secondary${dashboardSummary.usersMaxed ? " dashboard-progress-fill-danger" : ""}`}
                    style={{ width: `${dashboardSummary.userUsagePercent}%` }}
                  />
                </div>
              </div>

              <div className={`dashboard-usage-card${dashboardSummary.scopesMaxed ? " is-maxed" : ""}`}>
                <div className="dashboard-usage-header">
                  <div>
                    <div className="dashboard-usage-title">Scopes</div>
                    <div className="dashboard-usage-caption">{dashboardSummary.scopeLimitCaption}</div>
                  </div>
                  <div className="dashboard-usage-ratio">
                    {isLoading ? "Loading..." : `${totalScopes} / ${scopeLimit}`}
                  </div>
                </div>
                <div className="dashboard-progress-track" aria-hidden="true">
                  <div
                    className={`dashboard-progress-fill dashboard-progress-fill-tertiary${dashboardSummary.scopesMaxed ? " dashboard-progress-fill-danger" : ""}`}
                    style={{ width: `${dashboardSummary.scopeUsagePercent}%` }}
                  />
                </div>
              </div>

              <div className={`dashboard-usage-card${dashboardSummary.authoritiesMaxed ? " is-maxed" : ""}`}>
                <div className="dashboard-usage-header">
                  <div>
                    <div className="dashboard-usage-title">Authorities</div>
                    <div className="dashboard-usage-caption">
                      {dashboardSummary.authorityLimitCaption} shared across roles and authorities.
                    </div>
                  </div>
                  <div className="dashboard-usage-ratio">
                    {isLoading ? "Loading..." : `${totalAuthorities} / ${authorityLimit}`}
                  </div>
                </div>
                <div className="dashboard-progress-track" aria-hidden="true">
                  <div
                    className={`dashboard-progress-fill dashboard-progress-fill-quaternary${dashboardSummary.authoritiesMaxed ? " dashboard-progress-fill-danger" : ""}`}
                    style={{ width: `${dashboardSummary.authorityUsagePercent}%` }}
                  />
                </div>
              </div>

              <div
                className={`client-summary-grid dashboard-summary-grid dashboard-summary-grid-${summaryTiles.length}`}
              >
                {summaryTiles.map((tile) => (
                  <div
                    className={`client-summary-tile${tile.className ? ` ${tile.className}` : ""}`}
                    key={tile.label}
                  >
                    <div className="client-summary-label">{tile.label}</div>
                    <div className="client-summary-value">{tile.value}</div>
                  </div>
                ))}
              </div>
            </div>
          </section>

          <div className="dashboard-secondary-grid">
            <section className="client-card client-card-secondary">
              <div className="client-card-header">
                <div>
                  <div className="client-card-kicker">Activity</div>
                  <div className="client-card-title">Recent user activity</div>
                </div>
                <div className="client-card-caption">
                  Track new users, fresh logins, and upcoming session activity signals from one place.
                </div>
              </div>
              <div className="client-card-contents dashboard-activity-panel">
                <div className="dashboard-activity-grid">
                <div className="dashboard-activity-section">
                  <div className="dashboard-activity-heading">Recently created users</div>
                  <div className="dashboard-activity-table-shell">
                    <div className="dashboard-activity-table-head" role="row">
                      <span role="columnheader">User</span>
                      <span role="columnheader">Signed up</span>
                      <span role="columnheader">Action</span>
                    </div>
                    {recentUsers.length === 0 ? (
                      <div className="dashboard-activity-empty">
                        {isLoading ? "Loading users..." : "No recent users yet."}
                      </div>
                    ) : (
                      <div className="dashboard-activity-table-scroll">
                        <div className="dashboard-activity-table-body">
                          {recentUsers.map((user, index) => (
                            <div className="dashboard-activity-row" key={user.key || `recent-${user.id || user.email}-${index}`}>
                              <div className="dashboard-activity-cell dashboard-activity-cell-user">
                                <div className="dashboard-activity-title">{user.email}</div>
                              </div>
                              <div className="dashboard-activity-cell dashboard-activity-cell-time">
                                {formatTimestamp(user.activityTs || user.createdAt)}
                              </div>
                              <div className="dashboard-activity-cell dashboard-activity-cell-action">
                                {user.registeredClientId && user.id ? (
                                  <Link
                                    className="dashboard-activity-open-link"
                                    to={`/clients/${user.registeredClientId}/users/${user.id}`}
                                    state={buildClientUserRouteState(user)}
                                  >
                                    Open
                                  </Link>
                                ) : (
                                  <span className="dashboard-activity-open-muted">Unavailable</span>
                                )}
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                </div>

                <div className="dashboard-activity-section">
                  <div className="dashboard-activity-heading">Recent logins</div>
                  <div className="dashboard-activity-table-shell">
                    <div className="dashboard-activity-table-head" role="row">
                      <span role="columnheader">User</span>
                      <span role="columnheader">Last login</span>
                      <span role="columnheader">Action</span>
                    </div>
                    {recentActiveUsers.length === 0 ? (
                      <div className="dashboard-activity-empty">
                        {isLoading ? "Loading activity..." : "No recent token activity yet."}
                      </div>
                    ) : (
                      <div className="dashboard-activity-table-scroll">
                        <div className="dashboard-activity-table-body">
                          {recentActiveUsers.map((user, index) => (
                            <div className="dashboard-activity-row" key={user.key || `active-${user.id || user.email}-${index}`}>
                              <div className="dashboard-activity-cell dashboard-activity-cell-user">
                                <div className="dashboard-activity-title">{user.email}</div>
                              </div>
                              <div className="dashboard-activity-cell dashboard-activity-cell-time">
                                {formatTimestamp(user.issuedAt)}
                              </div>
                              <div className="dashboard-activity-cell dashboard-activity-cell-action">
                                {user.registeredClientId && user.id ? (
                                  <Link
                                    className="dashboard-activity-open-link"
                                    to={`/clients/${user.registeredClientId}/users/${user.id}`}
                                    state={buildClientUserRouteState(user)}
                                  >
                                    Open
                                  </Link>
                                ) : (
                                  <span className="dashboard-activity-open-muted">Unavailable</span>
                                )}
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                </div>

                {futureActivitySections.map((section) => (
                  <div className="dashboard-activity-section" key={section.key}>
                    <div className="dashboard-activity-heading">{section.heading}</div>
                    <div className="dashboard-activity-table-shell">
                      <div className="dashboard-activity-table-head" role="row">
                        <span role="columnheader">User</span>
                        <span role="columnheader">{section.timeLabel}</span>
                        <span role="columnheader">Action</span>
                      </div>
                      <div className="dashboard-activity-empty">
                        {section.emptyMessage}
                      </div>
                    </div>
                  </div>
                ))}
                </div>
              </div>
            </section>
          </div>
        </div>
      </div>
    </div>
  );
}
