import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { MantineProvider } from "@mantine/core";
import "@mantine/core/styles.css";
import "./index.css";
import "./theme.css";
import App from "./App";

function readSafeAreaInsets() {
    if (typeof window === "undefined" || typeof document === "undefined") {
        return { top: 0, right: 0, bottom: 0, left: 0 };
    }

    const probe = document.createElement("div");
    probe.style.position = "fixed";
    probe.style.top = "0";
    probe.style.left = "0";
    probe.style.visibility = "hidden";
    probe.style.pointerEvents = "none";
    probe.style.paddingTop = "env(safe-area-inset-top)";
    probe.style.paddingRight = "env(safe-area-inset-right)";
    probe.style.paddingBottom = "env(safe-area-inset-bottom)";
    probe.style.paddingLeft = "env(safe-area-inset-left)";
    document.body.appendChild(probe);

    const styles = window.getComputedStyle(probe);
    const safeArea = {
        top: Math.round(Number.parseFloat(styles.paddingTop) || 0),
        right: Math.round(Number.parseFloat(styles.paddingRight) || 0),
        bottom: Math.round(Number.parseFloat(styles.paddingBottom) || 0),
        left: Math.round(Number.parseFloat(styles.paddingLeft) || 0),
    };

    probe.remove();
    return safeArea;
}

function readViewportDebugOverrides() {
    if (typeof window === "undefined") {
        return {
            safeAreaTop: null,
            safeAreaRight: null,
            safeAreaBottom: null,
            safeAreaLeft: null,
            bottomBrowserOffset: null,
        };
    }

    const searchParams = new URLSearchParams(window.location.search);
    const parsePx = (key) => {
        const rawValue = searchParams.get(key);
        if (rawValue == null || rawValue.trim() === "") {
            return null;
        }

        const parsedValue = Number.parseFloat(rawValue);
        return Number.isFinite(parsedValue) ? Math.max(0, Math.round(parsedValue)) : null;
    };

    return {
        safeAreaTop: parsePx("safeTop"),
        safeAreaRight: parsePx("safeRight"),
        safeAreaBottom: parsePx("safeBottom"),
        safeAreaLeft: parsePx("safeLeft"),
        bottomBrowserOffset: parsePx("browserBottom"),
    };
}

function ViewportMetricsBridge() {
    React.useEffect(() => {
        if (typeof window === "undefined" || typeof document === "undefined") {
            return undefined;
        }

        const root = document.documentElement;

        const updateViewportVars = () => {
            const visualViewport = window.visualViewport;
            const layoutHeight = window.innerHeight || root.clientHeight || 0;
            const visibleHeight = visualViewport?.height ?? layoutHeight;
            const visibleOffsetTop = visualViewport?.offsetTop ?? 0;
            const detectedSafeArea = readSafeAreaInsets();
            const overrides = readViewportDebugOverrides();
            const bottomBrowserOffset = overrides.bottomBrowserOffset ?? Math.max(
                0,
                Math.round(layoutHeight - (visibleHeight + visibleOffsetTop))
            );
            const effectiveSafeArea = {
                top: overrides.safeAreaTop ?? detectedSafeArea.top,
                right: overrides.safeAreaRight ?? detectedSafeArea.right,
                bottom: overrides.safeAreaBottom ?? detectedSafeArea.bottom,
                left: overrides.safeAreaLeft ?? detectedSafeArea.left,
            };

            root.style.setProperty("--app-layout-viewport-height", `${layoutHeight}px`);
            root.style.setProperty("--app-visible-viewport-height", `${Math.round(visibleHeight)}px`);
            root.style.setProperty("--app-bottom-browser-offset", `${bottomBrowserOffset}px`);
            root.style.setProperty("--app-safe-area-top", `${effectiveSafeArea.top}px`);
            root.style.setProperty("--app-safe-area-right", `${effectiveSafeArea.right}px`);
            root.style.setProperty("--app-safe-area-bottom", `${effectiveSafeArea.bottom}px`);
            root.style.setProperty("--app-safe-area-left", `${effectiveSafeArea.left}px`);
        };

        updateViewportVars();
        window.addEventListener("resize", updateViewportVars, { passive: true });
        window.addEventListener("orientationchange", updateViewportVars, { passive: true });
        window.visualViewport?.addEventListener("resize", updateViewportVars, { passive: true });
        window.visualViewport?.addEventListener("scroll", updateViewportVars, { passive: true });

        return () => {
            window.removeEventListener("resize", updateViewportVars);
            window.removeEventListener("orientationchange", updateViewportVars);
            window.visualViewport?.removeEventListener("resize", updateViewportVars);
            window.visualViewport?.removeEventListener("scroll", updateViewportVars);
        };
    }, []);

    return null;
}

ReactDOM.createRoot(document.getElementById("root")).render(
    <MantineProvider>
        <BrowserRouter>
            <ViewportMetricsBridge />
            <App />
        </BrowserRouter>
    </MantineProvider>
);
