import { NavLink, Outlet } from "react-router-dom";

export default function Layout() {
  return (
    <div className="app-root">
      <aside className="sidebar">
        <div className="brand">Identity Platform</div>

        <nav className="nav">
          <NavLink to="/" className="nav-item">Dashboard</NavLink>
          <NavLink to="/clients" className="nav-item">Clients</NavLink>
          <NavLink to="/subscriptions" className="nav-item">Subscription</NavLink>
        </nav>

        <div className="tier">Tier: Free</div>
      </aside>

      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
