import type { MetadataRoute } from "next";
import { PUBLIC_PAGES, SITE } from "@/lib/site";

/** Only the public, canonical pages. Sign-in style pages are noindex and the app is private, so neither is listed. */
export default function sitemap(): MetadataRoute.Sitemap {
  return PUBLIC_PAGES.map((path) => ({
    url: path === "/" ? SITE.url : `${SITE.url}${path}`,
    changeFrequency: path === "/" ? "monthly" : "yearly",
    priority: path === "/" ? 1 : 0.5,
  }));
}
