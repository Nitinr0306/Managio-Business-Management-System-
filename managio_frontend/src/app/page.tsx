import type { Metadata } from "next";
import { LandingPage } from "@/features/marketing/landing-page";
import { SITE } from "@/lib/site";

/** The landing page is fully static, so navigations to it must be instant. */
export const instant = true;

const TITLE = "Managio: free business management for gyms, salons, coaching centres and shops";

export const metadata: Metadata = {
  title: { absolute: TITLE },
  description: SITE.description,
  alternates: { canonical: "/" },
  openGraph: { type: "website", siteName: "Managio", locale: "en_IN", url: "/", title: "Managio: run every business you own from one screen", description: SITE.description },
  twitter: { card: "summary_large_image", title: "Managio: run every business you own from one screen", description: SITE.description },
};

export default function Page() {
  return <LandingPage />;
}
