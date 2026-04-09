import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { clearPendingSubscriptionCheckout } from "../services/subscription";
import "./SubscriptionCheckoutStatus.css";

const REDIRECT_SECONDS = 5;

function buildCountdownDigits(seconds) {
  return Array.from({ length: seconds + 1 }, (_, index) => seconds - index);
}

export default function SubscriptionCheckoutCancel() {
  const [secondsRemaining, setSecondsRemaining] = useState(REDIRECT_SECONDS);
  const countdownDigits = useMemo(() => buildCountdownDigits(REDIRECT_SECONDS), []);

  useEffect(() => {
    clearPendingSubscriptionCheckout();
  }, []);

  useEffect(() => {
    if (secondsRemaining <= 0) {
      window.location.replace("/subscriptions");
      return undefined;
    }

    const timeoutId = window.setTimeout(() => {
      setSecondsRemaining((current) => Math.max(0, current - 1));
    }, 1000);

    return () => window.clearTimeout(timeoutId);
  }, [secondsRemaining]);

  return (
    <div className="subscription-checkout-page">
      <section className="subscription-checkout-card subscription-checkout-card-cancel">
        <div className="subscription-checkout-kicker">Billing</div>
        <h1 className="subscription-checkout-title">Checkout canceled</h1>
        <p className="subscription-checkout-copy">
          No subscription changes were made. You can return to the plans page whenever you are ready.
        </p>

        <div className="subscription-checkout-redirect">
          <div className="subscription-checkout-redirect-copy">Returning to subscriptions shortly</div>
          <div className="subscription-checkout-countdown" aria-label={`${secondsRemaining} seconds remaining`}>
            <div className="subscription-checkout-countdown-window">
              <div
                className="subscription-checkout-countdown-strip"
                style={{ transform: `translateY(-${(REDIRECT_SECONDS - secondsRemaining) * 3.2}rem)` }}
              >
                {countdownDigits.map((digit) => (
                  <div className="subscription-checkout-countdown-digit" key={digit}>
                    {digit}
                  </div>
                ))}
              </div>
            </div>
            <div className="subscription-checkout-countdown-meta">
              <span className="subscription-checkout-countdown-label">seconds</span>
            </div>
          </div>
        </div>

        <div className="subscription-checkout-actions">
          <Link className="subscription-checkout-link subscription-checkout-link-primary" to="/subscriptions">
            Back to subscriptions
          </Link>
        </div>
      </section>
    </div>
  );
}
