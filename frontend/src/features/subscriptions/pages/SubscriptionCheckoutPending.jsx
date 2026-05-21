import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { APP_ENDPOINTS } from "../../../shared/config/endpoints";
import { clearPendingSubscriptionCheckout, getPendingSubscriptionCheckout } from "../services/subscription";
import "../styles/SubscriptionCheckoutStatus.css";

const STREAM_TIMEOUT_MS = 30000;

function buildSubscriptionEventsUrl(sessionId) {
  return `${APP_ENDPOINTS.platform.subscriptionEvents}?session_id=${encodeURIComponent(sessionId)}`;
}

export default function SubscriptionCheckoutPending() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [statusError, setStatusError] = useState("");
  const pendingCheckout = getPendingSubscriptionCheckout();
  const sessionId = searchParams.get("session_id");
  const expectedTierName = pendingCheckout?.tierName || "";

  useEffect(() => {
    if (!sessionId) {
      navigate("/subscriptions/cancel", { replace: true });
      return undefined;
    }

    let settled = false;
    const events = new EventSource(buildSubscriptionEventsUrl(sessionId));
    const timeoutId = window.setTimeout(() => {
      if (settled) {
        return;
      }

      settled = true;
      events.close();
      navigate("/subscriptions/cancel", { replace: true });
    }, STREAM_TIMEOUT_MS);

    events.addEventListener("redirect", (event) => {
      if (settled) {
        return;
      }

      const redirectUrl = event.data;
      if (!redirectUrl) {
        return;
      }

      settled = true;
      window.clearTimeout(timeoutId);
      events.close();
      clearPendingSubscriptionCheckout();
      navigate(redirectUrl, { replace: true });
    });

    events.onerror = () => {
      if (settled) {
        return;
      }

      setStatusError("Connection interrupted while waiting for billing confirmation. Reconnecting...");
    };

    return () => {
      settled = true;
      window.clearTimeout(timeoutId);
      events.close();
    };
  }, [navigate, sessionId]);

  return (
    <div className="subscription-checkout-page">
      <section className="subscription-checkout-card">
        <div className="subscription-checkout-kicker">Billing</div>
        <h1 className="subscription-checkout-title">Finishing your upgrade</h1>
        {expectedTierName ? (
          <div className="subscription-checkout-id">{expectedTierName}</div>
        ) : null}
        <p className="subscription-checkout-copy">
          Stripe checkout completed, waiting for confirmation
        </p>
        {statusError ? (
          <div className="subscriptions-error">{statusError}</div>
        ) : null}
        <div className="subscription-checkout-pulse">
          <div className="subscription-checkout-progress-track" aria-hidden="true">
            <div className="subscription-checkout-progress-bar subscription-checkout-progress-bar-animated" />
          </div>
          <div className="subscription-checkout-status-note">Waiting for redirect</div>
        </div>
      </section>
    </div>
  );
}

