import "./Login.css";
import "./Errors.css";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

import LoginButton from "../components/LoginButton";
import ErrorContainer from "../components/ErrorContainer";
import { enableDevAuthBypass, isDevAuthBypassAvailable } from "../auth/devAuth";
import { PAID_TIER, setDevSubscriptionOverrideTier } from "../services/subscription";

export default function Login() {
  const [errorMsg, setErrorMsg] = useState("");
  const navigate = useNavigate();
  const canBypassAuth = isDevAuthBypassAvailable();

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
          <h1>Please sign in.</h1>
          <LoginButton onError={() => setErrorMsg("Unable to authenticate.")} />
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
