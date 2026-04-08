const DEV_AUTH_STORAGE_KEY = "local-dev-auth-enabled";
const DEV_AUTH_EVENT = "codex:dev-auth-changed";

export function isLocalDevHost() {
  return typeof window !== "undefined" && (
    window.location.hostname === "localhost" ||
    window.location.hostname === "127.0.0.1"
  );
}

export function isDevAuthBypassAvailable() {
  return Boolean(import.meta.env.DEV && isLocalDevHost());
}

export function isDevAuthBypassed() {
  if (!isDevAuthBypassAvailable() || typeof window === "undefined") {
    return false;
  }

  return window.localStorage.getItem(DEV_AUTH_STORAGE_KEY) === "true";
}

export function enableDevAuthBypass() {
  if (typeof window !== "undefined") {
    window.localStorage.setItem(DEV_AUTH_STORAGE_KEY, "true");
    window.dispatchEvent(new CustomEvent(DEV_AUTH_EVENT, { detail: { enabled: true } }));
  }
}

export function disableDevAuthBypass() {
  if (typeof window !== "undefined") {
    window.localStorage.removeItem(DEV_AUTH_STORAGE_KEY);
    window.dispatchEvent(new CustomEvent(DEV_AUTH_EVENT, { detail: { enabled: false } }));
  }
}

export function subscribeToDevAuthBypassChanges(callback) {
  if (typeof window === "undefined") {
    return () => {};
  }

  const handleChange = () => {
    callback(isDevAuthBypassed());
  };

  const handleStorage = (event) => {
    if (event.key === DEV_AUTH_STORAGE_KEY) {
      callback(isDevAuthBypassed());
    }
  };

  window.addEventListener(DEV_AUTH_EVENT, handleChange);
  window.addEventListener("storage", handleStorage);

  return () => {
    window.removeEventListener(DEV_AUTH_EVENT, handleChange);
    window.removeEventListener("storage", handleStorage);
  };
}
