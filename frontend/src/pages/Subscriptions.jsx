import Button from "../components/Button";
import Card from "../components/Card";
import { useSubscription } from "../context/SubscriptionContext";
import {
  DEFAULT_TIER,
  isDevSubscriptionOverrideAvailable,
  setDevSubscriptionOverrideTier,
} from "../services/subscription";

export default function Subscriptions() {
  const { tier, status, error, source, refreshTier } = useSubscription();
  const canUseDevTierOverride = isDevSubscriptionOverrideAvailable();

  const handleSwitchToFree = () => {
    setDevSubscriptionOverrideTier(DEFAULT_TIER);
  };

  return (
    <>
    <div
      style={{
            display: "flex",
            flexDirection: "column",
            height: "100%",
      }} >
            
      <h2>Subscription</h2>
      
      <div style={{ maxWidth: "400px"}}>
        <Card >
            <p>Current tier: <strong>{status === "loading" ? "Loading..." : tier}</strong></p>
            <ul>
                <li>0 active users.</li>
                <li>100 avaialble user slots.</li>
                <li>No SLA</li>
            </ul>
            {error ? <p style={{ color: "#fca5a5" }}>{error}</p> : null}
            <p style={{ color: "rgba(229, 231, 235, 0.65)", fontSize: "0.8rem" }}>
              Tier source: {source}
            </p>
        </Card>
        </div>

        <div style={{marginTop: "auto", display: "flex", gap: "0.75rem"}}>
            <Button variant="secondary" type="button" onClick={refreshTier}>Refresh tier</Button>
            {canUseDevTierOverride ? (
              <Button variant="secondary" type="button" onClick={handleSwitchToFree}>
                Switch to free
              </Button>
            ) : null}
            <Button>Upgrade</Button>
        </div>
    </div>
    </>
  );
}
