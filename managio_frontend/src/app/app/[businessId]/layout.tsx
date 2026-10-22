import { Suspense } from "react";
import { BusinessShell, ShellSkeleton } from "@/components/shell/business-shell";

export default function BusinessLayout({ children }: LayoutProps<"/app/[businessId]">) {
  return (
    <Suspense fallback={<ShellSkeleton />}>
      <BusinessShell>{children}</BusinessShell>
    </Suspense>
  );
}
