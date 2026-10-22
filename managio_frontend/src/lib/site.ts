/**
 * Facts about the public site, in one place. The operator was confirmed by the owner on 2026-10-22 and the contact
 * address (the owner's own email) later the same day (docs/LEGAL.md lists what still needs their confirmation or a lawyer's review).
 */
export const SITE = {
  name: "Managio",
  /** The canonical origin. Preview deployments still point search engines here. */
  url: "https://managio.me",
  supportEmail: "nitinr0306@gmail.com",
  /** Who runs Managio, as named in the Terms and the Privacy Policy. */
  operator: "Nitin, an individual based in India",
  description:
    "Free business management for gyms, salons, coaching centres, schools, clinics and shops: members and renewals, invoices and payments, expenses, staff, tasks and approvals, with one login for every business you run.",
  /** Last change to the Privacy Policy and the Terms. */
  legalUpdated: "22 October 2026",
  /** Shown in the footer; a constant, because prerendered pages can't read the clock. */
  copyrightYear: 2026,
} as const;

/** Matches src/app/opengraph-image.alt.txt. */
const SHARING_IMAGE_ALT =
  "Managio: run every business you own from one screen. Free business management for gyms, salons, coaching centres, schools, clinics and shops.";

/**
 * Metadata for a public page: its own title, description and canonical URL, carried into the sharing cards
 * (Next.js replaces, rather than merges, a parent's openGraph and twitter objects).
 */
export function publicPageMetadata(path: string, title: string, description: string) {
  return {
    title,
    description,
    alternates: { canonical: path },
    openGraph: {
      type: "website" as const,
      siteName: "Managio",
      locale: "en_IN",
      url: path,
      title: `${title} · Managio`,
      description,
      images: [{ url: "/opengraph-image.png", width: 1200, height: 630, alt: SHARING_IMAGE_ALT }],
    },
    twitter: { card: "summary_large_image" as const, title: `${title} · Managio`, description, images: ["/twitter-image.png"] },
  };
}

/** Public pages that belong in search results and the sitemap; everything else is kept out. */
export const PUBLIC_PAGES = ["/", "/privacy", "/terms", "/security", "/contact"] as const;
