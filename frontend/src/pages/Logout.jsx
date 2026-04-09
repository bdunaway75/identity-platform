import { useEffect } from "react";
import { userManager } from "../auth/oidc";
import { disableDevAuthBypass, isDevAuthBypassed } from "../auth/devAuth";
import {
  clearDevSubscriptionOverrideTier,
  clearSubscriptionTierCache,
} from "../services/subscription";
import { clearPlatformApiCache } from "../services/platform";
import "./Login.css";

export default function Logout() {
  useEffect(() => {
    const wasUsingDevBypass = isDevAuthBypassed();
    disableDevAuthBypass();
    clearDevSubscriptionOverrideTier();
    clearSubscriptionTierCache();
    clearPlatformApiCache();

    if (wasUsingDevBypass) {
      window.location.replace("/login");
      return;
    }

    userManager
      .getUser()
      .then((user) =>
        userManager.signoutRedirect(
          user?.id_token
            ? { id_token_hint: user.id_token }
            : undefined
        )
      )
      .catch((error) => {
        console.error("Logout failed", error);
        window.location.replace("/login");
      });
  }, []);

  return (
    <div className="login-root">
      <div className="login-shell">
        <div className="login-card">
          <div className="logout-badge">Signing Out</div>
        <h1>Wrapping things up</h1>
        <p className="logout-copy">
          We are securely signing you out and returning you to the login screen.
        </p>
        <div className="logout-status" aria-live="polite">
          <span className="logout-spinner" aria-hidden="true" />
          <span>Ending your session...</span>
        </div>
          <div className="login-footer">You will be redirected automatically.</div>
        </div>
      </div>
    </div>
  );
}
