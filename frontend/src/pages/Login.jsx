import "./Login.css";
import "./Errors.css";
import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";

import LoginButton from "../components/LoginButton";
import ErrorContainer from "../components/ErrorContainer";
import { enableDevAuthBypass, isDevAuthBypassAvailable } from "../auth/devAuth";
import { PAID_TIER, setDevSubscriptionOverrideTier } from "../services/subscription";
import { APP_FLAGS } from "../config/endpoints";

export default function Login() {
  const [errorMsg, setErrorMsg] = useState("");
  const navigate = useNavigate();
  const location = useLocation();
  const canBypassAuth = isDevAuthBypassAvailable();
  const isBetaMode = APP_FLAGS.betaMode || new URLSearchParams(location.search).get("beta") === "1";
  const betaModeMessage = "Demo code access only.";

  useEffect(() => {
    const html = document.documentElement;
    const body = document.body;
    const themeColorMeta = document.querySelector('meta[name="theme-color"]');
    const previousThemeColor = themeColorMeta?.getAttribute("content") ?? null;

    html.classList.add("login-page-theme");
    body.classList.add("login-page-theme");
    themeColorMeta?.setAttribute("content", "#091112");

    return () => {
      html.classList.remove("login-page-theme");
      body.classList.remove("login-page-theme");

      if (previousThemeColor) {
        themeColorMeta?.setAttribute("content", previousThemeColor);
      }
    };
  }, []);

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
        {isBetaMode ? <ErrorContainer errors={betaModeMessage} /> : null}
        {!isBetaMode && errorMsg ? <ErrorContainer errors={errorMsg} /> : null}
        <div className="login-card">
          <div className="auth-brand">
            <img className="auth-brand-logo" src="/ip-favicon.png" alt="Identity Platform logo" />
          </div>
          <div className="login-kicker">Identity Platform</div>
          <h1>Please sign in.</h1>
          <p className="login-copy">
            Sign in with your platform account, or use a demo access code if you were given one.
          </p>
          <LoginButton
            onError={() => setErrorMsg("Unable to authenticate.")}
            disabled={isBetaMode}
            disabledLabel="Login disabled"
          />
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
            Authorized users only
            <br />
            Need a code? <a href="mailto:support@identificationplatform.com">support@identificationplatform.com</a>
          </div>
        </div>
      </div>
    </div>
  );
}
