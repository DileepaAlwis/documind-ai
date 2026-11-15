import axios from "axios";
import type { AxiosError, InternalAxiosRequestConfig } from "axios";
import { tokenStore } from "./tokenStore";
import type { AuthResponse } from "../types/api";

export const API_BASE_URL: string =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";

/** Client for authenticated calls. Adds the bearer token and refreshes it on 401. */
export const api = axios.create({ baseURL: API_BASE_URL, withCredentials: true });

/** Bare client for /auth/* calls. No interceptors. */
export const authApi = axios.create({ baseURL: API_BASE_URL, withCredentials: true });

let refreshPromise: Promise<AuthResponse> | null = null;
let sessionExpiredHandler: (() => void) | null = null;

export function setSessionExpiredHandler(handler: (() => void) | null): void {
  sessionExpiredHandler = handler;
}

/** Exchanges the refresh cookie for a new access token. Concurrent callers share one request. */
export function refreshSession(): Promise<AuthResponse> {
  if (!refreshPromise) {
    refreshPromise = authApi
      .post<AuthResponse>("/auth/refresh")
      .then((response) => {
        tokenStore.set(response.data.accessToken);
        return response.data;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

api.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

type RetryableConfig = InternalAxiosRequestConfig & { _retried?: boolean };

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetryableConfig | undefined;

    if (error.response?.status === 401 && original && !original._retried) {
      original._retried = true;
      try {
        const session = await refreshSession();
        original.headers.set("Authorization", `Bearer ${session.accessToken}`);
        return api(original);
      } catch (refreshError) {
        tokenStore.clear();
        sessionExpiredHandler?.();
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  },
);
