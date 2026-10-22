"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { LoadingBlock } from "@/components/ui/spinner";
import { useStaffSession } from "@/lib/auth/use-session";

/** Renders the signed-in app only for a staff session; otherwise sends the person to sign in and back. */
export function RequireStaff({ children }: { children: ReactNode }) {
  const session = useStaffSession();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (session.status === "signed-out") {
      router.replace(`/sign-in?next=${encodeURIComponent(pathname)}`);
    }
  }, [session.status, pathname, router]);

  if (session.status !== "signed-in") {
    return <FullPageLoading />;
  }
  return children;
}

export function FullPageLoading() {
  return (
    <div className="grid min-h-screen place-items-center bg-canvas">
      <LoadingBlock label="Loading Managio" />
    </div>
  );
}
