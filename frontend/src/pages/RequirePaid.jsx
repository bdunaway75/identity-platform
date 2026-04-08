import Spinner from "react-bootstrap/Spinner";
import { Navigate, Outlet } from "react-router-dom";
import { useSubscription } from "../context/SubscriptionContext";

export default function RequirePaid() {
  const { isPaid, status } = useSubscription();

  if (status === "loading") {
    return <Spinner animation="grow" />;
  }

  if (!isPaid) {
    return <Navigate to="/subscriptions" replace />;
  }

  return <Outlet />;
}
