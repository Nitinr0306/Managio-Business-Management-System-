import type { Metadata } from "next";
import type { ReactNode } from "react";

/**
 * Sign-in, sign-up, password and invitation pages: useful to people, not to search results, so they are kept out of
 * the index (and out of the sitemap) while links on them can still be followed.
 */
export const metadata: Metadata = { robots: { index: false, follow: true } };

export default function AuthLayout({ children }: { children: ReactNode }) {
  return children;
}
