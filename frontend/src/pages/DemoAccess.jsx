import "./Login.css";
import "./Errors.css";
import { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { OidcClient } from "oidc-client-ts";
import ErrorContainer from "../components/ErrorContainer";
import { disableDevAuthBypass } from "../auth/devAuth";
import { userManager } from "../auth/oidc";
import { APP_ENDPOINTS } from "../config/endpoints";

function normalizeDemoCode(value) {
  return String(value ?? "").trim();
}

function resolveRequestedDemoCode(searchParams) {
  return normalizeDemoCode(searchParams.get("code") ?? searchParams.get("number"));
}

export default function DemoAccess() {
  const [searchParams] = useSearchParams();
  const [demoCode, setDemoCode] = useState("");
  const [errorMsg, setErrorMsg] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const requestedDemoCode = useMemo(() => resolveRequestedDemoCode(searchParams), [searchParams]);

  useEffect(() => {
    if (requestedDemoCode) {
      setDemoCode(requestedDemoCode);
    }

    if (searchParams.get("error") === "invalid_code") {
      setErrorMsg("That demo access code is invalid or has already been used.");
    }
  }, [requestedDemoCode, searchParams]);

  const submitDisabled = useMemo(
    () => submitting || normalizeDemoCode(demoCode).length === 0,
    [demoCode, submitting]
  );

  const handleSubmit = async (event) => {
    event.preventDefault();

    const normalizedCode = normalizeDemoCode(demoCode);
    if (!normalizedCode) {
      setErrorMsg("Demo access code is required.");
      return;
    }

    setErrorMsg("");
    setSubmitting(true);
    disableDevAuthBypass();

    try {
      const oidcClient = new OidcClient(userManager.settings);
      const signinRequest = await oidcClient.createSigninRequest({});
      const authorizeUrl = new URL(signinRequest.url);
      const form = document.createElement("form");

      form.method = "POST";
      form.action = APP_ENDPOINTS.platform.demoAccessCode;
      form.style.display = "none";

      const codeInput = document.createElement("input");
      codeInput.type = "hidden";
      codeInput.name = "code";
      codeInput.value = normalizedCode;
      form.appendChild(codeInput);

      authorizeUrl.searchParams.forEach((value, key) => {
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = key;
        input.value = value;
        form.appendChild(input);
      });

      document.body.appendChild(form);
      form.submit();
    } catch (error) {
      console.error("Demo access request setup failed", error);
      setErrorMsg("Unable to prepare demo access.");
      setSubmitting(false);
    }
  };

  return (
    <div className="login-root">
      <div className="login-shell">
        <div className="login-card">
          <div className="login-kicker">Demo Access</div>
          <h1>Enter your demo code</h1>
          <p className="login-copy">
            Use a one-time demo access code to sign in without the normal login form.
          </p>
          {errorMsg ? <ErrorContainer errors={errorMsg} className="error-container-centered" /> : null}

          <form className="login-form" onSubmit={handleSubmit}>
            <label className="login-label" htmlFor="demo-access-code">
              Access code
            </label>
            <input
              id="demo-access-code"
              className="login-input"
              type="text"
              autoComplete="one-time-code"
              placeholder="Enter demo code"
              value={demoCode}
              onChange={(event) => setDemoCode(event.currentTarget.value)}
              disabled={submitting}
              autoFocus
            />

            <button type="submit" className="login-button" disabled={submitDisabled}>
              {submitting ? "Opening demo..." : "Continue with demo code"}
            </button>
          </form>

          <div className="login-secondary-actions">
            <Link className="login-secondary-link" to="/app/login">
              Back to normal login
            </Link>
          </div>

          <div className="login-footer">Demo codes can only be used once.</div>
        </div>
      </div>
    </div>
  );
}
