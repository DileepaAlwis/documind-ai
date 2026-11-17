import type { ButtonHTMLAttributes } from "react";
import { LoaderCircle } from "lucide-react";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "danger";
  loading?: boolean;
}

const variants = {
  primary: "bg-brand-600 text-white hover:bg-brand-700",
  secondary: "border border-stone-300 bg-white text-ink hover:bg-stone-50",
  danger: "bg-red-600 text-white hover:bg-red-700",
};

export default function Button({
  variant = "primary",
  loading = false,
  disabled,
  className = "",
  children,
  ...rest
}: ButtonProps) {
  const base =
    "inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold transition " +
    "focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2 " +
    "disabled:cursor-not-allowed disabled:opacity-60";

  return (
    <button className={`${base} ${variants[variant]} ${className}`} disabled={disabled || loading} {...rest}>
      {loading && <LoaderCircle className="h-4 w-4 animate-spin" aria-hidden="true" />}
      {children}
    </button>
  );
}
