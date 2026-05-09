import {useEffect, useState} from "react";
import {Navigate, Outlet, Route, Routes, useLocation} from "react-router-dom";
import AppShellLayout from "./layout/AppShellLayout.jsx";
import Login from "../features/auth/pages/Login";
import DemoAccess from "../features/auth/pages/DemoAccess";
import Callback from "../features/auth/pages/Callback";
import Logout from "../features/auth/pages/Logout";
import RequireAuth from "../features/auth/routes/RequireAuth";
import Home from "../features/home/pages/Home";
import Clients from "../features/clients/pages/Clients";
import ClientsAccessPage from "../features/clients/pages/ClientsAccessPage";
import ClientWorkspace from "../features/clients/pages/ClientWorkspace";
import CreateClient from "../features/clients/pages/CreateClient";
import ClientUpdateSuccess from "../features/clients/pages/ClientUpdateSuccess";
import ClientUserDetail from "../features/clients/pages/ClientUserDetail";
import Subscriptions from "../features/subscriptions/pages/Subscriptions";
import SubscriptionCheckoutSuccess from "../features/subscriptions/pages/SubscriptionCheckoutSuccess";
import SubscriptionCheckoutCancel from "../features/subscriptions/pages/SubscriptionCheckoutCancel";
import Docs from "../features/docs/pages/Docs";
import RoadMap from "../features/docs/pages/RoadMap.jsx";
import Admin from "../features/admin/pages/Admin";
import NonAuthPageLayout from "./layout/NonAuthPageLayout.jsx";
import {SubscriptionProvider, useSubscription} from "../features/subscriptions/context/SubscriptionContext";
import { userManager } from "../features/auth/services/oidc";
import {registerAuthSessionHandlers} from "../features/auth/services/session";
import "bootstrap/dist/css/bootstrap.min.css";
import NavBarProvider from "./context/NavBarContext.jsx";
import Welcome from "../features/home/pages/Welcome.jsx";
import Spinner from "react-bootstrap/Spinner";

/**
 * Acts as a wrapper to all child paths, any matching child route is provided AuthSessionHandlers, and the subscription context is loaded upon mount
 * @returns {JSX.Element}
 * @constructor
 */
function SubscribedAppShell() {
    useEffect(() => registerAuthSessionHandlers(), []);

    return (
        <NavBarProvider initialIsLoggedIn={true}>
            <SubscriptionProvider>
                <AppShellLayout/>
            </SubscriptionProvider>
        </NavBarProvider>
    );
}

function BaseAppShell() {
    return (
        <NavBarProvider initialIsLoggedIn={false}>
            <AppShellLayout/>
        </NavBarProvider>
    );
}

function AdaptiveShell() {
    const [status, setStatus] = useState("checking");

    useEffect(() => {
        let isMounted = true;

        userManager.getUser()
            .then((user) => {
                if (!isMounted) {
                    return;
                }

                setStatus(user && !user.expired ? "authed" : "unauth");
            })
            .catch(() => {
                if (isMounted) {
                    setStatus("unauth");
                }
            });

        return () => {
            isMounted = false;
        };
    }, []);

    if (status === "checking") {
        return <Spinner animation="grow" />;
    }

    if (status === "authed") {
        return <SubscribedAppShell/>;
    }
    return <BaseAppShell/>;
}

/**
 * Prevent unpaid users from accessing privileged tabs, when an attempt is made they are redirected back to the source path.
 * @returns {JSX.Element}
 * @constructor
 */
function ClientsRouteGate() {
    const location = useLocation();
    const {status, isPaid} = useSubscription();

    if (status === "loading") {
        return <ClientWorkspace/>;
    }

    if (!isPaid) {
        if (location.pathname !== "/clients") {
            return <Navigate
                to="/clients"
                replace
            />;
        }

        return <ClientsAccessPage/>;
    }

    return <ClientWorkspace><Outlet/></ClientWorkspace>;
}

export default function App() {
    return (
        <Routes>
            <Route element={<NonAuthPageLayout/>}>
                <Route
                    path="/app/login"
                    element={<Login/>}
                />
                <Route
                    path="/demo-access"
                    element={<DemoAccess/>}
                />
                <Route
                    path="/callback"
                    element={<Callback/>}
                />
                <Route
                    path="/logout"
                    element={<Logout/>}
                />
            </Route>

            <Route element={<AdaptiveShell/>}>
                <Route
                    path="/"
                    element={<Welcome/>}
                />
                <Route
                    path="/welcome"
                    element={<Welcome/>}
                />
                <Route
                    path="/docs"
                    element={<Docs/>}
                />
                <Route
                    path="/roadmap"
                    element={<RoadMap/>}
                />
            </Route>

            <Route element={<RequireAuth/>}>
                <Route element={<SubscribedAppShell/>}>
                    <Route
                        path="/home"
                        element={<Home/>}
                    />
                    <Route
                        path="/success"
                        element={<ClientUpdateSuccess/>}
                    />
                    <Route
                        path="/clients"
                        element={<ClientsRouteGate/>}
                    >
                        <Route
                            index
                            element={<Clients/>}
                        />
                        <Route
                            path="new"
                            element={<CreateClient/>}
                        />
                        <Route
                            path=":registeredClientId/edit"
                            element={<CreateClient/>}
                        />
                        <Route
                            path=":registeredClientId/users/:clientUserId"
                            element={<ClientUserDetail/>}
                        />
                    </Route>
                    <Route
                        path="/subscriptions"
                        element={<Subscriptions/>}
                    />
                    <Route
                        path="/subscriptions/success"
                        element={<SubscriptionCheckoutSuccess/>}
                    />
                    <Route
                        path="/subscriptions/cancel"
                        element={<SubscriptionCheckoutCancel/>}
                    />
                    <Route
                        path="/admin"
                        element={<Admin/>}
                    />
                </Route>
            </Route>
        </Routes>
    );
}
