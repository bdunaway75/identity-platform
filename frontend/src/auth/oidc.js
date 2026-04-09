import { UserManager } from "oidc-client-ts";
import { APP_ENDPOINTS } from "../config/endpoints";

export const userManager = new UserManager({
  authority: APP_ENDPOINTS.oidc.authority,
  client_id: APP_ENDPOINTS.oidc.clientId,
  redirect_uri: APP_ENDPOINTS.oidc.redirectUri,
  post_logout_redirect_uri: APP_ENDPOINTS.oidc.postLogoutRedirectUri,
  response_type: "code",
  scope: "openid",
  automaticSilentRenew: false,
  monitorSession: false,
  loadUserInfo: false,
});

userManager.stopSilentRenew();
