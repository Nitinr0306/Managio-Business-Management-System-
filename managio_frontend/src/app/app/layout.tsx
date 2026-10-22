import type { Metadata } from "next";
import { Suspense } from "react";
import { FullPageLoading, RequireStaff } from "@/components/shell/require-staff";

/**
 * Everything under /app needs a staff session. The access token lives only in browser memory (D-009), so the
 * server prerenders the loading shell and the signed-in UI renders in the browser. Instant-navigation
 * validation can't see past that gate, so this subtree opts out of it (docs/DECISIONS.md D-024).
 */
export const instant = false;

/** Private: the signed-in app holds business data behind a session and must never be indexed. */
export const metadata: Metadata = { robots: { index: false, follow: false } };

export default function AppLayout({ children }: LayoutProps<"/app">) {
  return (
    <Suspense fallback={<FullPageLoading />}>
      <RequireStaff>{children}</RequireStaff>
    </Suspense>
  );
}
