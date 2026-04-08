import { UserManager } from "oidc-client-ts";

export const userManager = new UserManager({
  authority: "http://localhost:8080", // your AS base URL
  client_id: "identity-platform",
  redirect_uri: "http://localhost:5173/callback",
  post_logout_redirect_uri: "http://localhost:5173/login",
  response_type: "code",
  scope: "openid",
  automaticSilentRenew: true,
  loadUserInfo: false,
});
