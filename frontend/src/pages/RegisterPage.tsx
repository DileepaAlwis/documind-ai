import { useState } from "react";
import type { FormEvent } from "react";
import { Link, Navigate } from "react-router";
import { Lock, Mail, User } from "lucide-react";
import AuthLayout from "../layouts/AuthLayout";
import Alert from "../components/ui/Alert";
import Button from "../components/ui/Button";
import TextField from "../components/ui/TextField";
import { useAuth } from "../context/useAuth";
import { getErrorMessage } from "../utils/errors";
import {
  validateEmail,
  validateFullName,
  validatePassword,
  validatePasswordConfirmation,
} from "../utils/validation";

interface FieldErrors {
  fullName: string | null;
  email: string | null;
  password: string | null;
  confirmation: string | null;
}

const noErrors: FieldErrors = { fullName: null, email: null, password: null, confirmation: null };

export default function RegisterPage() {
  const { register, status } = useAuth();

  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [errors, setErrors] = useState<FieldErrors>(noErrors);
  const [serverError, setServerError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (status === "authenticated") {
    return <Navigate to="/dashboard" replace />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setServerError(null);

    const next: FieldErrors = {
      fullName: validateFullName(fullName),
      email: validateEmail(email),
      password: validatePassword(password),
      confirmation: validatePasswordConfirmation(password, confirmation),
    };
    setErrors(next);
    if (Object.values(next).some(Boolean)) return;

    setSubmitting(true);
    try {
      await register({ fullName: fullName.trim(), email: email.trim(), password });
    } catch (error) {
      setServerError(getErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout title="Create your account" subtitle="It takes less than a minute.">
      <form onSubmit={handleSubmit} noValidate className="space-y-5">
        {serverError && <Alert message={serverError} />}
        <TextField
          label="Full name"
          autoComplete="name"
          icon={<User className="h-4 w-4" />}
          value={fullName}
          onChange={(e) => setFullName(e.target.value)}
          error={errors.fullName}
        />
        <TextField
          label="Email"
          type="email"
          autoComplete="email"
          placeholder="you@example.com"
          icon={<Mail className="h-4 w-4" />}
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={errors.email}
        />
        <TextField
          label="Password"
          type="password"
          autoComplete="new-password"
          hint="8-72 characters, with at least one letter and one number."
          icon={<Lock className="h-4 w-4" />}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={errors.password}
        />
        <TextField
          label="Confirm password"
          type="password"
          autoComplete="new-password"
          icon={<Lock className="h-4 w-4" />}
          value={confirmation}
          onChange={(e) => setConfirmation(e.target.value)}
          error={errors.confirmation}
        />
        <Button type="submit" loading={submitting} className="w-full">
          Create account
        </Button>
      </form>
      <p className="mt-6 text-center text-sm text-stone-600">
        Already registered?{" "}
        <Link to="/login" className="font-medium text-brand-700 hover:underline">
          Log in
        </Link>
      </p>
    </AuthLayout>
  );
}
