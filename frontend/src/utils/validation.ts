const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,72}$/;

export function validateFullName(value: string): string | null {
  const name = value.trim();
  if (!name) return "Full name is required.";
  if (name.length > 150) return "Full name must be 150 characters or fewer.";
  return null;
}

export function validateEmail(value: string): string | null {
  const email = value.trim();
  if (!email) return "Email is required.";
  if (email.length > 255 || !EMAIL_PATTERN.test(email)) return "Enter a valid email address.";
  return null;
}

export function validatePassword(value: string): string | null {
  if (!value) return "Password is required.";
  if (!PASSWORD_PATTERN.test(value)) {
    return "Use 8-72 characters with at least one letter and one number.";
  }
  return null;
}

export function validatePasswordConfirmation(password: string, confirmation: string): string | null {
  if (!confirmation) return "Please confirm your password.";
  if (password !== confirmation) return "Passwords do not match.";
  return null;
}
