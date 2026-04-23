import { useEffect } from "react";
import { userManager } from "../auth/oidc";
import { disableDevAuthBypass, isDevAuthBypassed } from "../auth/devAuth";
import {
  clearDevSubscriptionOverrideTier,
  clearSubscriptionTierCache,
} from "../services/subscription";
import { clearPlatformApiCache } from "../services/platform";
import "./Login.css";

function buildFallbackLogoutUrl() {
  const authority = String(userManager.settings.authority ?? "").trim();
  const clientId = String(userManager.settings.client_id ?? "").trim();
  const postLogoutRedirectUri = String(userManager.settings.post_logout_redirect_uri ?? "").trim();

  if (!authority) {
    return "/app/login";
  }

  const logoutUrl = new URL("/connect/logout", authority);

  if (clientId) {
    logoutUrl.searchParams.set("client_id", clientId);
  }

  if (postLogoutRedirectUri) {
    logoutUrl.searchParams.set("post_logout_redirect_uri", postLogoutRedirectUri);
  }

  return logoutUrl.toString();
}

function buildSignoutRequest(user) {
  const request = {};
  const clientId = String(userManager.settings.client_id ?? "").trim();
  const postLogoutRedirectUri = String(userManager.settings.post_logout_redirect_uri ?? "").trim();

  if (clientId) {
    request.client_id = clientId;
  }

  if (postLogoutRedirectUri) {
    request.post_logout_redirect_uri = postLogoutRedirectUri;
  }

  if (user?.id_token) {
    request.id_token_hint = user.id_token;
  }

  return request;
}

export default function Logout() {
  useEffect(() => {
    const html = document.documentElement;
    const body = document.body;
    const themeColorMeta = document.querySelector('meta[name="theme-color"]');
    const previousThemeColor = themeColorMeta?.getAttribute("content") ?? null;
    const wasUsingDevBypass = isDevAuthBypassed();

    html.classList.add("login-page-theme");
    body.classList.add("login-page-theme");
    themeColorMeta?.setAttribute("content", "#091112");

    disableDevAuthBypass();
    clearDevSubscriptionOverrideTier();
    clearSubscriptionTierCache();
    clearPlatformApiCache();

    if (wasUsingDevBypass) {
      window.location.replace("/app/login");
      return;
    }

    userManager
      .getUser()
      .then((user) => userManager.signoutRedirect(buildSignoutRequest(user)))
      .catch((error) => {
        console.error("Logout failed", error);
        window.location.replace(buildFallbackLogoutUrl());
      });

    return () => {
      html.classList.remove("login-page-theme");
      body.classList.remove("login-page-theme");

      if (previousThemeColor) {
        themeColorMeta?.setAttribute("content", previousThemeColor);
      }
    };
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
