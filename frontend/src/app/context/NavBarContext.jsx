import { createContext, useCallback, useContext, useState } from "react";

const NavBarContext = createContext(null);
const DEFAULT_NAV_ITEMS = new Map([
    [
        "/",
        {
            to: "/",
            label: "Welcome",
            end: true,
            matches: (pathname) => pathname === "/" || pathname === "/welcome",
        },
    ],
    [
        "/docs",
        {
            to: "/docs",
            label: "Documentation",
            end: false,
            matches: (pathname) => pathname.startsWith("/docs"),
        },
    ],
    [
        "/roadmap",
        {
            to: "/roadmap",
            label: "Road map",
            end: true,
            matches: (pathname) => pathname === "/roadmap",
        },
    ],
]);

function createDefaultNavItems() {
    return new Map(DEFAULT_NAV_ITEMS);
}

export default function NavBarProvider({ children, initialIsLoggedIn = false }) {
    const [navItems, setNavItems] = useState(createDefaultNavItems);

    const [tier, setTier] = useState("Free");
    const [isLoggedIn, setIsLoggedIn] = useState(initialIsLoggedIn);
    const clearNavItems = useCallback(() => {
        setNavItems(createDefaultNavItems());
    }, []);

    return (
        <NavBarContext.Provider
            value={{
                navItems,
                setNavItems,
                clearNavItems,
                tier,
                setTier,
                isLoggedIn,
                setIsLoggedIn
            }}
        >
            {children}
        </NavBarContext.Provider>
    );
}

export function useNavBar() {
    const context = useContext(NavBarContext);

    if (!context) {
        throw new Error("useNavBar must be used within NavBarProvider.");
    }

    return context;
}
