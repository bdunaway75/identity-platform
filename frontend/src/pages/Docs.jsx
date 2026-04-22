import "./Docs.css";
import { useState } from "react";

const docSections = [
  {
    id: "overview",
    number: "1.",
    kicker: "Overview",
    title: "What Identity Platform does",
    paragraphs: [
      "Identity Platform is a hosted OAuth 2.0 and OpenID Connect auth server for teams that need to register apps, manage users, and work with tokens in one place.",
      "It keeps platform users separate from client users, so the people who manage apps are not mixed with the people who sign in to them.",
    ],
    highlights: [
      "Hosted OAuth 2.0 and OpenID Connect auth server",
      "Platform-user and client-user separation",
      "Client registry and user management",
      "Token viewing and invalidation tools",
    ],
  },
  {
    id: "protocols",
    number: "2.",
    kicker: "Protocols",
    title: "How apps connect",
    paragraphs: [
      "Apps connect with OAuth 2.0 and OpenID Connect. The server supports authorization code flow, PKCE for public clients, JWT access tokens, consent settings, and redirect URI validation.",
      "You can use normal OIDC client libraries instead of building a custom login flow around the service.",
    ],
    highlights: [
      "Authorization code flow and PKCE support",
      "JWT access-token issuance",
      "Registered redirect and logout URI validation",
      "Client-level token settings and grant-type control",
    ],
  },
  {
    id: "security",
    number: "3.",
    kicker: "Security",
    title: "Keys, tokens, and passwords",
    paragraphs: [
      "Passwords are stored as Argon2 hashes, issued tokens are handled through Spring Authorization Server and Spring Security, and protected platform APIs are guarded with JWT validation and backend authorization checks.",
      "JWT signing keys are rotated on a schedule. During rollover, older verification keys stay in the JWK set for a while so token checks keep working, and retired keys are removed later.",
      "Security checks stay on the backend, so ownership rules, role checks, and plan limits are not left to the frontend alone.",
    ],
    highlights: [
      "Argon2 password hashing",
      "Automatic RSA JWT signing-key rotation",
      "Published JWK set with inactive verification keys during rollover",
      "Backend JWT validation and authorization enforcement",
    ],
  },
  {
    id: "boundaries",
    number: "4.",
    kicker: "Boundaries",
    title: "Ownership rules and token controls",
    paragraphs: [
      "Platform users can only manage registered clients they own. Client-user management is scoped to those owned clients, and token inspection or invalidation flows are restricted to those same ownership boundaries.",
      "That means an admin can review issued tokens, invalidate a single token, invalidate all tokens for a client, and manage users without crossing client boundaries.",
      "It also keeps app admins separate from the end users who sign in to those apps.",
    ],
    highlights: [
      "Owned-client scoping for every admin action",
      "Single-token invalidation",
      "Per-client token invalidation",
      "User management tied to owned clients",
    ],
  },
  {
    id: "setup",
    number: "5.",
    kicker: "Setup",
    title: "What an app needs",
    paragraphs: [
      "To onboard an application, a customer creates a registered client, chooses the grant types and client authentication methods it needs, and defines the redirect URIs and post-logout redirect URIs the app will actually use.",
      "The backend validates redirect and logout URIs, checks client settings, supports PKCE rules for public flows, and keeps token lifetime settings on the client itself.",
      "Public browser-based applications typically use authorization code plus PKCE. Confidential applications can additionally use a client secret where appropriate.",
    ],
    highlights: [
      "Grant-type and client-authentication-method control",
      "Redirect and post-logout redirect validation",
      "PKCE-aware client configuration",
      "Client-level token TTL configuration",
    ],
  },
  {
    id: "integration",
    number: "6.",
    kicker: "Integration",
    title: "What apps get back",
    paragraphs: [
      "Applications should treat Identity Platform as their OpenID Provider and OAuth Authorization Server. Configure the authority URL, client id, redirect URI, post-logout redirect URI, and requested scopes in the app or SDK you are using.",
      "After that, the application redirects users into the hosted sign-in flow, receives the callback through the standard OIDC redirect path, and can validate or use the issued tokens with the expected issuer configuration.",
      "The backend also adds claims like the authorized party and normalized authorities so apps can make authorization decisions from the token they receive.",
    ],
    highlights: [
      "Normal OIDC authority/client configuration",
      "Hosted authorization flow and callback handling",
      "JWT claims include authorized party and authority data",
      "Designed to work with existing OIDC SDKs",
    ],
  },
  {
    id: "sessions",
    number: "7.",
    kicker: "Sessions",
    title: "Token and session behavior",
    paragraphs: [
      "Access token lifetime is controlled per registered client through token settings. The sign-in session on the server is separate, so token expiry and full sign-out are related but not the same thing.",
      "That lets apps use short-lived API tokens without forcing a full sign-in prompt every time a token expires, while still keeping logout, refresh, and session behavior explicit.",
      "In practice, token lifetimes, sign-in sessions, and revocation can be managed separately.",
    ],
    highlights: [
      "Per-client access-token and refresh-token settings",
      "Server-side sign-in session separated from token TTL",
      "Explicit logout and revocation behavior",
      "Separate token and session handling",
    ],
  },
  {
    id: "limits",
    number: "8.",
    kicker: "Limits",
    title: "Plan limits and backend checks",
    paragraphs: [
      "Limits around registered clients, users, scopes, and authorities are validated on the backend so entitlement rules cannot be bypassed by manipulating requests in the browser.",
      "The same plan data is shown in the app so customers can see what is available before saving changes, while the backend stays in charge of what is actually allowed.",
      "That keeps plan limits tied directly to the auth server instead of checking them only in the UI.",
    ],
    highlights: [
      "Backend validation of client, user, scope, and authority allowances",
      "Plan-aware feedback in the app",
      "Consistent subscription-to-limit mapping",
      "Usage rules enforced outside the browser",
    ],
  },
  {
    id: "onboarding",
    number: "9.",
    kicker: "Quick Start",
    title: "Basic onboarding flow for customer applications",
    steps: [
      "Create a registered client for the application you want to connect.",
      "Choose the grant types, authentication method, and redirect URIs the application requires.",
      "Enable PKCE for public clients and keep secrets only on confidential server-side clients.",
      "Configure your app or OIDC SDK with the authority URL, client id, callback URL, logout redirect URL, and scopes.",
      "Run the normal sign-in flow and validate the returned issuer, tokens, and callback behavior in your environment.",
    ],
  },
];

