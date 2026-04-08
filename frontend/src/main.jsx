import React from "react";
import "./theme.css";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { MantineProvider } from "@mantine/core";
import App from "./App";
import '@mantine/core/styles.css';

ReactDOM.createRoot(document.getElementById("root")).render(
    <MantineProvider>
        <BrowserRouter>
            <App />
        </BrowserRouter>
    </MantineProvider>
);
