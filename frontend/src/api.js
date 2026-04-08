import { userManager } from "./auth/oidc";

export async function apiFetch(url, options = {}) {
  const user = await userManager.getUser();

  const onAuthRoute =
      window.location.pathname.startsWith("/login") ||
      window.location.pathname.startsWith("/callback");

  if (!user || user.expired) {
    if (!onAuthRoute) await userManager.signinRedirect();
    throw new Error("Not authenticated");
  }

  const headers = new Headers(options.headers || {});
  headers.set("Authorization", `Bearer ${user.access_token}`);
  headers.set("Accept", "application/json");

  let body = options.body;
  if (body && typeof body === "object" && !(body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
    body = JSON.stringify(body);
  }

  const resp = await fetch(url, { ...options, headers, body });

  if (resp.status === 401 && !onAuthRoute) {
    await userManager.signinRedirect();
  }

  return resp;
}
