import type { ReactNode } from "react";
import { FileSearch, Laptop, Quote, ShieldCheck } from "lucide-react";

const points = [
  { icon: Quote, text: "Every answer points to the document and page it came from." },
  { icon: ShieldCheck, text: "Your files stay private to your account." },
  { icon: Laptop, text: "Runs on a local language model. No paid API." },
];

export default function AuthLayout({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: ReactNode;
}) {
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <aside className="hidden flex-col justify-between bg-brand-900 p-12 text-brand-50 lg:flex">
        <div className="flex items-center gap-2 text-lg font-semibold">
          <FileSearch className="h-6 w-6" aria-hidden="true" />
          DocuMind AI
        </div>
        <div>
          <h2 className="max-w-md text-3xl font-semibold leading-snug">
            Ask your documents. Get answers you can verify.
          </h2>
          <ul className="mt-8 space-y-4">
            {points.map(({ icon: Icon, text }) => (
              <li key={text} className="flex items-start gap-3 text-brand-100">
                <Icon className="mt-0.5 h-5 w-5 shrink-0" aria-hidden="true" />
                <span>{text}</span>
              </li>
            ))}
          </ul>
        </div>
        <p className="text-sm text-brand-100/70">Portfolio project</p>
      </aside>

      <main className="flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-sm">
          <div className="mb-8 flex items-center gap-2 text-lg font-semibold text-brand-700 lg:hidden">
            <FileSearch className="h-6 w-6" aria-hidden="true" />
            DocuMind AI
          </div>
          <h1 className="text-2xl font-semibold">{title}</h1>
          <p className="mt-1.5 text-sm text-stone-600">{subtitle}</p>
          <div className="mt-8">{children}</div>
        </div>
      </main>
    </div>
  );
}