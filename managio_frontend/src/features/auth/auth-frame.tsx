import Link from "next/link";
import type { ReactNode } from "react";
import { Logo } from "@/components/brand/logo";
import { cn } from "@/lib/cn";

/** The frame for sign-in style pages: a deep band with the wordmark and a card that overlaps it (Variant B). */
export function AuthFrame({ children, wide = false, footer }: { children: ReactNode; wide?: boolean; footer?: ReactNode }) {
  return (
    <div className="min-h-screen bg-canvas">
      <div className="relative h-[40vh] min-h-[280px] overflow-hidden bg-ink px-6 pt-7 sm:px-10">
        <div
          aria-hidden
          className="pointer-events-none absolute inset-0 opacity-[0.16] [background-image:linear-gradient(90deg,#fff_1px,transparent_1px)] [background-size:28px_100%]"
        />
        <div aria-hidden className="pointer-events-none absolute -right-24 -top-24 h-80 w-80 rounded-full bg-cobalt/40 blur-3xl" />
        <Link href="/" aria-label="Managio home" className="relative inline-flex rounded-md">
          <Logo tone="on-dark" />
        </Link>
      </div>
      <main className="-mt-[24vh] px-4 pb-16">
        <div className={cn("relative mx-auto w-full rounded-2xl border border-line bg-surface p-6 shadow-pop sm:p-9", wide ? "max-w-[640px]" : "max-w-[480px]")}>
          {children}
        </div>
        {footer && <div className="mx-auto mt-6 max-w-[480px] text-center text-[13.5px] text-muted">{footer}</div>}
        <nav aria-label="Legal" className="mx-auto mt-8 flex max-w-[480px] justify-center gap-5 text-[12.5px] text-muted">
          <Link href="/privacy" className="hover:text-ink">Privacy Policy</Link>
          <Link href="/terms" className="hover:text-ink">Terms of Service</Link>
          <Link href="/contact" className="hover:text-ink">Contact</Link>
        </nav>
      </main>
    </div>
  );
}

export function AuthHeading({ title, subtitle }: { title: ReactNode; subtitle?: ReactNode }) {
  return (
    <div className="mb-6">
      <h1 className="text-[26px] font-bold leading-8 tracking-tight text-ink text-balance">{title}</h1>
      {subtitle && <p className="mt-1.5 text-[14px] text-muted text-pretty">{subtitle}</p>}
    </div>
  );
}

/** The agreement shown wherever a new account is created. */
export function AgreementNote() {
  return (
    <p className="text-center text-[12.5px] leading-relaxed text-muted">
      By creating an account you agree to the{" "}
      <Link href="/terms" className="font-medium text-ink underline underline-offset-2 hover:text-cobalt">Terms of Service</Link> and confirm you
      have read the <Link href="/privacy" className="font-medium text-ink underline underline-offset-2 hover:text-cobalt">Privacy Policy</Link>.
    </p>
  );
}
