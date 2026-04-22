import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { MantineProvider } from "@mantine/core";
import "@mantine/core/styles.css";
import "./index.css";
import "./theme.css";
import App from "./App";

ReactDOM.createRoot(document.getElementById("root")).render(
    <MantineProvider>
        <BrowserRouter>
            <App />
        </BrowserRouter>
    </MantineProvider>
);
