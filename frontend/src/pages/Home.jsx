import { useEffect, useState } from "react";
import Card from "../components/Card";
import { useSubscription } from "../context/SubscriptionContext";
import { fetchTotalClientCount, fetchTotalUserCount } from "../services/subscription";

export default function Home() {
  const { tier, status } = useSubscription();
  const [totalUsers, setTotalUsers] = useState(null);
  const [totalClients, setTotalClients] = useState(null);

  useEffect(() => {
    let isMounted = true;

    fetchTotalUserCount()
      .then((count) => {
        if (isMounted) {
          setTotalUsers(count);
        }
      })
      .catch(() => {
        if (isMounted) {
          setTotalUsers(0);
        }
      });

    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    let isMounted = true;

    fetchTotalClientCount()
      .then((count) => {
        if (isMounted) {
          setTotalClients(count);
        }
      })
      .catch(() => {
        if (isMounted) {
          setTotalClients(0);
        }
      });

    return () => {
      isMounted = false;
    };
  }, []);

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
          <div>Tier: {status === "loading" ? "Loading..." : tier}</div>
          <div>Clients: {totalClients ?? "Loading..."}</div>
          <div>Total Users: {totalUsers ?? "Loading..."}</div>
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
