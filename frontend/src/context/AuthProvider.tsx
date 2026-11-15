import { useCallback, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { setSessionExpiredHandler } from "../lib/api";
import * as authService from "../services/authService";
import type { RegisterPayload, User } from "../types/api";
import { AuthContext } from "./AuthContext";
import type { AuthContextValue, AuthStatus } from "./AuthContext";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [status, setStatus] = useState<AuthStatus>("loading");

  // Restore the session on page load. Concurrent calls share one request (see lib/api.ts).
  useEffect(() => {
    let active = true;
    authService
      .refreshSession()
      .then((session) => {
        if (active) {
          setUser(session.user);
          setStatus("authenticated");
        }
      })
      .catch(() => {
        if (active) {
          setUser(null);
          setStatus("anonymous");
        }
      });
    return () => {
      active = false;
    };
  }, []);

  // If a later refresh fails, drop back to the login screen.
  useEffect(() => {
    setSessionExpiredHandler(() => {
      setUser(null);
      setStatus("anonymous");
    });
    return () => setSessionExpiredHandler(null);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const session = await authService.login(email, password);
    setUser(session.user);
    setStatus("authenticated");
  }, []);

  const register = useCallback(
    async (payload: RegisterPayload) => {
      await authService.register(payload);
      await login(payload.email, payload.password);
    },
    [login],
  );

  const logout = useCallback(async () => {
    try {
      await authService.logout();
    } finally {
      setUser(null);
      setStatus("anonymous");
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ user, status, login, register, logout }),
    [user, status, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}