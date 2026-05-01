import { useSubscription } from "../context/SubscriptionContext";
import { useEffect, useMemo, useState } from "react";
import Spinner from "react-bootstrap/Spinner";
import { Navigate } from "react-router-dom";
import { fetchAdminDashboard } from "../services/platform";
import "./Clients.css";
import "./Home.css";

export default function Admin() {
  const { isAdmin, status } = useSubscription();
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [demoCodes, setDemoCodes] = useState([]);

  useEffect(() => {
    if (status !== "ready" || !isAdmin) {
      return undefined;
    }

    let isMounted = true;

    setIsLoading(true);
    setError("");

    fetchAdminDashboard()
      .then((payload) => {
        if (!isMounted) {
          return;
        }

        setDemoCodes(Array.isArray(payload?.demoCodes) ? payload.demoCodes : []);
      })
      .catch((loadError) => {
        if (!isMounted) {
          return;
        }

        setError(loadError?.message || "Unable to load admin dashboard.");
        setDemoCodes([]);
      })
      .finally(() => {
        if (isMounted) {
          setIsLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [isAdmin, status]);

  const summary = useMemo(() => {
    const totalCodes = demoCodes.length;
    const totalUses = demoCodes.reduce((sum, code) => sum + Number(code.useCount || 0), 0);
    const totalCapacity = demoCodes.reduce((sum, code) => sum + Number(code.useLimit || 0), 0);

    return {
      totalCodes,
      totalUses,
      totalCapacity,
    };
  }, [demoCodes]);

  if (status === "loading") {
    return <Spinner animation="grow" />;
  }

  if (!isAdmin) {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="clients-page">
      <div className="client-shell">
        <div className="client-header">
          <div className="client-header-copy">
            <div className="client-title">Admin Dashboard</div>
            <div className="client-subtitle">
              An overview of all admin related data.
            </div>
          </div>
        </div>

        <div className="dashboard-layout">
          <section className="client-card client-card-primary dashboard-overview-card">
            <div className="client-card-header">
              <div>
                <div className="client-card-kicker">Demo access code useage</div>
              </div>

            </div>

            <div className="client-card-contents dashboard-card-contents">
              <div className="client-summary-grid dashboard-summary-grid dashboard-summary-grid-3">
                <div className="client-summary-tile">
                  <div className="client-summary-label">Total available codes</div>
                  <div className="client-summary-value">{isLoading ? "..." : summary.totalCodes}</div>
                </div>
                <div className="client-summary-tile">
                  <div className="client-summary-label">Total amount of uses acrossed codes</div>
                  <div className="client-summary-value">{isLoading ? "..." : summary.totalUses}</div>
                </div>
                <div className="client-summary-tile">
                  <div className="client-summary-label">Total amount of allowed uses acrossed all codes</div>
                  <div className="client-summary-value">{isLoading ? "..." : summary.totalCapacity}</div>
                </div>
              </div>

              <div className="client-card client-card-secondary">
                <div className="client-card-contents">
                  <div className="dashboard-activity-section">
                    <div className="dashboard-activity-table-shell">
                      <div className="dashboard-activity-table-head" role="row">
                        <span role="columnheader">Access code</span>
                        <span role="columnheader">Usage</span>
                        <span role="columnheader">Limit</span>
                      </div>

                      {isLoading ? (
                        <div className="dashboard-activity-empty">Loading demo access codes...</div>
                      ) : error ? (
                        <div className="dashboard-activity-empty">{error}</div>
                      ) : demoCodes.length === 0 ? (
                        <div className="dashboard-activity-empty">No demo access codes were returned.</div>
                      ) : (
                        <div className="dashboard-activity-table-scroll">
                          <div className="dashboard-activity-table-body">
                            {demoCodes.map((code) => (
                              <div className="dashboard-activity-row" key={code.accessCode}>
                                <div className="dashboard-activity-cell dashboard-activity-cell-user">
                                  <div className="dashboard-activity-title">{code.accessCode}</div>
                                </div>
                                <div className="dashboard-activity-cell dashboard-activity-cell-time">
                                  {code.useCount} used
                                </div>
                                <div className="dashboard-activity-cell dashboard-activity-cell-action">
                                  <span className="dashboard-activity-open-muted">{code.useLimit}</span>
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
