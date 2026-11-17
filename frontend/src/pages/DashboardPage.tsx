import { useEffect, useState } from "react";
import { Link } from "react-router";
import { FileText, FolderKanban, CircleCheck } from "lucide-react";
import Alert from "../components/ui/Alert";
import { useAuth } from "../context/useAuth";
import { listWorkspaces } from "../services/workspaceService";
import type { Workspace } from "../types/api";
import { getErrorMessage } from "../utils/errors";

export default function DashboardPage() {
  const { user } = useAuth();
  const [workspaces, setWorkspaces] = useState<Workspace[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    listWorkspaces()
      .then((data) => {
        if (active) setWorkspaces(data);
      })
      .catch((err) => {
        if (active) setError(getErrorMessage(err));
      });
    return () => {
      active = false;
    };
  }, []);

  const totalDocuments = workspaces?.reduce((sum, w) => sum + w.documentCount, 0);
  const readyDocuments = workspaces?.reduce((sum, w) => sum + w.readyDocumentCount, 0);

  const stats = [
    { label: "Workspaces", value: workspaces?.length, icon: FolderKanban },
    { label: "Documents", value: totalDocuments, icon: FileText },
    { label: "Ready to query", value: readyDocuments, icon: CircleCheck },
  ];

  return (
    <div>
      <h1 className="text-2xl font-semibold">Welcome, {user?.fullName}</h1>
      <p className="mt-1 text-sm text-stone-600">Here is what is in your account.</p>

      {error && (
        <div className="mt-6">
          <Alert message={error} />
        </div>
      )}

      <div className="mt-6 grid gap-4 sm:grid-cols-3">
        {stats.map(({ label, value, icon: Icon }) => (
          <div key={label} className="rounded-xl border border-stone-200 bg-white p-5">
            <div className="flex items-center gap-2 text-sm text-stone-500">
              <Icon className="h-4 w-4" aria-hidden="true" />
              {label}
            </div>
            <p className="mt-2 text-3xl font-semibold">{value ?? "–"}</p>
          </div>
        ))}
      </div>

      <section className="mt-8">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Recent workspaces</h2>
          <Link to="/workspaces" className="text-sm font-medium text-brand-700 hover:underline">
            View all
          </Link>
        </div>
        {workspaces && workspaces.length === 0 && (
          <p className="mt-3 text-sm text-stone-600">
            No workspaces yet.{" "}
            <Link to="/workspaces" className="font-medium text-brand-700 hover:underline">
              Create your first one
            </Link>
            .
          </p>
        )}
        {workspaces && workspaces.length > 0 && (
          <ul className="mt-3 divide-y divide-stone-200 rounded-xl border border-stone-200 bg-white">
            {workspaces.slice(0, 5).map((workspace) => (
              <li key={workspace.id} className="flex items-center justify-between px-5 py-3 text-sm">
                <span className="truncate font-medium">{workspace.name}</span>
                <span className="text-stone-500">{workspace.documentCount} documents</span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}