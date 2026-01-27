<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html>
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>Authorization Error</title>
    <style>
        :root{
            --bg0:#070A12; --bg1:#0B1020;
            --panel: rgba(255,255,255,.06);
            --stroke: rgba(255,255,255,.12);
            --text: rgba(255,255,255,.92);
            --muted: rgba(255,255,255,.62);
            --cyan:#22d3ee; --violet:#a78bfa; --pink:#fb7185;
            --shadow: 0 24px 80px rgba(0,0,0,.6);
            --radius: 18px;
            font-family: system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial;
        }
        body{
            margin:0; min-height:100vh; color:var(--text);
            background:
                    radial-gradient(1200px 800px at 20% -10%, rgba(34,211,238,0.14), transparent 60%),
                    radial-gradient(900px 700px at 90% 10%, rgba(167,139,250,0.12), transparent 60%),
                    linear-gradient(180deg, var(--bg0), var(--bg1) 40%, var(--bg0));
            display:grid; place-items:center; padding:24px;
        }
        .card{
            width:min(760px,100%);
            border-radius:var(--radius);
            background: linear-gradient(180deg, rgba(255,255,255,0.09), rgba(255,255,255,0.04));
            border:1px solid var(--stroke);
            box-shadow: var(--shadow);
            overflow:hidden;
            position:relative;
        }
        .card::before{
            content:""; position:absolute; inset:-1px; pointer-events:none; opacity:.9;
            background:
                    radial-gradient(520px 240px at 10% 0%, rgba(34,211,238,0.18), transparent 60%),
                    radial-gradient(520px 240px at 90% 10%, rgba(167,139,250,0.16), transparent 60%);
        }
        .inner{ position:relative; padding:22px 22px 18px 22px; }
        h1{ margin:0 0 10px 0; font-size:1.25rem; letter-spacing:-0.02em; }
        .meta{ color:var(--muted); margin-bottom:14px; }
        .pill{
            display:inline-flex; align-items:center; gap:10px;
            padding:8px 12px; border-radius:999px;
            border:1px solid rgba(251,113,133,0.25);
            background: rgba(251,113,133,0.10);
            box-shadow: 0 0 28px rgba(251,113,133,0.12);
            font-weight:700;
        }
        code{
            display:block;
            margin-top:12px;
            padding:12px 14px;
            border-radius:14px;
            border:1px solid rgba(255,255,255,0.12);
            background: rgba(0,0,0,0.25);
            color: rgba(255,255,255,0.86);
            overflow:auto;
            white-space:pre-wrap;
            font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
            font-size:0.9rem;
        }
        .actions{
            display:flex; gap:10px; margin-top:16px; flex-wrap:wrap;
        }
        a.btn{
            padding:10px 14px; border-radius:12px; text-decoration:none;
            border:1px solid rgba(255,255,255,0.12);
            background: rgba(255,255,255,0.06);
            color:var(--text);
        }
        a.btn.primary{
            border:1px solid rgba(34,211,238,0.25);
            background: linear-gradient(135deg, rgba(34,211,238,0.22), rgba(167,139,250,0.16));
            box-shadow: 0 0 30px rgba(34,211,238,0.12);
        }
    </style>
</head>
<body>
<div class="card">
    <div class="inner">
        <h1>Authorization failed</h1>
        <div class="meta">Your OAuth request was rejected before login could start.</div>

        <div class="pill">
            <span>error</span>
            <span style="opacity:.9">${error}</span>
        </div>

        <c:if test="${not empty description}">
            ${description}
        </c:if>

        <div class="actions">
            <a class="btn primary" href="/login">Go to Login</a>
            <a class="btn" href="/">Home</a>
        </div>
    </div>
</div>
</body>
</html>
