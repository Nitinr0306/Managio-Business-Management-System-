import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

/**
 * The session client: attaches the access token, refreshes it once when it has expired (shared by requests
 * made at the same moment), retries, and signs out when the refresh cookie is no longer accepted.
 */

type Call = { url: string; auth: string | null; method: string };

const user = { id: 1, email: "ananya@example.com", fullName: "Ananya Rao", phone: null, emailVerified: true, hasPassword: true, googleLinked: false };
const json = (status: number, body: unknown) => new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

async function freshClient() {
  vi.resetModules();
  return import("./client");
}

describe("staff session client", () => {
  let calls: Call[];

  beforeEach(() => {
    calls = [];
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function stubFetch(handler: (request: Request, call: Call) => Response | Promise<Response>) {
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const request = input instanceof Request ? input : new Request(new URL(String(input), "http://localhost"), init);
      const call = { url: new URL(request.url, "http://localhost").pathname, auth: request.headers.get("Authorization"), method: request.method };
      calls.push(call);
      return handler(request, call);
    }));
  }

  it("refreshes an expired token once and retries the request", async () => {
    const { staff, api, unwrap } = await freshClient();
    staff.store.signIn("old-token", 900, user);
    stubFetch((_, call) => {
      if (call.url === "/api/v1/auth/refresh") return json(200, { accessToken: "new-token", expiresIn: 900, user });
      return call.auth === "Bearer new-token" ? json(200, []) : json(401, { code: "auth.required" });
    });

    await expect(unwrap(api.GET("/api/v1/me/businesses"))).resolves.toEqual([]);
    expect(calls.map((c) => `${c.url} ${c.auth ?? "-"}`)).toEqual([
      "/api/v1/me/businesses Bearer old-token",
      "/api/v1/auth/refresh -",
      "/api/v1/me/businesses Bearer new-token",
    ]);
    expect(staff.store.getState().token).toBe("new-token");
  });

  it("shares one refresh between requests that fail at the same time", async () => {
    const { staff, api, unwrap } = await freshClient();
    staff.store.signIn("old-token", 900, user);
    stubFetch(async (_, call) => {
      if (call.url === "/api/v1/auth/refresh") {
        await new Promise((resolve) => setTimeout(resolve, 10));
        return json(200, { accessToken: "new-token", expiresIn: 900, user });
      }
      return call.auth === "Bearer new-token" ? json(200, { unread: 0 }) : json(401, {});
    });

    await Promise.all([
      unwrap(api.GET("/api/v1/me/notifications/count")),
      unwrap(api.GET("/api/v1/me/notifications/count")),
      unwrap(api.GET("/api/v1/me/notifications/count")),
    ]);
    expect(calls.filter((c) => c.url === "/api/v1/auth/refresh")).toHaveLength(1);
  });

  it("signs out when the refresh cookie is refused", async () => {
    const { staff, api, unwrap } = await freshClient();
    staff.store.signIn("old-token", 900, user);
    stubFetch(() => json(401, { code: "auth.session_expired", detail: "Your session has ended. Sign in again." }));

    await expect(unwrap(api.GET("/api/v1/me/businesses"))).rejects.toMatchObject({ status: 401 });
    expect(staff.store.getState().status).toBe("signed-out");
    expect(staff.store.getState().token).toBeNull();
  });

  it("never sends the token to, or retries, sign-in endpoints", async () => {
    const { staff, api, unwrap } = await freshClient();
    const { ApiError } = await import("./errors");
    staff.store.signIn("old-token", 900, user);
    stubFetch(() => json(401, { code: "auth.invalid_credentials", detail: "That email and password don't match. Try again." }));

    const failure = unwrap(api.POST("/api/v1/auth/sign-in", { body: { email: "a@b.co", password: "x" } }));
    await expect(failure).rejects.toBeInstanceOf(ApiError);
    await expect(failure).rejects.toMatchObject({ code: "auth.invalid_credentials" });
    expect(calls).toHaveLength(1);
    expect(calls[0].auth).toBeNull();
    expect(staff.store.getState().status).toBe("signed-in");
  });

  it("refreshes ahead of time when the token is about to expire", async () => {
    const { staff, api, unwrap } = await freshClient();
    staff.store.signIn("nearly-expired", 10, user);
    stubFetch((_, call) =>
      call.url === "/api/v1/auth/refresh" ? json(200, { accessToken: "new-token", expiresIn: 900, user }) : json(200, user),
    );

    await unwrap(api.GET("/api/v1/me"));
    expect(calls.map((c) => c.url)).toEqual(["/api/v1/auth/refresh", "/api/v1/me"]);
    expect(calls[1].auth).toBe("Bearer new-token");
  });

  it("restores a session from the refresh cookie on start-up", async () => {
    const { staff } = await freshClient();
    stubFetch(() => json(200, { accessToken: "restored", expiresIn: 900, user }));
    expect(staff.store.getState().status).toBe("unknown");
    await expect(staff.refresh()).resolves.toBe(true);
    expect(staff.store.getState()).toMatchObject({ status: "signed-in", token: "restored", principal: { email: user.email } });
  });

  it("turns a dropped connection into a network error", async () => {
    const { api, unwrap } = await freshClient();
    vi.stubGlobal("fetch", vi.fn(async () => {
      throw new TypeError("Failed to fetch");
    }));
    await expect(unwrap(api.GET("/api/v1/reference/permissions"))).rejects.toMatchObject({ code: "network" });
  });

  it("keeps staff and portal sessions apart", async () => {
    const { staff, portal } = await freshClient();
    staff.store.signIn("staff-token", 900, user);
    expect(portal.store.getState().token).toBeNull();
  });
});
