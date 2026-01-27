import Login from "./pages/Login";
import RequireAuth from "./pages/RequireAuth";
import Callback from "./pages/Callback";
import Home from "./pages/Home";
import Clients from "./pages/Clients";
import Subscriptions from "./pages/Subscriptions";
import { Routes, Route } from "react-router-dom";
import Layout from "./Layout";
import 'bootstrap/dist/css/bootstrap.min.css';



export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/callback" element={<Callback/>} />

      <Route element={<RequireAuth />}>
        <Route element={<Layout />}>
          <Route path="/" element={<Home />} />
          <Route path="/clients" element={<Clients />} />
          <Route path="/subscriptions" element={<Subscriptions />} />
        </Route>
      </Route>
    </Routes>
  );
}
