import { NextResponse, type NextRequest } from "next/server";

/** Must match ClientAddressFilter in the API. */
const SECRET_HEADER = "x-managio-proxy-secret";
const CLIENT_IP_HEADER = "x-managio-client-ip";

/**
 * The header holding the visitor's address, set by the platform in front of this app. Vercel overwrites
 * `x-forwarded-for` with the real client address, so it can be trusted there. Anywhere else it is whatever the
 * client sent unless a proxy in front overwrites it, so it is only used when CLIENT_IP_HEADER names it.
 */
function clientIpHeader(): string | null {
  const configured = process.env.CLIENT_IP_HEADER?.trim().toLowerCase();
  if (configured) return configured;
  return process.env.VERCEL ? "x-forwarded-for" : null;
}

/**
 * `/api/*` is passed through to the API (next.config.ts). Here the pass-through vouches for the visitor's address
 * with the shared API_PROXY_SECRET, so the API's rate limits count real visitors instead of this server, while
 * anything a client sends in those headers itself is dropped (docs/ARCHITECTURE.md, "Client address").
 */
function forwardToApi(request: NextRequest) {
  const headers = new Headers(request.headers);
  headers.delete(SECRET_HEADER);
  headers.delete(CLIENT_IP_HEADER);
  const secret = process.env.API_PROXY_SECRET;
  const source = clientIpHeader();
  const address = source ? request.headers.get(source)?.split(",")[0]?.trim() : undefined;
  if (secret && address) {
    headers.set(SECRET_HEADER, secret);
    headers.set(CLIENT_IP_HEADER, address);
  }
  return NextResponse.next({ request: { headers } });
}

/**
 * An optimistic check before the signed-in app loads: with no refresh cookie there can be no session, so go
 * straight to sign-in instead of showing a loading screen first. It is only a shortcut; the API decides who
 * may do what, and an expired cookie is still caught in the browser (docs/ARCHITECTURE.md, Frontend).
 */
export function proxy(request: NextRequest) {
  if (request.nextUrl.pathname.startsWith("/api/")) {
    return forwardToApi(request);
  }
  if (request.cookies.has("mg_rt")) {
    return NextResponse.next();
  }
  const signIn = new URL("/sign-in", request.url);
  signIn.searchParams.set("next", `${request.nextUrl.pathname}${request.nextUrl.search}`);
  return NextResponse.redirect(signIn);
}

export const config = {
  matcher: ["/api/:path*", "/app", "/app/:path*", "/admin", "/admin/:path*"],
};
