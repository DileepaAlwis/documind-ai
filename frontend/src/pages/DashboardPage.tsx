import { useEffect, useState } from "react";
import { FileSearch, LogOut } from "lucide-react";
import Alert from "../components/ui/Alert";
import Button from "../components/ui/Button";
import { useAuth } from "../context/useAuth";
import { api } from "../lib/api";
import type { User } from "../types/api";
import { getErrorMessage } from "../utils/errors";

export default function DashboardPage() {
  const { logout } = useAuth();
  const [profile, setProfile] = useState<User | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    api
      .get<User>("/users/me")
      .then((response) => {
        if (active) setProfile(response.data);
      })
      .catch((err) => {
        if (active) setError(getErrorMessage(err));
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="min-h-screen">
      <header className="border-b border-stone-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-2 font-semibold text-brand-700">
            <FileSearch className="h-5 w-5" aria-hidden="true" />
            DocuMind AI
          </div>
          <Button variant="secondary" type="button" onClick={() => void logout()}>
            <LogOut className="h-4 w-4" aria-hidden="true" />
            Log out
          </Button>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-6 py-10">
        {error && <Alert message={error} />}
        {profile && (
          <section className="rounded-xl border border-stone-200 bg-white p-6">
            <h1 className="text-xl font-semibold">Welcome, {profile.fullName}</h1>
            <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-3">
              <div>
                <dt className="text-stone-500">Email</dt>
                <dd className="font-medium">{profile.email}</dd>
              </div>
              <div>
                <dt className="text-stone-500">Role</dt>
                <dd className="font-medium">{profile.role}</dd>
              </div>
              <div>
                <dt className="text-stone-500">Member since</dt>
                <dd className="font-medium">{new Date(profile.createdAt).toLocaleDateString()}</dd>
              </div>
            </dl>
            <p className="mt-6 text-sm text-stone-600">
              Workspaces and document upload arrive in the next steps.
            </p>
          </section>
        )}
      </main>
    </div>
  );
}
