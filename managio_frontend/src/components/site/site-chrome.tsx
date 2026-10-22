import Link from "next/link";
import { Logo } from "@/components/brand/logo";
import { cn } from "@/lib/cn";
import { SITE } from "@/lib/site";

/** The header of the public pages other than the landing page (legal, contact, security). */
export function SiteHeader() {
  return (
    <header className="border-b border-line bg-surface">
      <div className="mx-auto flex h-16 max-w-[1200px] items-center justify-between px-4 sm:px-8">
        <Link href="/" aria-label="Managio home" className="rounded-md">
          <Logo />
        </Link>
        <nav aria-label="Main" className="flex items-center gap-1 sm:gap-2">
          <Link href="/sign-in" className="whitespace-nowrap rounded-md px-3 py-2 text-[14px] font-medium text-ink hover:text-cobalt">Sign in</Link>
          <Link href="/sign-up" className="whitespace-nowrap rounded-md bg-cobalt px-3.5 py-2 text-[14px] font-semibold text-white hover:bg-cobalt-dark">
            Create an account
          </Link>
        </nav>
      </div>
    </header>
  );
}

/** Standard footer groups: help and legal only. Signing in and creating an account live in the header and the hero. */
const FOOTER_GROUPS = [
  {
    title: "Help",
    links: [
      { href: "/contact", label: "Contact" },
      { href: "/security", label: "Security" },
    ],
  },
  {
    title: "Legal",
    links: [
      { href: "/privacy", label: "Privacy Policy" },
      { href: "/terms", label: "Terms of Service" },
    ],
  },
] as const;

/** The footer of every public page, with the legal links search engines and Google's consent screen look for. */
export function SiteFooter({ className }: { className?: string }) {
  return (
    <footer className={cn("border-t border-line bg-canvas", className)}>
      <div className="mx-auto grid max-w-[1200px] gap-8 px-4 pb-8 pt-12 text-[13.5px] text-muted sm:px-8 md:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)]">
        <div className="flex max-w-[360px] flex-col gap-3">
          <Link href="/" aria-label="Managio home" className="w-fit rounded-md">
            <Logo size="sm" />
          </Link>
          <p className="leading-relaxed">Free business management for gyms, salons, coaching centres, schools, clinics and shops.</p>
        </div>
        <nav aria-label="Footer" className="grid grid-cols-2 gap-8">
          {FOOTER_GROUPS.map((group) => (
            <div key={group.title}>
              <h2 className="text-[13px] font-semibold text-ink">{group.title}</h2>
              <ul className="mt-3 space-y-2.5">
                {group.links.map((link) => (
                  <li key={link.href}>
                    <Link href={link.href} className="hover:text-ink">
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </nav>
      </div>
      <div className="mx-auto max-w-[1200px] px-4 sm:px-8">
        <p className="border-t border-line py-6 text-[12.5px] text-muted">© {SITE.copyrightYear} Managio</p>
      </div>
    </footer>
  );
}
