<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="t" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ attribute name="title" required="false" %>
<%@ attribute name="head" fragment="true" required="false" %>

<t:layout title="${title}">
    <jsp:attribute name="head">
        <jsp:invoke fragment="head"/>
    </jsp:attribute>
  <jsp:body>
    <style>
      :root{
        --bg1:#0b1523; --bg2:#0f2233; --bg3:#12334b;
        --card:#ffffff; --ink:#0f1320; --muted:#5c6b80;
        --brand1:#167ea7; --brand2:#1fb6d5; --brand3:#79e3ff;
        --accent:#1dd1a1;
      }

      html, body { height:100%; }

      /* Page background (subtle, non-choppy) */
      .page {
        min-height:100vh;
        display:grid;
        place-items:center;
        background:
                radial-gradient(1200px 800px at 20% 10%, rgba(31,182,213,.18), transparent 60%),
                radial-gradient(1000px 700px at 80% 80%, rgba(18,51,75,.25), transparent 65%),
                linear-gradient(180deg, var(--bg1), var(--bg2));
        position:relative;
        overflow:hidden;
      }
      .page::before {
        content:"";
        position:absolute; inset:0;
        background-image:
                radial-gradient(circle at 10% 15%, rgba(255,255,255,.10) 0 1px, transparent 1.2px),
                radial-gradient(circle at 35% 40%, rgba(255,255,255,.08) 0 1px, transparent 1.2px),
                radial-gradient(circle at 60% 25%, rgba(255,255,255,.08) 0 1px, transparent 1.2px),
                radial-gradient(circle at 80% 70%, rgba(255,255,255,.10) 0 1px, transparent 1.2px),
                radial-gradient(circle at 30% 80%, rgba(255,255,255,.08) 0 1px, transparent 1.2px);
        opacity:.25;
        pointer-events:none;
      }
      /* faint connective-lines pattern */
      .page::after{
        content:"";
        position:absolute; inset:-200% -200%;
        background-image:
                linear-gradient(transparent 97%, rgba(255,255,255,.045) 97%),
                linear-gradient(90deg, transparent 97%, rgba(255,255,255,.04) 97%);
        background-size:30px 30px, 30px 30px;
        opacity:.15;
        transform:translateZ(0);
        /* very slow pan to avoid choppiness */
        animation:gridDrift 60s linear infinite;
      }
      @keyframes gridDrift {
        from { transform: translate(-1%, -1%) }
        to   { transform: translate(0, 0) }
      }

      /* Card */
      .login-card{
        width:min(92vw, 600px);
        display:grid;
        grid-template-columns: 1fr;
        gap:1.25rem;
        background: color-mix(in srgb, var(--card) 92%, #fff 8%);
        border:1px solid rgba(255,255,255,.35);
        border-radius:16px;
        padding:2rem 2rem 2.2rem;
        box-shadow:
                0 10px 30px rgba(0,0,0,.18),
                inset 0 1px 0 rgba(255,255,255,.6);
        color:var(--ink);
        position:relative;
        z-index:2;
      }

      .brand-graphic{
        display:flex; align-items:center; justify-content:center;
        padding-bottom:.25rem;
      }

      .float {
        will-change: transform;
        animation: float 12s ease-in-out infinite;
      }
      @keyframes float {
        0%,100% { transform: translate3d(0,0,0) }
        50%     { transform: translate3d(0,-6px,0) }
      }

      .form{
        display:grid; gap:.85rem;
      }
      .label{ font-size:.92rem; color:var(--muted); margin-bottom:.25rem; display:block;}
      .input{
        width:100%;
        font: 500 1rem/1.2 system-ui, -apple-system, Segoe UI, Roboto, "Helvetica Neue", Arial;
        color:var(--ink);
        background:#fff;
        border:1px solid rgba(16,22,40,.15);
        border-radius:10px;
        padding:.85rem .9rem;
        outline:none;
        box-shadow: 0 0 0 0 rgba(31,182,213,0);
        transition: box-shadow .2s ease, border-color .2s ease, transform .05s ease;
      }
      .input:focus{
        border-color: color-mix(in srgb, var(--brand2) 60%, #fff 40%);
        box-shadow: 0 0 0 4px color-mix(in srgb, var(--brand2) 16%, #fff 84%);
      }

      .btn{
        width: 50%;
        margin: .5rem auto 0;
        appearance:none;
        border:0;
        border-radius:12px;
        padding:.95rem 1.1rem;
        background: linear-gradient(180deg, var(--brand2), var(--brand1));
        color:#fff; font-weight:700; letter-spacing:.2px;
        cursor:pointer;
        transition: transform .06s ease, filter .2s ease, box-shadow .2s ease;
      }
      .btn:hover{ filter: brightness(1.05); }
      .btn:active{ transform: translateY(1px); }

      .muted{
        color:var(--muted);
        font-size:.9rem;
      }

      .alerts{ margin-top:.5rem; }
      .alert{
        border-radius:10px; padding:.6rem .8rem; font-size:.95rem;
      }
      .alert-danger{
        color:#842029; background:#f8d7da; border:1px solid #f5c2c7;
      }
      .alert-success{
        color:#0f5132; background:#d1e7dd; border:1px solid #badbcc;
      }

      /* Respect reduced motion */
      @media (prefers-reduced-motion: reduce) {
        .float, .page::after { animation: none !important; }
      }

      /* Small screens */
      @media (max-width: 560px){
        .login-card{ padding:1.4rem; }
      }
    </style>

    <div class="page">
      <div class="login-card">
        <div class="brand-graphic">
          <svg class="float" role="img" aria-label="Security shield with servers, gears, and data nodes"
               width="220" height="190" viewBox="0 0 520 320" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <linearGradient id="shieldGrad" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%"  stop-color="#1fb6d5"/>
                <stop offset="60%" stop-color="#167ea7"/>
                <stop offset="100%" stop-color="#145c7f"/>
              </linearGradient>
              <linearGradient id="rackGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#e9f7fb"/>
                <stop offset="100%" stop-color="#cfe9f4"/>
              </linearGradient>
              <filter id="soft" x="-20%" y="-20%" width="140%" height="140%">
                <feGaussianBlur stdDeviation="4" result="b"/>
                <feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge>
              </filter>
            </defs>

            <path d="M260,16 L440,60 L440,178
                     C440,232 344,298 260,324
                     C176,298 80,232 80,178
                     L80,60 Z"
                  fill="url(#shieldGrad)" stroke="#0f2a3a" stroke-width="4" filter="url(#soft)"/>

            <g opacity=".25" stroke="#e9fbff" stroke-width="3">
              <rect x="130" y="90"  rx="8" ry="8" width="62" height="38" fill="none"/>
              <rect x="328" y="82"  rx="8" ry="8" width="62" height="38" fill="none"/>
              <rect x="190" y="160" rx="8" ry="8" width="62" height="38" fill="none"/>
              <rect x="300" y="160" rx="8" ry="8" width="62" height="38" fill="none"/>
              <line x1="192" y1="178" x2="160" y2="109"/>
              <line x1="332" y1="101" x2="260" y2="179"/>
              <line x1="224" y1="179" x2="300" y2="179"/>
            </g>

            <g transform="translate(150,110)">
              <rect x="0" y="0" width="220" height="42" rx="10" fill="url(#rackGrad)" stroke="#98cfe2"/>
              <rect x="0" y="48" width="220" height="42" rx="10" fill="url(#rackGrad)" stroke="#98cfe2"/>
              <rect x="0" y="96" width="220" height="42" rx="10" fill="url(#rackGrad)" stroke="#98cfe2"/>

              <g fill="#0f6b8a">
                <circle cx="22" cy="21" r="6"/>
                <circle cx="22" cy="69" r="6"/>
                <circle cx="22" cy="117" r="6"/>
              </g>
              <g fill="#0f6b8a">
                <rect x="52"  y="16" width="64" height="10" rx="4"/>
                <rect x="52"  y="64" width="64" height="10" rx="4"/>
                <rect x="52"  y="112" width="64" height="10" rx="4"/>
                <rect x="132" y="16" width="10" height="10" rx="2"/>
                <rect x="148" y="16" width="10" height="10" rx="2"/>
                <rect x="132" y="64" width="10" height="10" rx="2"/>
                <rect x="148" y="64" width="10" height="10" rx="2"/>
                <rect x="132" y="112" width="10" height="10" rx="2"/>
                <rect x="148" y="112" width="10" height="10" rx="2"/>
              </g>
            </g>

            <g transform="translate(400,190)">
              <g transform="translate(0,0)">
                <circle cx="0" cy="0" r="28" fill="#1fb6d5" stroke="#0f6b8a" stroke-width="4"/>
                <g fill="#1fb6d5" stroke="#0f6b8a" stroke-width="3">
                  <rect x="-4" y="-44" width="8" height="12" rx="2"/>
                  <rect x="-4" y="32"  width="8" height="12" rx="2"/>
                  <rect x="-44" y="-4" width="12" height="8" rx="2"/>
                  <rect x="32"  y="-4" width="12" height="8" rx="2"/>
                  <rect x="-32" y="-32" width="10" height="8" transform="rotate(-45)"/>
                  <rect x="22"  y="-32" width="10" height="8" transform="rotate(45)"/>
                  <rect x="-32" y="24"  width="10" height="8" transform="rotate(45)"/>
                  <rect x="22"  y="24"  width="10" height="8" transform="rotate(-45)"/>
                </g>
                <circle cx="0" cy="0" r="10" fill="#0f6b8a"/>
                <animateTransform attributeName="transform" type="rotate"
                                  from="0 0 0" to="360 0 0" dur="22s" repeatCount="indefinite"/>
              </g>

              <g transform="translate(42,38)">
                <circle cx="0" cy="0" r="16" fill="#79e3ff" stroke="#0f6b8a" stroke-width="3"/>
                <g fill="#79e3ff" stroke="#0f6b8a" stroke-width="2">
                  <rect x="-3" y="-26" width="6" height="8" rx="2"/>
                  <rect x="-3" y="18"  width="6" height="8" rx="2"/>
                  <rect x="-26" y="-3" width="8" height="6" rx="2"/>
                  <rect x="18"  y="-3" width="8" height="6" rx="2"/>
                  <rect x="-18" y="-18" width="6" height="6" transform="rotate(-45)"/>
                  <rect x="12"  y="-18" width="6" height="6" transform="rotate(45)"/>
                  <rect x="-18" y="12"  width="6" height="6" transform="rotate(45)"/>
                  <rect x="12"  y="12"  width="6" height="6" transform="rotate(-45)"/>
                </g>
                <circle cx="0" cy="0" r="6" fill="#0f6b8a"/>
                <animateTransform attributeName="transform" type="rotate"
                                  from="360 0 0" to="0 0 0" dur="12s" repeatCount="indefinite"/>
              </g>
            </g>
          </svg>
        </div>
        <jsp:doBody/>
      </div>
    </div>
  </jsp:body>
  </t:layout>
