import { useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { clearPlatformApiCache } from "../services/platform";
import {
  clearSubscriptionTierCache,
  clearPendingSubscriptionCheckout,
  fetchSubscriptionCheckoutStatus,
  getPendingSubscriptionCheckout,
} from "../services/subscription";
import { useSubscription } from "../context/SubscriptionContext";
import "./SubscriptionCheckoutStatus.css";

const POLL_INTERVAL_MS = 3000;
const POLL_TIMEOUT_MS = 30000;
const REDIRECT_SECONDS = 5;

function buildCountdownDigits(seconds) {
  return Array.from({ length: seconds + 1 }, (_, index) => seconds - index);
}

export default function SubscriptionCheckoutSuccess() {
  const navigate = useNavigate();
  const { refreshTier } = useSubscription();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const successMode = String(location.state?.mode ?? (searchParams.get("session_id") ? "checkout" : "change")).trim().toLowerCase();
  const shouldPollWebhook = successMode === "checkout";
  const [syncState, setSyncState] = useState(shouldPollWebhook ? "pending" : "confirmed");
  const [secondsRemaining, setSecondsRemaining] = useState(REDIRECT_SECONDS);
  const [statusError, setStatusError] = useState("");
  const [pendingCheckout] = useState(() => getPendingSubscriptionCheckout());
  const sessionId = String(searchParams.get("session_id") ?? "").trim();
  const expectedTierName = String(location.state?.tierName ?? pendingCheckout?.tierName ?? "").trim();
  const countdownDigits = useMemo(() => buildCountdownDigits(REDIRECT_SECONDS), []);
  const customTitle = String(location.state?.title ?? "").trim();
  const customMessage = String(location.state?.message ?? "").trim();
  const hasSyncedSubscriptionRef = useRef(false);

  async function syncSubscriptionState() {
    clearPendingSubscriptionCheckout();
    clearPlatformApiCache();
    clearSubscriptionTierCache();
    await refreshTier();
  }

  useEffect(() => {
    if (!shouldPollWebhook || !sessionId || syncState !== "pending") {
      return undefined;
    }

    let isMounted = true;
    const pollStatus = async () => {
      try {
        const status = await fetchSubscriptionCheckoutStatus(sessionId);
        if (!isMounted) {
          return;
        }

        if (status === "completed") {
          setSyncState("confirmed");
          setStatusError("");
          clearPendingSubscriptionCheckout();
          return;
        }

        if (status === "failed") {
          setSyncState("timeout");
          setStatusError("The billing webhook reported a processing error.");
        }
      } catch (error) {
        if (isMounted) {
          setStatusError(error?.message || "Unable to check subscription status.");
        }
      }
    };

    pollStatus();
    const intervalId = window.setInterval(pollStatus, POLL_INTERVAL_MS);

    return () => {
      isMounted = false;
      window.clearInterval(intervalId);
    };
  }, [sessionId, shouldPollWebhook, syncState]);

  useEffect(() => {
    if (!shouldPollWebhook || syncState !== "pending") {
      return undefined;
    }

    const timeoutId = window.setTimeout(() => {
      setSyncState("timeout");
    }, POLL_TIMEOUT_MS);

    return () => window.clearTimeout(timeoutId);
  }, [shouldPollWebhook, syncState]);

  useEffect(() => {
    if (syncState !== "confirmed" || hasSyncedSubscriptionRef.current) {
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
  }, [refreshTier, syncState]);

  useEffect(() => {
    if (syncState !== "confirmed") {
      return undefined;
    }

    if (secondsRemaining <= 0) {
      navigate("/subscriptions", { replace: true });
      return undefined;
    }

    const timeoutId = window.setTimeout(() => {
      setSecondsRemaining((current) => Math.max(0, current - 1));
    }, 1000);

    return () => window.clearTimeout(timeoutId);
  }, [secondsRemaining, syncState]);

  const title = customTitle || (syncState === "confirmed"
    ? "Subscription active"
    : syncState === "timeout"
      ? "Still confirming your upgrade"
      : "Finishing your upgrade");
  const message = customMessage || (syncState === "confirmed"
    ? "Your billing completed successfully and the subscription webhook has confirmed the upgrade."
    : syncState === "timeout"
      ? "Stripe checkout completed, but the webhook has not confirmed the upgrade yet. It may still be processing in the background."
      : "Stripe checkout completed. We are waiting for the subscription webhook confirmation.");

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
      <section className={`subscription-checkout-card subscription-checkout-card-${syncState}`}>
        <div className="subscription-checkout-kicker">Billing</div>
        <h1 className="subscription-checkout-title">{title}</h1>
        {expectedTierName ? (
          <div className="subscription-checkout-id">{expectedTierName}</div>
        ) : null}
        <p className="subscription-checkout-copy">{message}</p>
        {statusError ? (
          <div className="subscriptions-error">{statusError}</div>
        ) : null}
        {shouldPollWebhook && syncState === "pending" ? (
          <div className="subscription-checkout-pulse">
            <div className="subscription-checkout-progress-track" aria-hidden="true">
              <div className="subscription-checkout-progress-bar subscription-checkout-progress-bar-animated" />
            </div>
            <div className="subscription-checkout-status-note">Waiting for webhook confirmation</div>
          </div>
        ) : null}

        {syncState === "confirmed" ? (
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
        ) : null}

        <div className="subscription-checkout-actions">
          {shouldPollWebhook ? (
            <button
              type="button"
              className="subscription-checkout-link subscription-checkout-link-primary"
              onClick={() => window.location.reload()}
            >
              Check again
            </button>
          ) : null}
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
