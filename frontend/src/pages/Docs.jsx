import "./Docs.css";

const trustSections = [
  {
    kicker: "Protocols",
    title: "Standards-based authentication",
    paragraphs: [
      "Identity Platform is built around OAuth 2.0 and OpenID Connect so customer applications integrate against familiar authorization-code and token-based flows instead of proprietary session patterns.",
      "Registered clients are managed as first-class OAuth clients with redirect URI validation, client authentication method control, grant-type control, PKCE support, consent handling, and OpenID scope support.",
    ],
  },
  {
    kicker: "Security",
    title: "How the service protects customer data",
    paragraphs: [
      "Passwords are stored as Argon2 hashes, token and authorization state are handled through Spring Authorization Server and Spring Security, and protected platform APIs are enforced with JWT validation plus role-based authorization checks.",
      "Ownership boundaries are enforced on the backend. Platform users can only manage registered clients they own, and client-user management is scoped to those owned clients. Tier limits are also validated server-side so usage rules cannot be bypassed from the UI.",
      "Session and token-related state are backed by Redis-aware serialization, and invalid or expired authentication on the frontend is forced back through logout/login flow instead of allowing stale authenticated requests to continue.",
    ],
  },
  {
    kicker: "Operations",
    title: "What a customer app needs to configure",
    paragraphs: [
      "To connect an application, a customer registers a client, chooses the allowed grant types and client authentication methods, defines redirect URIs and optional post-logout redirect URIs, and configures the scopes, authorities, and token settings the app should use.",
      "A typical browser-based application only needs a client id, redirect URI, post-logout redirect URI, authorization code flow, PKCE, and the expected OIDC authority/base URL. Confidential server-side clients can additionally use a client secret where appropriate.",
    ],
  },
  {
    kicker: "Integration",
    title: "How other apps use the auth server",
    paragraphs: [
      "Customer applications should treat Identity Platform as their OpenID Provider and OAuth Authorization Server. That means configuring their app or SDK with the authority URL, client id, redirect URI, post-logout redirect URI, and the scopes required for their sign-in flow.",
      "Once configured, the external application redirects users into the hosted login flow, receives authorization codes or tokens through the standard callback path, and can use the issued tokens against downstream protected APIs that trust the same issuer.",
    ],
  },
];

const trustPoints = [
  "OAuth 2.0 and OpenID Connect based flows",
  "Authorization code + PKCE support for public/browser clients",
  "JWT-protected platform APIs with backend authorization checks",
  "Argon2 password hashing",
  "Redis-backed security/session support",
  "Server-side ownership and tier enforcement",
];

const onboardingSteps = [
  "Create a registered client and choose the grant types and client authentication methods your application needs.",
  "Add the exact redirect URIs and post-logout redirect URIs your app will use.",
  "Set PKCE and consent requirements based on whether the client is public or confidential.",
  "Configure your app or identity SDK with the authority URL, client id, redirect URI, logout redirect URI, and requested scopes.",
  "Run the standard OIDC sign-in flow and validate the returned tokens against the configured issuer.",
];

export default function Docs() {
  return (
    <div className="docs-page">
      <section className="docs-hero">
        <div className="docs-kicker">Security Docs</div>
        <h1>Built to be integrated and trusted</h1>
        <p>
          Identity Platform follows modern identity protocols and enforces security-critical rules on the
          backend so customer applications can integrate with predictable OAuth 2.0 and OpenID Connect flows.
        </p>
      </section>

      <section className="docs-trust-strip" aria-label="Trust highlights">
        {trustPoints.map((point) => (
          <div className="docs-trust-pill" key={point}>
            {point}
          </div>
        ))}
      </section>

      <section className="docs-article">
        {trustSections.map((section) => (
          <article className="docs-section" key={section.title}>
            <div className="docs-kicker">{section.kicker}</div>
            <h2>{section.title}</h2>
            {section.paragraphs.map((paragraph) => (
              <p key={paragraph}>{paragraph}</p>
            ))}
          </article>
        ))}

        <article className="docs-section docs-section-emphasis">
          <div className="docs-kicker">Quick Start</div>
          <h2>Basic onboarding flow for customer applications</h2>
          <ol className="docs-step-list">
            {onboardingSteps.map((step) => (
              <li key={step}>{step}</li>
            ))}
          </ol>
        </article>
      </section>
    </div>
  );
}
