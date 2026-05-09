import {useEffect} from "react";
import {Outlet} from "react-router-dom";

export default function NonAuthPageLayout() {

    useEffect(() => {
        const html = document.documentElement;
        const body = document.body;
        const themeColorMeta = document.querySelector('meta[name="theme-color"]');
        const previousThemeColor = themeColorMeta?.getAttribute("content") ?? null;

        html.classList.add("login-page-theme");
        body.classList.add("login-page-theme");
        themeColorMeta?.setAttribute("content", "#091112");

        return () => {
            html.classList.remove("login-page-theme");
            body.classList.remove("login-page-theme");

            if (previousThemeColor) {
                themeColorMeta?.setAttribute("content", previousThemeColor);
            }
        };
    }, []);

    return <Outlet/>
}