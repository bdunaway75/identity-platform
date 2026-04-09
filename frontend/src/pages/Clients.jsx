import ClientRegistrySection from "../components/ClientRegistrySection.jsx";
import { useSubscription } from "../context/SubscriptionContext";
import "./Clients.css";

export default function Clients() {
  const { isPaid, status } = useSubscription();

  if (status === "loading") {
    return (
      <div className="clients-locked-page">
        <div className="clients-locked-card">
          <div className="clients-locked-kicker">Checking plan</div>
          <h1>Loading subscription access</h1>
          <p className="clients-locked-copy">
            We are checking whether this account can view registered clients.
          </p>
        </div>
      </div>
    );
  }

  if (!isPaid) {
    return (
      <div className="clients-locked-page">
        <div className="clients-locked-card">
          <div className="clients-locked-kicker">Paid Feature</div>
          <h1>Unlock client registration with a paid plan</h1>
          <p className="clients-locked-copy">
            Upgrade your plan to review registered clients, attached users, and token activity.
          </p>
          <button type="button" className="clients-lock-button">
            Purchase
          </button>
        </div>
      </div>
    );
  }

  return (
    <ClientRegistrySection />
  );
}
