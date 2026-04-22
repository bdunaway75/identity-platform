import { isDevAuthBypassed } from "./devAuth";
import { userManager } from "./oidc";

let isRedirectingToLogout = false;
const SESSION_PLATFORM_CACHE_KEY = "platform-api-cache";
const PENDING_SUBSCRIPTION_CHECKOUT_KEY = "pending-subscription-checkout";

function isAuthRoute(pathname) {
  return pathname === "/app/login" || pathname === "/logout" || pathname === "/callback";
}

function clearExpiredSessionArtifacts() {
  if (typeof window === "undefined" || typeof window.sessionStorage === "undefined") {
    return;
  }

  try {
    window.sessionStorage.removeItem(SESSION_PLATFORM_CACHE_KEY);
    window.sessionStorage.removeItem(PENDING_SUBSCRIPTION_CHECKOUT_KEY);
  } catch {
    // Ignore storage cleanup failures and continue redirecting.
  }
}

function redirectToLogin() {
  if (typeof window === "undefined" || isRedirectingToLogout) {
    return;
  }

  isRedirectingToLogout = true;
  window.location.replace(isAuthRoute(window.location.pathname) ? "/app/login" : "/app/login?expired=1");
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

  clearExpiredSessionArtifacts();
  redirectToLogin();
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

  return () => {
    userManager.events.removeAccessTokenExpired(handleSessionExpired);
    userManager.events.removeUserSignedOut(handleSessionExpired);
  };
}
