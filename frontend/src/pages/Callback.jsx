import { useEffect } from "react";
import { userManager } from "../auth/oidc";
import "./Callback.css";

export default function Callback() {
  useEffect(() => {
    userManager
      .signinRedirectCallback()
      .then(() => {
        window.location.replace("/");
      })
      .catch(err => {
        console.error("OIDC callback error", err);
        window.location.replace("/login");
      });
  }, []);

  return (
    <div className="callback-root">
      <div className="callback-card">
        <div className="callback-badge">Signing In</div>
        <h1>Welcome back</h1>
        <p>
          We are finishing your secure sign-in and taking you to the dashboard now.
        </p>
        <div className="callback-status" aria-live="polite">
          <span className="callback-spinner" aria-hidden="true" />
          <span>Preparing your workspace...</span>
        </div>
      </div>
    </div>
  );
}
