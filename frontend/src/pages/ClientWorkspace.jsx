import { Outlet, useLocation } from "react-router-dom";
import "./Clients.css";

export default function ClientWorkspace({ children = null }) {
  const location = useLocation();
  const isCreateRoute = location.pathname === "/clients/new";
  const isUserDetailRoute = /^\/clients\/[^/]+\/users\/[^/]+$/.test(location.pathname);

  const eyebrow = isUserDetailRoute ? "Client User" : isCreateRoute ? "Register" : "Registry";
  const title = isUserDetailRoute
    ? "Manage a registered client user"
    : isCreateRoute
      ? "Create a new client"
      : "Manage your registered clients";
  const subtitle = isUserDetailRoute
    ? "Review attached user details, inspect issued tokens, and make account updates without leaving the registry area."
    : isCreateRoute
      ? "Configure the client sign-in, URLs, and permissions."
      : "Review the clients attached to your platform account and inspect attached users from a dedicated registry page.";

  return (
    <div className="clients-page">
      <div className="client-shell">
        {isUserDetailRoute ? null : (
          <div className="client-header">
            <div className="client-header-copy">
              <div className="client-eyebrow">{eyebrow}</div>
              <div className="client-title">{title}</div>
              <div className="client-subtitle">{subtitle}</div>
            </div>
          </div>
        )}

        {children ?? <Outlet />}
      </div>
    </div>
  );
}
