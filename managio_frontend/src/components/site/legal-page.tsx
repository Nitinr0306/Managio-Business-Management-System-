import type { ReactNode } from "react";
import { SiteFooter, SiteHeader } from "./site-chrome";

/** A readable long-form page: legal text, contact and security information. */
export function LegalPage({ title, intro, updated, children }: { title: string; intro: ReactNode; updated?: string; children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col bg-canvas text-ink">
      <a href="#content" className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded focus:bg-surface focus:px-3 focus:py-2">
        Skip to content
      </a>
      <SiteHeader />
      <main id="content" className="flex-1 px-4 py-12 sm:px-8 sm:py-16">
        <article className="mx-auto max-w-[720px]">
          <h1 className="text-[34px] font-bold leading-tight tracking-[-0.02em] text-balance sm:text-[40px]">{title}</h1>
          {updated && <p className="mt-3 text-[13.5px] text-muted">Last updated {updated}</p>}
          <div className="mt-6 text-[17px] leading-relaxed text-ink text-pretty">{intro}</div>
          <div className="mt-10 space-y-10">{children}</div>
        </article>
      </main>
      <SiteFooter />
    </div>
  );
}

/** A numbered or plain section of a long-form page. */
export function LegalSection({ id, title, children }: { id?: string; title: string; children: ReactNode }) {
  return (
    <section id={id} aria-labelledby={id ? `${id}-title` : undefined} className="scroll-mt-6">
      <h2 id={id ? `${id}-title` : undefined} className="text-[21px] font-semibold tracking-tight text-balance">{title}</h2>
      <div className="mt-3 space-y-3 text-[15.5px] leading-[1.7] text-ink/90 [&_a]:font-medium [&_a]:text-cobalt [&_a]:underline [&_a]:underline-offset-2 [&_li]:pl-1 [&_strong]:font-semibold [&_ul]:list-disc [&_ul]:space-y-1.5 [&_ul]:pl-5">
        {children}
      </div>
    </section>
  );
}
