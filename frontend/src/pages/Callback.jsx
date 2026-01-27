import { useEffect } from "react";
import { userManager } from "../auth/oidc";

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

  return <div>Signing you in…</div>;
}
