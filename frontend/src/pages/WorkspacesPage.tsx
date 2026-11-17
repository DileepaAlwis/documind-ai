import { useEffect, useState } from "react";
import { FolderPlus, Pencil, Plus, Search, Trash2 } from "lucide-react";
import Alert from "../components/ui/Alert";
import Button from "../components/ui/Button";
import Modal from "../components/ui/Modal";
import { useToast } from "../components/toast/useToast";
import WorkspaceFormModal from "../components/workspaces/WorkspaceFormModal";
import { useDebouncedValue } from "../hooks/useDebouncedValue";
import { deleteWorkspace, listWorkspaces } from "../services/workspaceService";
import type { Workspace } from "../types/api";
import { getErrorMessage } from "../utils/errors";

function plural(count: number, word: string): string {
  return `${count} ${word}${count === 1 ? "" : "s"}`;
}

export default function WorkspacesPage() {
  const { notify } = useToast();
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search.trim(), 300);

  const [workspaces, setWorkspaces] = useState<Workspace[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  const [editing, setEditing] = useState<Workspace | "new" | null>(null);
  const [deleting, setDeleting] = useState<Workspace | null>(null);
  const [deleteBusy, setDeleteBusy] = useState(false);

  useEffect(() => {
    let active = true;
    listWorkspaces(debouncedSearch)
      .then((data) => {
        if (active) {
          setWorkspaces(data);
          setLoadError(null);
        }
      })
      .catch((error) => {
        if (active) setLoadError(getErrorMessage(error));
      });
    return () => {
      active = false;
    };
  }, [debouncedSearch, reloadKey]);

  function reload() {
    setReloadKey((key) => key + 1);
  }

  function handleSaved(saved: Workspace, created: boolean) {
    notify(created ? `Created “${saved.name}”.` : `Saved “${saved.name}”.`);
    setEditing(null);
    reload();
  }

  async function confirmDelete() {
    if (!deleting) return;
    setDeleteBusy(true);
    try {
      await deleteWorkspace(deleting.id);
      notify(`Deleted “${deleting.name}”.`);
      setDeleting(null);
      reload();
    } catch (error) {
      notify(getErrorMessage(error), "error");
    } finally {
      setDeleteBusy(false);
    }
  }

  const isSearching = debouncedSearch.length > 0;

  return (
    <div>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold">Workspaces</h1>
          <p className="mt-1 text-sm text-stone-600">
            A workspace groups related documents, so you can ask questions across all of them.
          </p>
        </div>
        <Button type="button" onClick={() => setEditing("new")}>
          <Plus className="h-4 w-4" aria-hidden="true" />
          New workspace
        </Button>
      </div>

      <div className="relative mt-6 max-w-sm">
        <label htmlFor="workspace-search" className="sr-only">
          Search workspaces
        </label>
        <Search
          className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-stone-400"
          aria-hidden="true"
        />
        <input
          id="workspace-search"
          type="search"
          placeholder="Search workspaces"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="block w-full rounded-lg border border-stone-300 bg-white py-2.5 pl-10 pr-3 text-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-100"
        />
      </div>

      <div className="mt-6">
        {loadError && (
          <div className="space-y-3">
            <Alert message={loadError} />
            <Button type="button" variant="secondary" onClick={reload}>
              Try again
            </Button>
          </div>
        )}

        {!loadError && workspaces === null && <p className="text-sm text-stone-500">Loading workspaces…</p>}

        {!loadError && workspaces !== null && workspaces.length === 0 && (
          <div className="rounded-xl border border-dashed border-stone-300 bg-white px-6 py-14 text-center">
            <FolderPlus className="mx-auto h-10 w-10 text-stone-400" aria-hidden="true" />
            {isSearching ? (
              <p className="mt-3 text-stone-600">No workspaces match “{debouncedSearch}”.</p>
            ) : (
              <>
                <p className="mt-3 font-medium">Create your first workspace</p>
                <p className="mt-1 text-sm text-stone-600">
                  Think of one per course, project or topic.
                </p>
                <Button type="button" className="mt-5" onClick={() => setEditing("new")}>
                  <Plus className="h-4 w-4" aria-hidden="true" />
                  New workspace
                </Button>
              </>
            )}
          </div>
        )}

        {!loadError && workspaces !== null && workspaces.length > 0 && (
          <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {workspaces.map((workspace) => (
              <li key={workspace.id} className="flex flex-col rounded-xl border border-stone-200 bg-white p-5">
                <h2 className="truncate text-base font-semibold">{workspace.name}</h2>
                <p className="mt-1 line-clamp-2 min-h-10 text-sm text-stone-600">
                  {workspace.description ?? "No description"}
                </p>
                <p className="mt-4 text-xs text-stone-500">
                  {plural(workspace.documentCount, "document")} · {workspace.readyDocumentCount} ready
                </p>
                <p className="text-xs text-stone-500">
                  Updated {new Date(workspace.updatedAt).toLocaleDateString()}
                </p>
                <div className="mt-4 flex gap-2">
                  <Button
                    type="button"
                    variant="secondary"
                    className="flex-1"
                    onClick={() => setEditing(workspace)}
                  >
                    <Pencil className="h-4 w-4" aria-hidden="true" />
                    Edit
                  </Button>
                  <Button
                    type="button"
                    variant="secondary"
                    aria-label={`Delete ${workspace.name}`}
                    onClick={() => setDeleting(workspace)}
                  >
                    <Trash2 className="h-4 w-4" aria-hidden="true" />
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      <WorkspaceFormModal
        open={editing !== null}
        workspace={editing === null || editing === "new" ? null : editing}
        onClose={() => setEditing(null)}
        onSaved={handleSaved}
      />

      <Modal open={deleting !== null} title="Delete workspace?" onClose={() => setDeleting(null)}>
        {deleting && (
          <>
            <p className="text-sm text-stone-700">
              &ldquo;{deleting.name}&rdquo; and every document inside it will be permanently removed.
              This cannot be undone.
            </p>
            <div className="mt-6 flex justify-end gap-2">
              <Button type="button" variant="secondary" onClick={() => setDeleting(null)}>
                Cancel
              </Button>
              <Button type="button" variant="danger" loading={deleteBusy} onClick={() => void confirmDelete()}>
                Delete
              </Button>
            </div>
          </>
        )}
      </Modal>
    </div>
  );
}