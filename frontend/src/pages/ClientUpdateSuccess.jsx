import { useEffect, useMemo, useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import "./ClientUpdateSuccess.css";

const REDIRECT_SECONDS = 5;

function buildCountdownDigits() {
  return Array.from({ length: REDIRECT_SECONDS + 1 }, (_, index) => REDIRECT_SECONDS - index).map((value) => ({
    value,
    offset: REDIRECT_SECONDS - value,
  }));
}

export default function ClientUpdateSuccess() {
  const location = useLocation();
  const navigate = useNavigate();
  const [secondsRemaining, setSecondsRemaining] = useState(REDIRECT_SECONDS);
  const successState = location.state ?? null;
  const kicker = String(successState?.kicker ?? "").trim() || "Success";
  const displayName = String(successState?.title ?? "").trim() || "Update complete";
  const displayIdentifier = String(successState?.identifier ?? "").trim();
  const detailLabel = String(successState?.detailLabel ?? "").trim();
  const detailValue = String(successState?.detailValue ?? "").trim();
  const message = String(successState?.message ?? "").trim() || "Your request completed successfully.";
  const redirectTo = String(successState?.redirectTo ?? "").trim() || "/";
  const returnLabel = String(successState?.returnLabel ?? "").trim() || "Return now";
  const countdownDigits = useMemo(() => buildCountdownDigits(), []);

  useEffect(() => {
    if (secondsRemaining <= 0) {
      navigate(redirectTo, { replace: true });
      return undefined;
    }

    const timeoutId = window.setTimeout(() => {
      setSecondsRemaining((current) => Math.max(0, current - 1));
    }, 1000);

    return () => window.clearTimeout(timeoutId);
  }, [navigate, redirectTo, secondsRemaining]);

  if (!successState) {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="client-update-success-page">
      <section className="client-update-success-card">
        <div className="client-update-success-kicker">{kicker}</div>
        <h1 className="client-update-success-title">{displayName}</h1>
        {displayIdentifier ? (
          <div className="client-update-success-id">{displayIdentifier}</div>
        ) : null}
        <p className="client-update-success-copy">{message}</p>
        {detailValue ? (
          <div className="client-update-success-detail">
            {detailLabel ? <div className="client-update-success-detail-label">{detailLabel}</div> : null}
            <div className="client-update-success-detail-value">{detailValue}</div>
          </div>
        ) : null}

        <div className="client-update-success-redirect">
          <div className="client-update-success-redirect-copy">Redirecting shortly</div>
          <div className="client-update-success-countdown" aria-label={`${secondsRemaining} seconds remaining`}>
            <div className="client-update-success-countdown-window">
              <div
                className="client-update-success-countdown-strip"
                style={{ transform: `translateY(-${(REDIRECT_SECONDS - secondsRemaining) * 3.2}rem)` }}
              >
                {countdownDigits.map((digit) => (
                  <div className="client-update-success-countdown-digit" key={digit.offset}>
                    {digit.value}
                  </div>
                ))}
              </div>
            </div>
            <div className="client-update-success-countdown-meta">
              <span className="client-update-success-countdown-label">seconds</span>
              <div className="client-update-success-progress-track" aria-hidden="true">
                <div
                  className="client-update-success-progress-bar"
                  style={{ width: `${(secondsRemaining / REDIRECT_SECONDS) * 100}%` }}
                />
              </div>
            </div>
          </div>
        </div>

        <Link className="client-update-success-link" to={redirectTo}>
          {returnLabel}
        </Link>
      </section>
    </div>
  );
}
