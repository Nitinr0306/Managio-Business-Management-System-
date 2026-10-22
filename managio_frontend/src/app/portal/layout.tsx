import type { Metadata } from "next";
import { Suspense } from "react";

/**
 * The customer portal. Like /app, the session lives in browser memory (D-009, D-022), so the signed-in view
 * renders in the browser and this subtree opts out of instant-navigation validation (D-024).
 */
export const instant = false;

/** Private: the customer portal holds business data behind a session and must never be indexed. */
export const metadata: Metadata = { robots: { index: false, follow: false } };

export default function PortalLayout({ children }: LayoutProps<"/portal">) {
  return <Suspense>{children}</Suspense>;
}
