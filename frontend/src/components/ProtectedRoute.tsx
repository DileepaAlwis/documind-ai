import { Navigate, Outlet, useLocation } from "react-router";
import { useAuth } from "../context/useAuth";
import FullPageSpinner from "./FullPageSpinner";

export default function ProtectedRoute() {
  const { status } = useAuth();
  const location = useLocation();

  if (status === "loading") return <FullPageSpinner />;
  if (status === "anonymous") return <Navigate to="/login" replace state={{ from: location }} />;
  return <Outlet />;
}