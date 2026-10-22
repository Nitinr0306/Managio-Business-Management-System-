// @vitest-environment node
import { NextRequest } from "next/server";
import { afterEach, describe, expect, it, vi } from "vitest";
import { proxy } from "./proxy";

/** Headers the proxy sets for the upstream request travel as x-middleware-request-* on its response. */
function forwarded(response: Response, name: string) {
  return response.headers.get(`x-middleware-request-${name}`);
}

function apiRequest(headers: Record<string, string>) {
  return new NextRequest("https://managio.me/api/v1/auth/sign-in", { method: "POST", headers });
}

describe("proxy: /api pass-through", () => {
  afterEach(() => vi.unstubAllEnvs());

  it("vouches for the address Vercel reports, with the shared secret", () => {
    vi.stubEnv("VERCEL", "1");
    vi.stubEnv("API_PROXY_SECRET", "s3cret-s3cret-s3cret-s3cret-s3cret");
    const response = proxy(apiRequest({ "x-forwarded-for": "198.51.100.7" }));

    expect(forwarded(response, "x-managio-proxy-secret")).toBe("s3cret-s3cret-s3cret-s3cret-s3cret");
    expect(forwarded(response, "x-managio-client-ip")).toBe("198.51.100.7");
  });

  it("never passes on what a client sent in the trusted headers", () => {
    vi.stubEnv("VERCEL", "1");
    vi.stubEnv("API_PROXY_SECRET", "s3cret-s3cret-s3cret-s3cret-s3cret");
    const response = proxy(apiRequest({
      "x-forwarded-for": "198.51.100.7",
      "x-managio-client-ip": "203.0.113.66",
      "x-managio-proxy-secret": "guess",
    }));

    expect(forwarded(response, "x-managio-client-ip")).toBe("198.51.100.7");
    expect(forwarded(response, "x-managio-proxy-secret")).toBe("s3cret-s3cret-s3cret-s3cret-s3cret");
  });

  it("outside Vercel, trusts no forwarding header unless CLIENT_IP_HEADER names one", () => {
    vi.stubEnv("API_PROXY_SECRET", "s3cret-s3cret-s3cret-s3cret-s3cret");
    const untrusted = proxy(apiRequest({ "x-forwarded-for": "203.0.113.66", "x-managio-client-ip": "203.0.113.67" }));
    expect(forwarded(untrusted, "x-managio-proxy-secret")).toBeNull();
    expect(forwarded(untrusted, "x-managio-client-ip")).toBeNull();

    vi.stubEnv("CLIENT_IP_HEADER", "X-Real-IP");
    const configured = proxy(apiRequest({ "x-real-ip": "198.51.100.9", "x-forwarded-for": "203.0.113.66" }));
    expect(forwarded(configured, "x-managio-client-ip")).toBe("198.51.100.9");
  });

  it("sends nothing to vouch with when no secret is configured", () => {
    vi.stubEnv("VERCEL", "1");
    const response = proxy(apiRequest({ "x-forwarded-for": "198.51.100.7" }));
    expect(forwarded(response, "x-managio-proxy-secret")).toBeNull();
    expect(forwarded(response, "x-managio-client-ip")).toBeNull();
  });
});

describe("proxy: signed-in pages", () => {
  it("sends visitors without a refresh cookie to sign-in, remembering where they were going", () => {
    const response = proxy(new NextRequest("https://managio.me/app/12/billing?view=open"));
    expect(response.status).toBe(307);
    expect(response.headers.get("location")).toBe("https://managio.me/sign-in?next=%2Fapp%2F12%2Fbilling%3Fview%3Dopen");
  });

  it("lets a visitor with a refresh cookie through", () => {
    const response = proxy(new NextRequest("https://managio.me/admin", { headers: { cookie: "mg_rt=abc" } }));
    expect(response.headers.get("location")).toBeNull();
  });
});
