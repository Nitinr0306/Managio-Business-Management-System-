"use client";

import type { UseQueryResult } from "@tanstack/react-query";
import { ArrowLeft, ShieldAlert } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import { EmptyState, Skeleton } from "@/components/ui/layout";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { FullPageLoading } from "@/components/shell/require-staff";
import { useStaffSession } from "@/lib/auth/use-session";
import { cn } from "@/lib/cn";
import { useAdminAccess } from "./api";
import { Logo } from "@/components/brand/logo";

const SECTIONS = [
  { href: "/admin", label: "Overview" },
  { href: "/admin/businesses", label: "Businesses" },
  { href: "/admin/people", label: "People" },
  { href: "/admin/health", label: "System health" },
] as const;

/**
 * The platform console's frame (D-028). It is deliberately unlike a business's sidebar: this is Managio's own
 * operations view, not any one business, so it has a dark header and no business switcher. Until the API
 * confirms the person is an administrator, none of the console is shown.
 */
export function AdminShell({ children }: { children: ReactNode }) {
  const access = useAdminAccess();
  if (access.isPending) {
    return <FullPageLoading />;
  }
  return (
    <div className="min-h-screen bg-canvas">
      <ConsoleHeader showSections={access.isSuccess} />
      <main className="mx-auto max-w-[1240px] px-4 py-8 sm:px-8">
        {access.isSuccess ? children : <Refused error={access.error} />}
      </main>
    </div>
  );
}

function ConsoleHeader({ showSections }: { showSections: boolean }) {
  const pathname = usePathname();
  const session = useStaffSession();
  return (
    <header className="bg-ink text-white">
      <div className="mx-auto flex h-14 max-w-[1240px] items-center justify-between gap-4 px-4 sm:px-8">
        <div className="flex min-w-0 items-center gap-3">
          <Logo tone="on-dark" size="sm" className="self-center" />
          <span className="truncate text-[13px] text-white/70">Platform console</span>
        </div>
        <div className="flex items-center gap-4 text-[13px]">
          <span className="hidden text-white/70 sm:inline">{session.principal?.fullName}</span>
          <Link href="/app" className="inline-flex items-center gap-1.5 rounded text-white/90 hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white">
            <ArrowLeft className="h-4 w-4" aria-hidden />
            Your businesses
          </Link>
        </div>
      </div>
      {showSections && (
        <nav aria-label="Console" className="mx-auto max-w-[1240px] overflow-x-auto px-4 sm:px-8">
          <ul className="flex gap-6">
            {SECTIONS.map((section) => {
              const current = section.href === "/admin" ? pathname === "/admin" : pathname.startsWith(section.href);
              return (
                <li key={section.href}>
                  <Link
                    href={section.href}
                    aria-current={current ? "page" : undefined}
                    className={cn(
                      "flex h-10 items-center whitespace-nowrap border-b-2 text-[13.5px] font-medium",
                      current ? "border-white text-white" : "border-transparent text-white/70 hover:text-white",
                    )}
                  >
                    {section.label}
                  </Link>
                </li>
              );
            })}
          </ul>
        </nav>
      )}
    </header>
  );
}

/** What anyone who isn't a platform administrator sees, or the reason the check itself failed. */
function Refused({ error }: { error: unknown }) {
  if (error instanceof ApiError && error.status === 403) {
    return (
      <EmptyState
        icon={<ShieldAlert className="h-5 w-5" />}
        title="The platform console is for Managio's administrators"
        body="Your account isn't one of them. If it should be, its email needs to be on the server's administrator list and verified."
        action={<Link href="/app" className="text-[13.5px] font-medium text-cobalt hover:underline">Go to your businesses</Link>}
      />
    );
  }
  return <EmptyState title="The console couldn't load" body={errorMessage(error)} />;
}

/** Renders a console query: a skeleton while it loads, then the data, or why it can't be shown. */
export function AdminQuery<T>({ query, children }: { query: UseQueryResult<T>; children: (data: T) => ReactNode }) {
  if (query.isPending) {
    return (
      <div aria-busy="true" aria-label="Loading">
        <Skeleton className="mb-6 h-8 w-64" />
        <Skeleton className="h-28 w-full" />
        <Skeleton className="mt-6 h-64 w-full" />
      </div>
    );
  }
  if (query.isError) {
    // A 403 here means access was withdrawn while the console was open.
    return <Refused error={query.error} />;
  }
  return children(query.data);
}
