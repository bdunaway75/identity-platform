import Button from "../components/Button";
import Card from "../components/Card";

export default function Clients() {
  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        height: "100%",        // important
      }}
    >
      <h2>Clients</h2>

      
        <div style={{marginto:"100%"}}>
            
        </div>

      <div style={{ marginTop: "auto" }}>
        <Button>New Client</Button>
      </div>
    </div>
  );
}