export default function Docs() {
  const [activeSectionId, setActiveSectionId] = useState(docSections[0].id);
  const [mobileOpenSectionId, setMobileOpenSectionId] = useState(docSections[0].id);
  const activeSection = docSections.find((section) => section.id === activeSectionId) ?? docSections[0];
  const renderSectionContent = (section, inline = false) => (
    <article
      key={section.id}
      className={`docs-article-section${section.id === "onboarding" ? " docs-article-section-emphasis" : ""}${inline ? " docs-article-section-inline" : ""}`}
    >
      <div className="docs-article-heading">
        {!inline ? <span className="docs-article-number">{section.number}</span> : null}
        <div className="docs-article-heading-copy">
          <div className="docs-kicker">{section.kicker}</div>
          <h2>{section.title}</h2>
        </div>
      </div>

      {section.paragraphs?.map((paragraph) => (
        <p key={paragraph}>{paragraph}</p>
      ))}

      {section.highlights ? (
        <ul className="docs-highlight-list">
          {section.highlights.map((highlight) => (
            <li key={highlight}>{highlight}</li>
          ))}
        </ul>
      ) : null}

      {section.steps ? (
        <ol className="docs-step-list">
          {section.steps.map((step) => (
            <li key={step}>{step}</li>
          ))}
        </ol>
      ) : null}
    </article>
  );

  return (
    <div className="docs-page">
      <section className="docs-hero">
        <div className="docs-kicker">Security Docs</div>
        <h1>How Identity Platform works</h1>
        <p>
          Identity Platform uses OAuth 2.0 and OpenID Connect, rotates JWT signing keys, and keeps client rules on
          the backend so apps can integrate with it and admins can manage clients and users in one place.
        </p>
      </section>

      <div className="docs-layout">
        <aside className="docs-outline" aria-label="Documentation sections">
          <div className="docs-outline-label">Docs</div>
          <ol className="docs-outline-list">
            {docSections.map((section) => {
              const isActive = activeSectionId === section.id;
              const isOpen = mobileOpenSectionId === section.id;

              return (
                <li className="docs-outline-item" key={section.id}>
                  <button
                    type="button"
                    className={`docs-outline-button${isActive ? " is-selected" : ""}${isOpen ? " is-open" : ""}`}
                    onClick={() => {
                      setActiveSectionId(section.id);
                      setMobileOpenSectionId((currentId) => (currentId === section.id ? null : section.id));
                    }}
                    aria-pressed={isActive}
                    aria-expanded={isOpen}
                  >
                    <span className="docs-outline-number">{section.number}</span>
                    <span className="docs-outline-text-wrap">
                      <span className="docs-outline-text">{section.title}</span>
                      <span className={`docs-outline-caret${isOpen ? " is-open" : ""}`} aria-hidden="true">
                        <svg viewBox="0 0 20 20" fill="none">
                          <path d="M5 7.5 10 12.5l5-5" />
                        </svg>
                      </span>
                    </span>
                  </button>
                  {isOpen ? (
                    <div className="docs-outline-mobile-content">
                      {renderSectionContent(section, true)}
                    </div>
                  ) : null}
                </li>
              );
            })}
          </ol>
        </aside>

        <section className="docs-content" aria-label="Documentation content">
          {renderSectionContent(activeSection)}
        </section>
      </div>
    </div>
  );
}
