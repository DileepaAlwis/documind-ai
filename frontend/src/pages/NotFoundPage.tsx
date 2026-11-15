import { Link } from "react-router";

export default function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 px-6 text-center">
      <p className="text-5xl font-semibold text-brand-700">404</p>
      <p className="text-stone-600">This page does not exist.</p>
      <Link to="/dashboard" className="font-medium text-brand-700 hover:underline">
        Back to the dashboard
      </Link>
    </div>
  );
}
