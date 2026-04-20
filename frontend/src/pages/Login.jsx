import "./Login.css";
import "./Errors.css";
import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";

import LoginButton from "../components/LoginButton";
import ErrorContainer from "../components/ErrorContainer";
import { enableDevAuthBypass, isDevAuthBypassAvailable } from "../auth/devAuth";
import { PAID_TIER, setDevSubscriptionOverrideTier } from "../services/subscription";

export default function Login() {
  const [errorMsg, setErrorMsg] = useState("");
  const navigate = useNavigate();
  const location = useLocation();
  const canBypassAuth = isDevAuthBypassAvailable();

  useEffect(() => {
    const searchParams = new URLSearchParams(location.search);
    if (searchParams.get("expired") === "1") {
      setErrorMsg("Your session expired. Please sign in again.");
    }
  }, [location.search]);

  const handleLocalAccess = () => {
    enableDevAuthBypass();
    setDevSubscriptionOverrideTier(PAID_TIER);
    navigate("/");
  };

  return (
    <div className="login-root">
      <div className="login-shell">
        {errorMsg && <ErrorContainer errors={errorMsg} />}
        <div className="login-card">
          <div className="login-kicker">Platform Login</div>
          <h1>Please sign in.</h1>
          <p className="login-copy">
            Sign in with your platform account, or use a demo access code if you were given one.
          </p>
          <LoginButton onError={() => setErrorMsg("Unable to authenticate.")} />
          <Link
            to="/demo-access"
            className="login-button login-button-secondary"
            style={{ marginTop: "0.75rem" }}
          >
            Use demo access code
          </Link>
          {canBypassAuth ? (
            <button
              type="button"
              className="login-button"
              style={{ marginTop: "0.75rem", background: "transparent", border: "1px solid rgba(255,255,255,0.2)" }}
              onClick={handleLocalAccess}
            >
              Continue in local dev mode
            </button>
          ) : null}

          <div className="login-footer">
            {canBypassAuth ? "Local testing mode is available on localhost." : "Authorized users only"}
          </div>
        </div>
      </div>
    </div>
  );
}
