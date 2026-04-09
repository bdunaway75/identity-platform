import Button from "../components/Button";
import "./Clients.css";

export default function ClientsAccessPage() {
  return (
    <div className="clients-locked-page clients-locked-page-embedded">
      <div className="clients-locked-card">
        <div className="clients-locked-kicker">Paid Feature</div>
        <h1>Unlock the client workspace with a paid plan</h1>
        <p className="clients-locked-copy">
          Paid plans include the registered client registry, new client creation, and client-user management from one workspace.
        </p>
        <Button type="button">Purchase</Button>
      </div>
        </div>
  );
}
