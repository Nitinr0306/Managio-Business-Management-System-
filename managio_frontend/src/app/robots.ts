import type { MetadataRoute } from "next";
import { SITE } from "@/lib/site";

/**
 * Crawlers may read the public pages. The signed-in app, the console, the portal and the API hold private business
 * data behind a session and have nothing to index. Sign-in style pages stay crawlable so their noindex is seen.
 */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: [{ userAgent: "*", allow: "/", disallow: ["/app$", "/app/", "/admin", "/portal", "/api/"] }],
    sitemap: `${SITE.url}/sitemap.xml`,
    host: SITE.url,
  };
}
