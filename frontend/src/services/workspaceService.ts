import { api } from "../lib/api";
import type { Workspace, WorkspaceInput } from "../types/api";

export async function listWorkspaces(query?: string): Promise<Workspace[]> {
  const { data } = await api.get<Workspace[]>("/workspaces", {
    params: query ? { q: query } : undefined,
  });
  return data;
}

export async function createWorkspace(input: WorkspaceInput): Promise<Workspace> {
  const { data } = await api.post<Workspace>("/workspaces", input);
  return data;
}

export async function updateWorkspace(id: number, input: WorkspaceInput): Promise<Workspace> {
  const { data } = await api.put<Workspace>(`/workspaces/${id}`, input);
  return data;
}

export async function deleteWorkspace(id: number): Promise<void> {
  await api.delete(`/workspaces/${id}`);
}
