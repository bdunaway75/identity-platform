import { useEffect, useState } from "react";
import { Navigate, Outlet } from "react-router-dom";
import { userManager } from "../auth/oidc";
import Spinner from 'react-bootstrap/Spinner';

export default function RequireAuth() {
  const [status, setStatus] = useState("checking");
  useEffect(() => {
    userManager.getUser().then(user => {
      if (user && !user.expired) {
        setStatus("authed");
      } else {
        setStatus("unauth");
      }
    });
  }, []);

  if (status === "checking") {
    return <Spinner animation="grow" />;
  }

  if (status === "unauth") {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}
