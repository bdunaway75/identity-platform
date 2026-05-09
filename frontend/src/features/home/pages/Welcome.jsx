import "../../clients/styles/Clients.css";
import "../styles/Home.css";
import { Link, useSearchParams } from "react-router-dom";

const trustPillars = [
  {
    kicker: "Why It Exists",
    title: "Built for real ownership boundaries",
    body:
      "Identity Platform was created around a simple split: platform users operate the identity layer, while client users sign in to the applications those platform users register. That separation helps keep admin responsibilities, tenant ownership, and end-user access from getting blurred together.",
  },
  {
    kicker: "Password Safety",
    title: "Passwords are not stored in plain form",
    body:
      "Platform passwords are stored as Argon2 hashes, and security checks stay on the backend. The goal is to make the browser a management surface, not the system that decides what is allowed.",
  },
  {
    kicker: "Token Safety",
    title: "Token handling is built for traceability",
    body:
      "Raw token values are not stored in the database. Token values are hashed before persistence, issued JWTs are tied to a signing key id, and signing keys rotate over time while older verification keys remain available during rollover.",
  },
  {
    kicker: "Signing Keys",
    title: "Understand which key signed what",
    body:
      "Issued tokens stay connected to a signing key id so key rotation is not a blind event. That makes it easier to understand verification history and reason about rollover instead of treating key changes like hidden infrastructure noise.",
  },
  {
    kicker: "User Management",
    title: "Built to make user access easier to manage",
    body:
      "The platform is meant to keep client-user management in one place, with room for upcoming workflows like directly adding users, shaping access, and keeping those changes close to the clients they belong to.",
  },
  {
    kicker: "Dashboards",
    title: "Designed to stay feasible as visibility grows",
    body:
      "The dashboard direction is about making usage, ownership, and activity understandable at a glance, so the platform can keep adding insight without turning day-to-day identity work into a reporting chore.",
  },
];

const reassuranceSections = [
  {
    heading: "Why this platform was built",
    copy:
      "As more applications depend on automation, delegated access, and machine-issued tokens, identity stops being just a login screen. This platform was built to make ownership, token lifecycle, and audit visibility explicit from the start.",
  },
  {
    heading: "How your data is handled",
    copy:
      "Passwords are Argon2-hashed, token values are stored as hashed values instead of raw secrets, and authorization rules live on the backend. That means account and token safety are not left to browser-only checks.",
  },
  {
    heading: "Why teams can trust the model",
    copy:
      "Platform users can only manage registered clients they own. Client-user data, token actions, and admin workflows are scoped to those owned clients so one customer space does not quietly bleed into another.",
  },
];

function normalizeDemoCode(value) {
  return String(value ?? "").trim();
}

function resolveProvidedDemoCode(searchParams) {
  return normalizeDemoCode(searchParams.get("code") ?? searchParams.get("number"));
}

export default function Welcome() {
  const [searchParams] = useSearchParams();
  const providedDemoCode = resolveProvidedDemoCode(searchParams);

  return (
    <div className="clients-page dashboard-page welcome-page">
      <div className="client-shell">
        <div className="client-header">
          <div className="client-header-copy">
            <div className="client-title">Welcome to Identity Platform</div>
            <div className="client-subtitle">
              A control plane for OAuth 2.0 and OpenID Connect that puts tenant ownership,
              token traceability, and operational clarity first.
            </div>
          </div>
        </div>

        {providedDemoCode ? (
          <section className="welcome-demo-code-card">
            <div>
              <div className="welcome-demo-code-kicker">Demo code provided</div>
              <div className="welcome-demo-code-copy">
                You have been provided a demo access code. Continue to demo access when you are ready.
              </div>
            </div>
            <Link
              className="welcome-demo-code-button"
              to={`/demo-access?code=${encodeURIComponent(providedDemoCode)}`}
            >
              Use demo code
            </Link>
          </section>
        ) : null}

        <div className="dashboard-layout">
          <section className="client-card client-card-primary welcome-hero-card">
            <div className="client-card-header">
              <div>
                <div className="client-card-kicker">First Look</div>
                <div className="client-card-title">What this platform is trying to solve</div>
              </div>
              <div className="client-card-caption">
                Identity Platform was built for teams that need more than a basic login box.
              </div>
            </div>

            <div className="client-card-contents welcome-hero-grid">
              <div className="client-summary-grid dashboard-summary-grid welcome-summary-grid">
                {trustPillars.map((pillar) => (
                  <div className="client-summary-tile welcome-summary-tile" key={pillar.title}>
                    <div className="client-summary-label">{pillar.kicker}</div>
                    <div className="welcome-summary-title">{pillar.title}</div>
                    <div className="welcome-summary-copy">{pillar.body}</div>
                  </div>
                ))}
              </div>
            </div>
          </section>

          <div className="dashboard-secondary-grid welcome-secondary-grid">
            <section className="client-card client-card-secondary">
              <div className="client-card-header">
                <div>
                  <div className="client-card-kicker">Trust Model</div>
                  <div className="client-card-title">What people should feel confident about</div>
                </div>
                <div className="client-card-caption">
                  The platform is designed to be understandable as well as secure.
                </div>
              </div>

              <div className="client-card-contents welcome-section-stack">
                {reassuranceSections.map((section) => (
                  <div className="welcome-trust-row" key={section.heading}>
                    <div className="welcome-trust-heading">{section.heading}</div>
                    <div className="welcome-trust-copy">{section.copy}</div>
                  </div>
                ))}
              </div>
            </section>
          </div>
        </div>
      </div>
    </div>
  );
}
