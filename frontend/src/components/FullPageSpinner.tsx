import { LoaderCircle } from "lucide-react";

export default function FullPageSpinner() {
  return (
    <div className="flex min-h-screen items-center justify-center" role="status" aria-label="Loading">
      <LoaderCircle className="h-8 w-8 animate-spin text-brand-600" />
    </div>
  );
}
