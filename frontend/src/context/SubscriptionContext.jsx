import { createContext, useContext, useEffect, useMemo, useRef, useState } from "react";
import {
  clearSubscriptionTierCache,
  DEFAULT_TIER,
  fetchSubscriptionTier,
  subscribeToSubscriptionTierChanges,
} from "../services/subscription";
import { subscribeToDevAuthBypassChanges } from "../auth/devAuth";

const SubscriptionContext = createContext(null);
const DEFAULT_SUBSCRIPTION_SNAPSHOT = Object.freeze({
  tier: DEFAULT_TIER,
  tierName: DEFAULT_TIER,
  source: "unknown",
  tiers: [],
  isDemoUser: false,
  allowedNumberOfRegisteredClients: 0,
  allowedNumberOfGlobalUsers: 0,
  allowedNumberOfGlobalScopes: 0,
  allowedNumberOfGlobalAuthorities: 0,
  totalRegisteredClients: 0,
  totalUsers: 0,
  totalScopes: 0,
  totalAuthorities: 0,
  totalRoles: 0,
});

export function SubscriptionProvider({ children }) {
  const loadRequestIdRef = useRef(0);
  const [subscriptionSnapshot, setSubscriptionSnapshot] = useState(DEFAULT_SUBSCRIPTION_SNAPSHOT);
  const [status, setStatus] = useState("loading");
  const [error, setError] = useState("");

  async function loadTier(options = {}) {
    const { force = false } = options;
    const requestId = loadRequestIdRef.current + 1;
    loadRequestIdRef.current = requestId;
    setStatus("loading");
    setError("");

    try {
      const result = await fetchSubscriptionTier({ force });
      if (loadRequestIdRef.current !== requestId) {
        return;
      }
      setSubscriptionSnapshot(result);
      setStatus("ready");
    } catch (loadError) {
      if (loadRequestIdRef.current !== requestId) {
        return;
      }
      console.error("Subscription tier lookup failed", loadError);
      setSubscriptionSnapshot({
        ...DEFAULT_SUBSCRIPTION_SNAPSHOT,
        source: "fallback",
      });
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
    tier: subscriptionSnapshot.tier,
    tierName: subscriptionSnapshot.tierName,
    tiers: subscriptionSnapshot.tiers,
    isDemoUser: subscriptionSnapshot.isDemoUser,
    isPaid:
      subscriptionSnapshot.allowedNumberOfRegisteredClients > 0 ||
      subscriptionSnapshot.allowedNumberOfGlobalUsers > 0,
    limits: {
      registeredClients: subscriptionSnapshot.allowedNumberOfRegisteredClients,
      globalUsers: subscriptionSnapshot.allowedNumberOfGlobalUsers,
      globalScopes: subscriptionSnapshot.allowedNumberOfGlobalScopes,
      globalAuthorities: subscriptionSnapshot.allowedNumberOfGlobalAuthorities,
    },
    usage: {
      registeredClients: subscriptionSnapshot.totalRegisteredClients,
      globalUsers: subscriptionSnapshot.totalUsers,
      globalScopes: subscriptionSnapshot.totalScopes,
      globalAuthorities: subscriptionSnapshot.totalAuthorities,
      roles: subscriptionSnapshot.totalRoles,
    },
    status,
    error,
    source: subscriptionSnapshot.source,
    refreshTier: () => {
      clearSubscriptionTierCache();
      return loadTier({ force: true });
    },
  }), [
    error,
    status,
    subscriptionSnapshot,
  ]);

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
