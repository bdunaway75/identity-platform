import {createContext, useContext, useEffect, useMemo, useState} from "react";
import {DEFAULT_TIER, fetchSubscriptionTier,} from "../services/subscription";
import {useNavBar} from "../../../app/context/NavBarContext.jsx";

const SubscriptionContext = createContext(null);
const DEFAULT_SUBSCRIPTION_SNAPSHOT = Object.freeze({
                                                        tier: DEFAULT_TIER,
                                                        tierName: DEFAULT_TIER,
                                                        source: "unknown",
                                                        tiers: [],
                                                        isDemoUser: false,
                                                        isAdmin: false,
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

function hasPlatformAccess(subscriptionSnapshot) {
    return Boolean(subscriptionSnapshot.isDemoUser) ||
        subscriptionSnapshot.allowedNumberOfRegisteredClients > 0 ||
        subscriptionSnapshot.allowedNumberOfGlobalUsers > 0;
}

export function SubscriptionProvider({children}) {
    const [subscriptionSnapshot, setSubscriptionSnapshot] = useState(DEFAULT_SUBSCRIPTION_SNAPSHOT);
    const [status, setStatus] = useState("loading");
    const [error, setError] = useState("");
    const {setTier, setNavItems, clearNavItems} = useNavBar();
    const isPaid = hasPlatformAccess(subscriptionSnapshot);

    async function loadTier(options = {}) {
        const {force = false} = options;
        setStatus("loading");
        setError("");

        try {
            const result = await fetchSubscriptionTier({force});
            setSubscriptionSnapshot(result);
            setStatus("ready");
        } catch (loadError) {
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
        setTier(subscriptionSnapshot.tier)
    }, [subscriptionSnapshot.tier]);

    useEffect(() => {
        clearNavItems();

        setNavItems((prev) => {
            const next = new Map([
                [
                    "/home",
                    {
                        to: "/home",
                        label: "Dashboard",
                        end: true,
                        matches: (pathname) => pathname === "/home",
                    },
                ],
                ...prev,
            ]);

            if (subscriptionSnapshot.isAdmin) {
                next.set("/admin", {
                    to: "/admin",
                    label: "Admin",
                    end: false,
                    matches: (pathname) => pathname.startsWith("/admin"),
                });
            }

            if (isPaid) {
                next.set("/clients", {
                    to: "/clients",
                    label: "Registry",
                    end: false,
                    matches: (pathname) =>
                        pathname.startsWith("/clients") && pathname !== "/clients/new",
                });
                next.set("/clients/new", {
                    to: "/clients/new",
                    label: "New Client",
                    end: false,
                    matches: (pathname) => pathname === "/clients/new",
                });
            } else {
                next.set("/clients", {
                    to: "/clients",
                    label: "Clients",
                    end: false,
                    matches: (pathname) => pathname.startsWith("/clients"),
                });
            }

            next.set("/subscriptions", {
                to: "/subscriptions",
                label: "Subscription",
                end: false,
                matches: (pathname) => pathname.startsWith("/subscriptions"),
            });
            next.set("/docs", {
                to: "/docs",
                label: "Documentation",
                end: false,
                matches: (pathname) => pathname.startsWith("/docs"),
            });
            next.set("/roadmap", {
                to: "/roadmap",
                label: "Road map",
                end: true,
                matches: (pathname) => pathname === "/roadmap",
            });

            return next;
        });
    }, [clearNavItems, isPaid, setNavItems, subscriptionSnapshot.isAdmin]);


    const value = useMemo(() => ({
        tier: subscriptionSnapshot.tier,
        tierName: subscriptionSnapshot.tierName,
        tiers: subscriptionSnapshot.tiers,
        isDemoUser: subscriptionSnapshot.isDemoUser,
        isAdmin: subscriptionSnapshot.isAdmin,
        isPaid,
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
    }), [
                              error,
                              isPaid,
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
