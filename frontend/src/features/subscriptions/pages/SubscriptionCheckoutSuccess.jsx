import { useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { clearPlatformApiCache } from "../../clients/services/platform";
import { clearSubscriptionTierCache, clearPendingSubscriptionCheckout, getPendingSubscriptionCheckout } from "../services/subscription";
import { useSubscription } from "../context/SubscriptionContext";
import "../styles/SubscriptionCheckoutStatus.css";

const REDIRECT_SECONDS = 5;

function buildCountdownDigits(seconds) {
  return Array.from({ length: seconds + 1 }, (_, index) => seconds - index);
}

export default function SubscriptionCheckoutSuccess() {
  const navigate = useNavigate();
  const { refreshTier } = useSubscription();
  const location = useLocation();
  const [secondsRemaining, setSecondsRemaining] = useState(REDIRECT_SECONDS);
  const [pendingCheckout] = useState(() => getPendingSubscriptionCheckout());
  const expectedTierName = location.state?.tierName || pendingCheckout?.tierName || "";
  const countdownDigits = useMemo(() => buildCountdownDigits(REDIRECT_SECONDS), []);
  const customTitle = location.state?.title || "";
  const customMessage = location.state?.message || "";
  const hasSyncedSubscriptionRef = useRef(false);

  async function syncSubscriptionState() {
    clearPendingSubscriptionCheckout();
    clearPlatformApiCache();
    clearSubscriptionTierCache();
    await refreshTier();
  }

  useEffect(() => {
    if (hasSyncedSubscriptionRef.current) {
      return undefined;
    }

    hasSyncedSubscriptionRef.current = true;
    let isMounted = true;

    syncSubscriptionState().catch((error) => {
      if (isMounted) {
        console.error("Unable to refresh subscription tier after successful billing", error);
      }
    });

    return () => {
      isMounted = false;
    };
  }, [refreshTier]);

  useEffect(() => {
    if (secondsRemaining <= 0) {
      navigate("/subscriptions", { replace: true });
      return undefined;
    }

    const timeoutId = window.setTimeout(() => {
      setSecondsRemaining((current) => Math.max(0, current - 1));
    }, 1000);

    return () => window.clearTimeout(timeoutId);
  }, [navigate, secondsRemaining]);

  const title = customTitle || "Subscription active";
  const message = customMessage || "Your billing completed successfully and the subscription upgrade is confirmed.";

  const handleReturnToSubscriptions = async () => {
    try {
      await syncSubscriptionState();
    } catch (error) {
      console.error("Unable to refresh subscription tier before returning", error);
    } finally {
      navigate("/subscriptions");
    }
  };

  return (
    <div className="subscription-checkout-page">
      <section className="subscription-checkout-card">
        <div className="subscription-checkout-kicker">Billing</div>
        <h1 className="subscription-checkout-title">{title}</h1>
        {expectedTierName ? (
          <div className="subscription-checkout-id">{expectedTierName}</div>
        ) : null}
        <p className="subscription-checkout-copy">{message}</p>
        <div className="subscription-checkout-redirect">
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
          <button
            type="button"
            className="subscription-checkout-link"
            onClick={handleReturnToSubscriptions}
          >
            Back to subscriptions
          </button>
        </div>
      </section>
    </div>
  );
}

