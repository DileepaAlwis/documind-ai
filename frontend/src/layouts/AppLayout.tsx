import { useState } from "react";
import { NavLink, Outlet } from "react-router";
import { FileSearch, FolderKanban, LayoutDashboard, LogOut, Menu } from "lucide-react";
import { useAuth } from "../context/useAuth";

const navItems = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/workspaces", label: "Workspaces", icon: FolderKanban },
];

function initialsOf(fullName: string): string {
  return fullName
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join("");
}

export default function AppLayout() {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);

  return (
    <div className="min-h-screen lg:flex">
      {/* Top bar, small screens only */}
      <div className="flex items-center gap-3 border-b border-stone-200 bg-white px-4 py-3 lg:hidden">
        <button type="button" aria-label="Open navigation" onClick={() => setOpen(true)}>
          <Menu className="h-6 w-6" />
        </button>
        <span className="flex items-center gap-2 font-semibold text-brand-700">
          <FileSearch className="h-5 w-5" aria-hidden="true" />
          DocuMind AI
        </span>
      </div>

      {open && (
        <div
          className="fixed inset-0 z-30 bg-black/40 lg:hidden"
          onClick={() => setOpen(false)}
          aria-hidden="true"
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-brand-900 text-brand-50 transition-transform lg:sticky lg:top-0 lg:h-screen lg:translate-x-0 ${
          open ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="flex items-center gap-2 px-6 py-6 text-lg font-semibold">
          <FileSearch className="h-6 w-6" aria-hidden="true" />
          DocuMind AI
        </div>

        <nav aria-label="Main" className="flex-1 space-y-1 px-3">
          {navItems.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              onClick={() => setOpen(false)}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition ${
                  isActive ? "bg-brand-700 text-white" : "text-brand-100 hover:bg-brand-700/60"
                }`
              }
            >
              <Icon className="h-4 w-4" aria-hidden="true" />
              {label}
            </NavLink>
          ))}
        </nav>

        <div className="border-t border-brand-700 p-4">
          <div className="flex items-center gap-3">
            <div
              className="flex h-9 w-9 items-center justify-center rounded-full bg-brand-700 text-sm font-semibold"
              aria-hidden="true"
            >
              {user ? initialsOf(user.fullName) : "?"}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium">{user?.fullName}</p>
              <p className="text-xs text-brand-100/70">{user?.role}</p>
            </div>
            <button
              type="button"
              onClick={() => void logout()}
              aria-label="Log out"
              className="rounded-lg p-2 text-brand-100 hover:bg-brand-700/60"
            >
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        </div>
      </aside>

      <main className="min-w-0 flex-1 px-4 py-8 sm:px-8">
        <div className="mx-auto max-w-5xl">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
