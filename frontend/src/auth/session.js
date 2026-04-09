import { isDevAuthBypassed } from "./devAuth";
import { userManager } from "./oidc";

let isRedirectingToLogout = false;

function isAuthRoute(pathname) {
  return pathname === "/login" || pathname === "/logout" || pathname === "/callback";
}

function redirectToLogout() {
  if (typeof window === "undefined" || isRedirectingToLogout) {
    return;
  }

  isRedirectingToLogout = true;
  const pathname = window.location.pathname;
  window.location.replace(isAuthRoute(pathname) ? "/login" : "/logout");
}

export async function forceLogoutForExpiredSession() {
  if (isDevAuthBypassed()) {
    return;
  }

  try {
    await userManager.removeUser();
  } catch {
    // Ignore local session cleanup failures and continue to redirect.
  }

  redirectToLogout();
}

export async function getValidAccessToken(errorMessage) {
  if (isDevAuthBypassed()) {
    throw new Error(errorMessage);
  }

  const user = await userManager.getUser();

  if (!user?.access_token || user.expired) {
    await forceLogoutForExpiredSession();
    throw new Error(errorMessage);
  }

  return user.access_token;
}

export async function authenticatedFetch(input, init) {
  const response = await fetch(input, init);

  if (response.status === 401 && !isDevAuthBypassed()) {
    await forceLogoutForExpiredSession();
  }

  return response;
}

export function registerAuthSessionHandlers() {
  if (isDevAuthBypassed()) {
    return () => {};
  }

  const handleSessionExpired = () => {
    forceLogoutForExpiredSession();
  };

  userManager.events.addAccessTokenExpired(handleSessionExpired);
  userManager.events.addUserSignedOut(handleSessionExpired);
  userManager.events.addUserUnloaded(handleSessionExpired);

  return () => {
    userManager.events.removeAccessTokenExpired(handleSessionExpired);
    userManager.events.removeUserSignedOut(handleSessionExpired);
    userManager.events.removeUserUnloaded(handleSessionExpired);
  };
}
