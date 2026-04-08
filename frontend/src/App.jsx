import { Routes, Route } from "react-router-dom";
import Login from "./pages/Login";
import RequireAuth from "./pages/RequireAuth";
import Callback from "./pages/Callback";
import Logout from "./pages/Logout";
import Home from "./pages/Home";
import Clients from "./pages/Clients";
import Subscriptions from "./pages/Subscriptions";
import Layout from "./Layout";
import { SubscriptionProvider } from "./context/SubscriptionContext";
import "bootstrap/dist/css/bootstrap.min.css";

function ProtectedAppShell() {
  return (
    <SubscriptionProvider>
      <Layout />
    </SubscriptionProvider>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/callback" element={<Callback />} />
      <Route path="/logout" element={<Logout />} />

      <Route element={<RequireAuth />}>
        <Route element={<ProtectedAppShell />}>
          <Route path="/" element={<Home />} />
          <Route path="/clients" element={<Clients />} />
          <Route path="/subscriptions" element={<Subscriptions />} />
        </Route>
      </Route>
    </Routes>
  );
}
