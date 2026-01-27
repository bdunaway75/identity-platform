<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Sign in</title>

    <style>
        :root {
            --bg0: #0b0b0b;
            --bg1: #111111;
            --card: rgba(20, 20, 20, 0.92);
            --border: rgba(255, 255, 255, 0.10);
            --grid: rgba(255, 255, 255, 0.035);

            --text: #e5e7eb;
            --muted: #9ca3af;

            --ring: rgba(37, 99, 235, 0.55);
            --accent0: #2563eb;
            --accent1: #1d4ed8;

            --danger: #f87171;
            --shadow: rgba(0, 0, 0, 0.65);
        }

        html, body {
            margin: 0;
            padding: 0;
            height: 100%;
            background: radial-gradient(circle at top, #1a1a1a, var(--bg0));
            font-family: system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif;
            color: var(--text);
        }

        body {
            display: grid;
            place-items: center;
            overflow: hidden;
        }

        /* animated grid background */
        body::before {
            content: "";
            position: absolute;
            inset: 0;
            background-image:
                    linear-gradient(var(--grid) 1px, transparent 1px),
                    linear-gradient(90deg, var(--grid) 1px, transparent 1px);
            background-size: 40px 40px;
            animation: gridMove 18s linear infinite;
            pointer-events: none;
        }

        @keyframes gridMove {
            from { transform: translateY(0); }
            to   { transform: translateY(40px); }
        }

        /* subtle glow behind the card */
        .glow {
            position: absolute;
            width: 520px;
            height: 520px;
            border-radius: 50%;
            background: radial-gradient(circle, rgba(37,99,235,0.18), transparent 60%);
            filter: blur(2px);
            z-index: 0;
        }

        .login-card {
            position: relative;
            width: 360px;
            padding: 2rem;
            border-radius: 10px;
            background: var(--card);
            border: 1px solid var(--border);
            box-shadow: 0 0 46px var(--shadow);
            z-index: 1;
            backdrop-filter: blur(6px);
        }

        .header {
            margin-bottom: 1.25rem;
            text-align: left;
        }

        h1 {
            margin: 0 0 0.35rem;
            font-size: 1.55rem;
            letter-spacing: 0.01em;
        }

        .subtitle {
            font-size: 0.92rem;
            color: var(--muted);
            line-height: 1.35;
        }

        .error {
            background: rgba(248, 113, 113, 0.10);
            border: 1px solid rgba(248, 113, 113, 0.25);
            color: var(--danger);
            font-size: 0.86rem;
            padding: 0.65rem 0.75rem;
            border-radius: 8px;
            margin: 0.9rem 0 1.1rem;
        }

        .field {
            margin-bottom: 1rem;
        }

        label {
            display: block;
            font-size: 0.85rem;
            margin-bottom: 0.35rem;
            color: #cbd5f5;
        }

        input[type="text"],
        input[type="email"],
        input[type="password"] {
            width: 100%;
            box-sizing: border-box;
            padding: 0.7rem 0.75rem;
            border-radius: 8px;
            border: 1px solid rgba(255, 255, 255, 0.14);
            background: #0a0a0a;
            color: var(--text);
            transition: border-color 140ms ease, box-shadow 140ms ease;
        }

        input::placeholder {
            color: rgba(156, 163, 175, 0.55);
        }

        input:focus {
            outline: none;
            border-color: rgba(37, 99, 235, 0.65);
            box-shadow: 0 0 0 4px var(--ring);
        }

        .actions {
            margin-top: 0.25rem;
        }

        button {
            width: 100%;
            padding: 0.78rem;
            background: linear-gradient(135deg, var(--accent0), var(--accent1));
            border: none;
            border-radius: 10px;
            color: white;
            font-weight: 650;
            cursor: pointer;
            letter-spacing: 0.02em;
            transition: transform 120ms ease, filter 120ms ease;
        }

        button:hover {
            filter: brightness(1.08);
        }

        button:active {
            transform: translateY(1px);
        }

        .divider {
            height: 1px;
            background: rgba(255,255,255,0.08);
            margin: 1.25rem 0 1rem;
        }

        .footer {
            font-size: 0.78rem;
            color: #6b7280;
            text-align: center;
            line-height: 1.3;
        }

        .footer a {
            color: rgba(229,231,235,0.85);
            text-decoration: none;
            border-bottom: 1px dashed rgba(229,231,235,0.35);
        }

        .footer a:hover {
            border-bottom-color: rgba(229,231,235,0.75);
        }
    </style>
</head>

<body>
<div class="glow" aria-hidden="true"></div>

<form class="login-card" method="post" action="/login" autocomplete="on">
    <div class="header">
        <h1>Sign in</h1>
        <div class="subtitle">Access the internal system</div>
    </div>

    <%-- show error if redirected back with ?error --%>
    <% if (request.getParameter("error") != null) { %>
    <div class="error">Invalid email or password.</div>
    <% } %>

    <%-- IMPORTANT: clientId must post back so your RegisterDto gets it. --%>
    <%-- Prefer model: ${registerDto.clientId}. Fallback: query param client_id. --%>
    <input type="hidden" name="clientId"
           value="${not empty registerDto.clientId ? registerDto.clientId : param.client_id}" />

    <div class="field">
        <label for="email">Email</label>
        <input
                id="email"
                name="email"
                type="email"
                inputmode="email"
                autocomplete="username"
                placeholder="you@example.com"
                required
                autofocus
        />
    </div>

    <div class="field">
        <label for="password">Password</label>
        <input
                id="password"
                name="password"
                type="password"
                autocomplete="current-password"
                placeholder="••••••••"
                required
        />
    </div>

    <div class="actions">
        <button type="submit">Login</button>
    </div>

    <div class="divider"></div>

    <div class="footer">
        Authorized users only<br/>
        <a href="/signUp">Create an account</a>
    </div>
</form>

</body>
</html>
