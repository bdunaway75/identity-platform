import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useSubscription } from "./context/SubscriptionContext";

export default function Layout() {
  const navigate = useNavigate();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const { tierName, status, isPaid } = useSubscription();

  const handleLogout = async () => {
    if (isLoggingOut) {
      return;
    }

    setIsLoggingOut(true);
    navigate("/logout", { replace: true });
  };

  return (
    <div className={`app-root${isSidebarCollapsed ? " is-sidebar-collapsed" : ""}`}>
      <aside className={`sidebar${isSidebarCollapsed ? " is-collapsed" : ""}`}>
        <div className="sidebar-topbar">
          <div className="brand">Identity Platform</div>
          <button
            type="button"
            className="sidebar-inline-toggle"
            onClick={() => setIsSidebarCollapsed((prev) => !prev)}
            aria-label={isSidebarCollapsed ? "Expand navigation" : "Collapse navigation"}
            aria-expanded={!isSidebarCollapsed}
          >
            <svg
              className="sidebar-inline-toggle-icon"
              viewBox="0 0 20 20"
              fill="none"
              aria-hidden="true"
            >
              <rect x="3" y="3" width="3" height="14" rx="1.5" />
              {isSidebarCollapsed ? (
                <path d="M9 10h8M13 6l4 4-4 4" />
              ) : (
                <path d="M17 10H9M13 6l-4 4 4 4" />
              )}
            </svg>
          </button>
        </div>

        <nav className="nav">
          <NavLink to="/" className="nav-item">Dashboard</NavLink>
          {isPaid ? (
            <>
              <NavLink to="/clients" end className="nav-item">Registry</NavLink>
              <NavLink to="/clients/new" className="nav-item">New Client</NavLink>
            </>
          ) : (
            <NavLink to="/clients" end className="nav-item">Clients</NavLink>
          )}
          <NavLink to="/subscriptions" className="nav-item">Subscription</NavLink>
          <NavLink to="/docs" className="nav-item">Docs</NavLink>
        </nav>

        <div className="sidebar-footer">
          <div className="tier">Tier: {status === "loading" ? "Loading..." : tierName}</div>
          <button
            type="button"
            className="sidebar-logout"
            onClick={handleLogout}
            disabled={isLoggingOut}
          >
            {isLoggingOut ? "Signing out..." : "Log out"}
          </button>
        </div>
      </aside>

      <main className={`content${isSidebarCollapsed ? " is-sidebar-collapsed" : ""}`}>
        <Outlet />
      </main>
    </div>
  );
}
