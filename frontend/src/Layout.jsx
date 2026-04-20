import { useEffect, useMemo, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useSubscription } from "./context/SubscriptionContext";

export default function Layout() {
  const location = useLocation();
  const navigate = useNavigate();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const [isMobileNavOpen, setIsMobileNavOpen] = useState(false);
  const { tierName, status, isPaid } = useSubscription();

  const navItems = useMemo(() => {
    const items = [
      {
        to: "/",
        label: "Dashboard",
        end: true,
        matches: (pathname) => pathname === "/" || pathname === "/success",
      },
    ];

    if (isPaid) {
      items.push(
        {
          to: "/clients",
          label: "Registry",
          end: false,
          matches: (pathname) => pathname.startsWith("/clients") && pathname !== "/clients/new",
        },
        {
          to: "/clients/new",
          label: "New Client",
          end: false,
          matches: (pathname) => pathname === "/clients/new",
        },
      );
    } else {
      items.push({
        to: "/clients",
        label: "Clients",
        end: false,
        matches: (pathname) => pathname.startsWith("/clients"),
      });
    }

    items.push(
      {
        to: "/subscriptions",
        label: "Subscription",
        end: false,
        matches: (pathname) => pathname.startsWith("/subscriptions"),
      },
      {
        to: "/docs",
        label: "Documentation",
        end: false,
        matches: (pathname) => pathname.startsWith("/docs"),
      },
    );

    return items;
  }, [isPaid]);

  const currentPath = useMemo(() => {
    if (location.pathname === "/success" && typeof location.state?.redirectTo === "string") {
      return location.state.redirectTo;
    }

    return location.pathname;
  }, [location.pathname, location.state]);

  const currentNavItem = navItems.find((item) => item.matches(currentPath)) ?? navItems[0];

  useEffect(() => {
    setIsMobileNavOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!isMobileNavOpen) {
      return undefined;
    }

    const handleKeyDown = (event) => {
      if (event.key === "Escape") {
        setIsMobileNavOpen(false);
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isMobileNavOpen]);

  const handleLogout = async () => {
    if (isLoggingOut) {
      return;
    }

    setIsMobileNavOpen(false);
    setIsLoggingOut(true);
    navigate("/logout", { replace: true });
  };

  const renderNavLinks = (className) =>
    navItems.map((item) => (
      <NavLink
        key={item.to}
        to={item.to}
        end={item.end}
        className={() => `${className}${item.matches(currentPath) ? " active" : ""}`}
        onClick={() => setIsMobileNavOpen(false)}
      >
        {item.label}
      </NavLink>
    ));

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

        <nav className="nav">{renderNavLinks("nav-item")}</nav>

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

      <div className="app-shell">
        <div className={`mobile-header-stack${isMobileNavOpen ? " is-menu-open" : ""}`}>
          <div className="mobile-topbar">
            <div className="mobile-topbar-row">
              <div className="mobile-topbar-branding">
                <div className="mobile-topbar-kicker">Identity Platform</div>
                <div className="mobile-topbar-tier">
                  {status === "loading" ? "Loading tier..." : `Tier: ${tierName}`}
                </div>
              </div>

              <button
                type="button"
                className={`mobile-nav-trigger${isMobileNavOpen ? " is-open" : ""}`}
                onClick={() => setIsMobileNavOpen((prev) => !prev)}
                aria-expanded={isMobileNavOpen}
                aria-controls="mobile-navigation-sheet"
                aria-label={isMobileNavOpen ? "Close section menu" : "Open section menu"}
              >
                <span className="mobile-nav-trigger-copy">
                  <span className="mobile-nav-trigger-label">Section</span>
                  <span className="mobile-nav-trigger-value">{currentNavItem.label}</span>
                </span>
                <svg className="mobile-nav-trigger-icon" viewBox="0 0 20 20" fill="none" aria-hidden="true">
                  <path d="M5 7.5 10 12.5l5-5" />
                </svg>
              </button>
            </div>
          </div>

          <div
            id="mobile-navigation-sheet"
            className={`mobile-nav-sheet${isMobileNavOpen ? " is-open" : ""}`}
          >
            <div className="mobile-nav-panel">
              <div className="mobile-nav-panel-header">
                <div className="mobile-nav-panel-kicker">Sections</div>
                <div className="mobile-nav-panel-title">Choose where you want to go.</div>
              </div>

              <nav className="mobile-nav-list">{renderNavLinks("mobile-nav-item")}</nav>

              <div className="mobile-nav-footer">
                <button
                  type="button"
                  className="mobile-nav-logout"
                  onClick={handleLogout}
                  disabled={isLoggingOut}
                >
                  {isLoggingOut ? "Signing out..." : "Log out"}
                </button>
              </div>
            </div>
          </div>
        </div>

        <main className={`content${isSidebarCollapsed ? " is-sidebar-collapsed" : ""}`}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
