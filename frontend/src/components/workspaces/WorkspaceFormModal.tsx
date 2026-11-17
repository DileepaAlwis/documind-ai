import { useId, useState } from "react";
import type { FormEvent } from "react";
import Alert from "../ui/Alert";
import Button from "../ui/Button";
import Modal from "../ui/Modal";
import TextField from "../ui/TextField";
import { createWorkspace, updateWorkspace } from "../../services/workspaceService";
import type { Workspace } from "../../types/api";
import { getErrorMessage } from "../../utils/errors";
import { validateWorkspaceDescription, validateWorkspaceName } from "../../utils/validation";

interface Props {
  open: boolean;
  /** null means "create a new workspace" */
  workspace: Workspace | null;
  onClose: () => void;
  onSaved: (saved: Workspace, created: boolean) => void;
}

export default function WorkspaceFormModal({ open, workspace, onClose, onSaved }: Props) {
  return (
    <Modal open={open} title={workspace ? "Edit workspace" : "New workspace"} onClose={onClose}>
      <WorkspaceForm workspace={workspace} onCancel={onClose} onSaved={onSaved} />
    </Modal>
  );
}

function WorkspaceForm({
  workspace,
  onCancel,
  onSaved,
}: {
  workspace: Workspace | null;
  onCancel: () => void;
  onSaved: Props["onSaved"];
}) {
  const descriptionId = useId();
  const [name, setName] = useState(workspace?.name ?? "");
  const [description, setDescription] = useState(workspace?.description ?? "");
  const [errors, setErrors] = useState<{ name: string | null; description: string | null }>({
    name: null,
    description: null,
  });
  const [serverError, setServerError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setServerError(null);

    const next = {
      name: validateWorkspaceName(name),
      description: validateWorkspaceDescription(description),
    };
    setErrors(next);
    if (next.name || next.description) return;

    setSubmitting(true);
    try {
      const input = { name: name.trim(), description: description.trim() };
      const saved = workspace
        ? await updateWorkspace(workspace.id, input)
        : await createWorkspace(input);
      onSaved(saved, workspace === null);
    } catch (error) {
      setServerError(getErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-4">
      {serverError && <Alert message={serverError} />}
      <TextField
        label="Name"
        autoFocus
        value={name}
        onChange={(e) => setName(e.target.value)}
        error={errors.name}
      />
      <div>
        <label htmlFor={descriptionId} className="mb-1.5 block text-sm font-medium text-ink">
          Description <span className="font-normal text-stone-500">(optional)</span>
        </label>
        <textarea
          id={descriptionId}
          rows={3}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          aria-invalid={errors.description ? true : undefined}
          className={`block w-full rounded-lg border bg-white px-3 py-2.5 text-sm text-ink focus:outline-none focus:ring-2 ${
            errors.description
              ? "border-red-400 focus:ring-red-200"
              : "border-stone-300 focus:border-brand-500 focus:ring-brand-100"
          }`}
        />
        {errors.description && (
          <p role="alert" className="mt-1.5 text-sm text-red-600">
            {errors.description}
          </p>
        )}
      </div>
      <div className="flex justify-end gap-2 pt-2">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" loading={submitting}>
          {workspace ? "Save changes" : "Create workspace"}
        </Button>
      </div>
    </form>
  );
}
