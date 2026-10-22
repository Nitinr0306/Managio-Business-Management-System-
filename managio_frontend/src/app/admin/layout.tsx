import type { Metadata } from "next";
import { Suspense } from "react";
import { FullPageLoading, RequireStaff } from "@/components/shell/require-staff";
import { AdminShell } from "@/features/admin/admin-shell";

/**
 * The platform console (D-028), separate from every business's own screens. It needs a staff session like /app;
 * whether that person is a platform administrator is decided by the API on every request.
 */
export const instant = false;

/** Private: the platform console holds business data behind a session and must never be indexed. */
export const metadata: Metadata = { robots: { index: false, follow: false } };

export default function AdminLayout({ children }: LayoutProps<"/admin">) {
  return (
    <Suspense fallback={<FullPageLoading />}>
      <RequireStaff>
        <AdminShell>{children}</AdminShell>
      </RequireStaff>
    </Suspense>
  );
}
