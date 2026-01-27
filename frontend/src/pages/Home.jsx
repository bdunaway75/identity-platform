import Card from "../components/Card";

export default function Home() {
  return (
    <>
      <h2>Dashboard</h2>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))",
          gap: "1.5rem",
          marginTop: "1.5rem",
        }}
      >
        <Card title="Account">
          <div>Tier: Free</div>
          <div>Clients: 0</div>
        </Card>

        <Card title="Usage">
          <div>Requests: 0</div>
          <div>Limit: 100/min</div>
        </Card>

        <Card title="System Status">
          <div style={{ color: "#22d3ee" }}>● All systems nominal</div>
        </Card>
      </div>
    </>
  );
}
