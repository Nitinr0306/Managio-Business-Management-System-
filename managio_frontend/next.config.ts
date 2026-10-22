import type { NextConfig } from "next";

/**
 * The browser only ever talks to this origin. Requests to /api/* are passed through to the Spring API, so the
 * HttpOnly refresh-token cookies are first-party and no CORS is needed (docs/ARCHITECTURE.md, D-009).
 */
const apiOrigin = process.env.API_ORIGIN ?? "http://localhost:8081";

/**
 * Only this origin and Google Identity Services (script, styles, its button frame and calls) may load anything.
 * Scripts keep 'unsafe-inline' because Next.js inlines its page data: removing it needs a nonce on every request,
 * which would end the prerendering of the public pages (docs/ARCHITECTURE.md, "Security headers"). React's
 * escaping and the absence of raw HTML injection stay the main defence against script injection; the policy
 * still stops plugins, framing, foreign scripts and data being sent anywhere but here.
 * The development server also needs 'unsafe-eval' for fast refresh.
 */
const contentSecurityPolicy = [
  "default-src 'self'",
  `script-src 'self' 'unsafe-inline'${process.env.NODE_ENV === "development" ? " 'unsafe-eval'" : ""} https://accounts.google.com/gsi/client`,
  "style-src 'self' 'unsafe-inline' https://accounts.google.com/gsi/style",
  "img-src 'self' data: blob:",
  "font-src 'self' data:",
  "connect-src 'self' https://accounts.google.com/gsi/",
  "frame-src https://accounts.google.com/gsi/",
  "frame-ancestors 'none'",
  "form-action 'self'",
  "base-uri 'self'",
  "object-src 'none'",
].join("; ");

const securityHeaders = [
  { key: "Content-Security-Policy", value: contentSecurityPolicy },
  // Browsers ignore this over plain http, so it only takes effect on the HTTPS deployment.
  { key: "Strict-Transport-Security", value: "max-age=63072000; includeSubDomains" },
  // Google's sign-in popup needs to talk back to this window.
  { key: "Cross-Origin-Opener-Policy", value: "same-origin-allow-popups" },
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "X-Frame-Options", value: "DENY" },
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
];

const nextConfig: NextConfig = {
  // A self-contained server for the Docker image (Vercel ignores it). The /api rewrite target is fixed at build
  // time, so the image is built with the API_ORIGIN it will talk to.
  output: "standalone",
  cacheComponents: true,
  partialPrefetching: true,
  reactCompiler: true,
  poweredByHeader: false,
  experimental: {
    // Signed-in pages render in the browser (the access token never reaches the server), so only segments that
    // declare `instant` are checked for instant navigation: the public pages (docs/DECISIONS.md D-024).
    instantInsights: { validationLevel: "manual-warning" },
  },
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${apiOrigin}/api/:path*` }];
  },
  async headers() {
    const noindex = (source: string, value: string) => ({ source, headers: [{ key: "X-Robots-Tag", value }] });
    return [
      { source: "/:path*", headers: securityHeaders },
      // Private areas and the API never belong in search results, even if a crawler finds a link (robots.ts too).
      ...["/app", "/app/:path*", "/admin", "/admin/:path*", "/portal", "/portal/:path*", "/api/:path*"].map((source) =>
        noindex(source, "noindex, nofollow"),
      ),
      ...["/sign-in", "/sign-up", "/forgot-password", "/reset-password", "/verify-email", "/invite"].map((source) =>
        noindex(source, "noindex"),
      ),
    ];
  },
  // One canonical host: www.managio.me answers with a permanent redirect to managio.me (Vercel serves HTTPS only).
  async redirects() {
    return [
      {
        source: "/:path*",
        has: [{ type: "host", value: "www.managio.me" }],
        destination: "https://managio.me/:path*",
        permanent: true,
      },
    ];
  },
};

export default nextConfig;
