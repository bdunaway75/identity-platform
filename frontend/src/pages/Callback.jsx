import { useEffect } from "react";
import { userManager } from "../auth/oidc";
import "./Login.css";

export default function Callback() {
  useEffect(() => {
    const html = document.documentElement;
    const body = document.body;
    const themeColorMeta = document.querySelector('meta[name="theme-color"]');
    const previousThemeColor = themeColorMeta?.getAttribute("content") ?? null;

    html.classList.add("login-page-theme");
    body.classList.add("login-page-theme");
    themeColorMeta?.setAttribute("content", "#091112");

    userManager
      .signinRedirectCallback()
      .then(() => {
        window.location.replace("/");
      })
      .catch(err => {
        console.error("OIDC callback error", err);
        window.location.replace("/app/login");
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
          <div className="callback-badge">Signing In</div>
          <h1>Welcome back</h1>
          <p className="callback-copy">
            We are finishing your secure sign-in and taking you to the dashboard now.
          </p>
          <div className="callback-status" aria-live="polite">
            <span className="callback-spinner" aria-hidden="true" />
            <span>Preparing your dashboard...</span>
          </div>
          <div className="login-footer">You will be redirected automatically.</div>
        </div>
      </div>
    </div>
  );
}
