import { userManager } from "./auth/oidc";

export async function apiFetch(url, options = {}) {
  const user = await userManager.getUser();

  if (!user || user.expired) {
    await userManager.signinRedirect();
    throw new Error("Not authenticated");
  }

  return fetch(url, {
    ...options,
    headers: {
      ...(options.headers || {}),
      Authorization: `Bearer ${user.access_token}`,
    },
  });
}
