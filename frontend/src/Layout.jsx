import { useEffect, useMemo, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useSubscription } from "./context/SubscriptionContext";

const creatorLinks = [
  {
    label: "Email Blake Dunaway",
    href: "mailto:badunawa@alumni.iu.edu",
    external: false,
    icon: (
      <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <rect x="3" y="5" width="18" height="14" rx="2.5" />
        <path d="m4 7 8 6 8-6" />
      </svg>
    ),
  },
  {
    label: "Blake Dunaway on GitHub",
    href: "https://github.com/bdunaway75/bdunaway75",
    external: true,
    icon: (
      <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M9 19c-5 1.5-5-2.5-7-3" />
        <path d="M15 22v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 19.5 4.77 5.07 5.07 0 0 0 19.41 1S18.23.65 15.5 2.48a13.38 13.38 0 0 0-7 0C5.77.65 4.59 1 4.59 1A5.07 5.07 0 0 0 4.5 4.77 5.44 5.44 0 0 0 3 8.52c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 8.5 18.13V22" />
      </svg>
    ),
  },
  {
    label: "Blake Dunaway on LinkedIn",
    href: "https://www.linkedin.com/in/blake-dunaway",
    external: true,
    icon: (
      <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M16 8a6 6 0 0 1 6 6v7h-4v-7a2 2 0 0 0-2-2 2 2 0 0 0-2 2v7h-4v-7a6 6 0 0 1 6-6z" />
        <rect x="2" y="9" width="4" height="12" />
        <circle cx="4" cy="4" r="2" />
      </svg>
    ),
  },
];

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

  const renderCreatorLinks = (className) =>
    creatorLinks.map((link) => (
      <a
        key={link.label}
        className={className}
        href={link.href}
        aria-label={link.label}
        title={link.label}
        target={link.external ? "_blank" : undefined}
        rel={link.external ? "noreferrer" : undefined}
      >
        {link.icon}
      </a>
    ));

  return (
    <div className="app-root">
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
          <div className="sidebar-socials" aria-label="Creator links">
            {renderCreatorLinks("creator-link")}
          </div>
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
                <div className="mobile-nav-socials" aria-label="Creator links">
                  {renderCreatorLinks("creator-link")}
                </div>
              </div>
            </div>
          </div>
        </div>

        <main className="content">
          <Outlet />
          <div className="mobile-bottom-spacer" aria-hidden="true" />
        </main>
      </div>
    </div>
  );
}
