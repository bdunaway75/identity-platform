import Button from "../components/Button";
import Card from "../components/Card";

export default function Subscriptions() {
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
            <p>Current tier: <strong>Free</strong></p>
            <ul>
                <li>0 active users.</li>
                <li>100 avaialble user slots.</li>
                <li>No SLA</li>
            </ul>
        </Card>
        </div>

        <div style={{marginTop: "auto"}}>
            <Button>Upgrade</Button>
        </div>
    </div>
    </>
  );
}
