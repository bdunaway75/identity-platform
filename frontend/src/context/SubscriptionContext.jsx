import { createContext, useContext, useEffect, useMemo, useState } from "react";
import {
  clearSubscriptionTierCache,
  DEFAULT_TIER,
  PAID_TIER,
  fetchSubscriptionTier,
  subscribeToSubscriptionTierChanges,
} from "../services/subscription";
import { subscribeToDevAuthBypassChanges } from "../auth/devAuth";

const SubscriptionContext = createContext(null);

export function SubscriptionProvider({ children }) {
  const [tier, setTier] = useState(DEFAULT_TIER);
  const [status, setStatus] = useState("loading");
  const [error, setError] = useState("");
  const [source, setSource] = useState("unknown");

  async function loadTier(options = {}) {
    const { force = false } = options;
    setStatus("loading");
    setError("");

    try {
      const result = await fetchSubscriptionTier({ force });
      setTier(result.tier);
      setSource(result.source);
      setStatus("ready");
    } catch (loadError) {
      console.error("Subscription tier lookup failed", loadError);
      setTier(DEFAULT_TIER);
      setSource("fallback");
      setError(loadError.message || "Unable to load subscription tier.");
      setStatus("error");
    }
  }

  useEffect(() => {
    loadTier();
  }, []);

  useEffect(() => {
    return subscribeToDevAuthBypassChanges(() => {
      clearSubscriptionTierCache();
      loadTier({ force: true });
    });
  }, []);

  useEffect(() => {
    return subscribeToSubscriptionTierChanges(() => {
      clearSubscriptionTierCache();
      loadTier({ force: true });
    });
  }, []);

  const value = useMemo(() => ({
    tier,
    isPaid: tier === PAID_TIER,
    status,
    error,
    source,
    refreshTier: () => {
      clearSubscriptionTierCache();
      return loadTier({ force: true });
    },
  }), [tier, status, error, source]);

  return (
    <SubscriptionContext.Provider value={value}>
      {children}
    </SubscriptionContext.Provider>
  );
}

export function useSubscription() {
  const context = useContext(SubscriptionContext);

  if (!context) {
    throw new Error("useSubscription must be used inside SubscriptionProvider.");
  }

  return context;
}
