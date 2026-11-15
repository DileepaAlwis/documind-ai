import { authApi, refreshSession } from "../lib/api";
import { tokenStore } from "../lib/tokenStore";
import type { AuthResponse, RegisterPayload, User } from "../types/api";

export { refreshSession };

export async function register(payload: RegisterPayload): Promise<User> {
  const { data } = await authApi.post<User>("/auth/register", payload);
  return data;
}

export async function login(email: string, password: string): Promise<AuthResponse> {
  const { data } = await authApi.post<AuthResponse>("/auth/login", { email, password });
  tokenStore.set(data.accessToken);
  return data;
}

export async function logout(): Promise<void> {
  try {
    await authApi.post("/auth/logout");
  } finally {
    tokenStore.clear();
  }
}
