import { useCallback, useMemo, useRef, useState } from "react";
import type { ReactNode } from "react";
import { CircleAlert, CircleCheck, X } from "lucide-react";
import { ToastContext } from "./ToastContext";
import type { ToastContextValue, ToastKind } from "./ToastContext";

interface Toast {
  id: number;
  message: string;
  kind: ToastKind;
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const nextId = useRef(1);

  const dismiss = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const notify = useCallback(
    (message: string, kind: ToastKind = "success") => {
      const id = nextId.current++;
      setToasts((current) => [...current, { id, message, kind }]);
      window.setTimeout(() => dismiss(id), 4000);
    },
    [dismiss],
  );

  const value = useMemo<ToastContextValue>(() => ({ notify }), [notify]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div
        aria-live="polite"
        className="pointer-events-none fixed right-4 top-4 z-50 flex w-80 max-w-[calc(100vw-2rem)] flex-col gap-2"
      >
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={`pointer-events-auto flex items-start gap-2 rounded-lg border px-3 py-2.5 text-sm shadow-md ${
              toast.kind === "success"
                ? "border-brand-100 bg-brand-50 text-brand-900"
                : "border-red-200 bg-red-50 text-red-700"
            }`}
          >
            {toast.kind === "success" ? (
              <CircleCheck className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
            ) : (
              <CircleAlert className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
            )}
            <span className="flex-1">{toast.message}</span>
            <button type="button" onClick={() => dismiss(toast.id)} aria-label="Dismiss message">
              <X className="h-4 w-4" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
