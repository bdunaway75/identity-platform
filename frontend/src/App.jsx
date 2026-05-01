import { useEffect } from "react";
import { Navigate, Outlet, Routes, Route, useLocation } from "react-router-dom";
import Login from "./pages/Login";
import DemoAccess from "./pages/DemoAccess";
import RequireAuth from "./pages/RequireAuth";
import Callback from "./pages/Callback";
import Logout from "./pages/Logout";
import Home from "./pages/Home";
import Clients from "./pages/Clients";
import ClientsAccessPage from "./pages/ClientsAccessPage";
import ClientWorkspace from "./pages/ClientWorkspace";
import CreateClient from "./pages/CreateClient";
import ClientUpdateSuccess from "./pages/ClientUpdateSuccess";
import ClientUserDetail from "./pages/ClientUserDetail";
import Subscriptions from "./pages/Subscriptions";
import SubscriptionCheckoutSuccess from "./pages/SubscriptionCheckoutSuccess";
import SubscriptionCheckoutCancel from "./pages/SubscriptionCheckoutCancel";
import Docs from "./pages/Docs";
import Admin from "./pages/Admin";
import Layout from "./Layout";
import { SubscriptionProvider } from "./context/SubscriptionContext";
import { useSubscription } from "./context/SubscriptionContext";
import { registerAuthSessionHandlers } from "./auth/session";
import "bootstrap/dist/css/bootstrap.min.css";

function ProtectedAppShell() {
  useEffect(() => registerAuthSessionHandlers(), []);

  return (
    <SubscriptionProvider>
      <Layout />
    </SubscriptionProvider>
  );
}

function ClientsRouteGate() {
  const location = useLocation();
  const { status, isPaid } = useSubscription();

  if (status === "loading") {
    return <ClientWorkspace />;
  }

  if (!isPaid) {
    if (location.pathname !== "/clients") {
      return <Navigate to="/clients" replace />;
    }

    return <ClientsAccessPage />;
  }

  return <ClientWorkspace><Outlet /></ClientWorkspace>;
}

export default function App() {
  return (
    <Routes>
      <Route path="/app/login" element={<Login />} />
      <Route path="/demo-access" element={<DemoAccess />} />
      <Route path="/callback" element={<Callback />} />
      <Route path="/logout" element={<Logout />} />

      <Route element={<RequireAuth />}>
        <Route element={<ProtectedAppShell />}>
          <Route path="/" element={<Home />} />
          <Route path="/success" element={<ClientUpdateSuccess />} />
          <Route path="/clients" element={<ClientsRouteGate />}>
            <Route index element={<Clients />} />
            <Route path="new" element={<CreateClient />} />
            <Route path=":registeredClientId/edit" element={<CreateClient />} />
            <Route path=":registeredClientId/users/:clientUserId" element={<ClientUserDetail />} />
          </Route>
          <Route path="/subscriptions" element={<Subscriptions />} />
          <Route path="/subscriptions/success" element={<SubscriptionCheckoutSuccess />} />
          <Route path="/subscriptions/cancel" element={<SubscriptionCheckoutCancel />} />
          <Route path="/docs" element={<Docs />} />
          <Route path="/admin" element={<Admin />} />
        </Route>
      </Route>
    </Routes>
  );
}
