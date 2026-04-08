import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useSubscription } from "./context/SubscriptionContext";

export default function Layout() {
  const navigate = useNavigate();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const { tier, status } = useSubscription();

  const handleLogout = async () => {
    if (isLoggingOut) {
      return;
    }

    setIsLoggingOut(true);
    navigate("/logout", { replace: true });
  };

  return (
    <div className="app-root">
      <aside className="sidebar">
        <div className="brand">Identity Platform</div>

        <nav className="nav">
          <NavLink to="/" className="nav-item">Dashboard</NavLink>
          <NavLink to="/clients" className="nav-item">Clients</NavLink>
          <NavLink to="/subscriptions" className="nav-item">Subscription</NavLink>
        </nav>

        <div className="sidebar-footer">
          <div className="tier">Tier: {status === "loading" ? "Loading..." : tier}</div>
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

      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
