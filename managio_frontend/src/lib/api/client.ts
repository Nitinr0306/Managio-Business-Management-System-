import createClient from "openapi-fetch";
import { createTokenStore, type TokenStore } from "@/lib/auth/token-store";
import { ApiError } from "./errors";
import type { components, paths } from "./schema";

/** Response and request shapes generated from the API's OpenAPI document (npm run api:types). */
export type Schemas = components["schemas"];

/** Refresh a little before the 15-minute access token runs out, so requests rarely meet a 401. */
const REFRESH_EARLY_MS = 30_000;

type SessionBody = { accessToken: string; expiresIn: number };

/**
 * A typed API client bound to one kind of session (staff or portal customer). It attaches the access token,
 * refreshes it once when it expires (shared by concurrent requests), retries the request, and marks the
 * session signed out when the refresh cookie is no longer valid.
 */
function createSession<P, B extends SessionBody>(refreshPath: string, principalOf: (body: B) => P) {
  const store: TokenStore<P> = createTokenStore<P>();
  let inflight: Promise<boolean> | null = null;

  function refresh(): Promise<boolean> {
    inflight ??= (async () => {
      try {
        const response = await fetch(refreshPath, {
          method: "POST",
          credentials: "same-origin",
          headers: { "Content-Type": "application/json" },
          body: "{}",
        });
        if (!response.ok) {
          store.signOut();
          return false;
        }
        const body = (await response.json()) as B;
        store.signIn(body.accessToken, body.expiresIn, principalOf(body));
        return true;
      } catch {
        if (store.getState().status === "unknown") {
          store.signOut();
        }
        return false;
      } finally {
        inflight = null;
      }
    })();
    return inflight;
  }

  async function send(request: Request, token: string | null): Promise<Response> {
    const authed = new Request(request);
    if (token) {
      authed.headers.set("Authorization", `Bearer ${token}`);
    }
    try {
      return await fetch(authed);
    } catch {
      throw ApiError.network();
    }
  }

  async function fetchWithSession(request: Request): Promise<Response> {
    const isAuthCall = new URL(request.url, "http://local").pathname.includes("/auth/");
    let state = store.getState();
    if (!isAuthCall && state.token && state.expiresAt - Date.now() < REFRESH_EARLY_MS) {
      await refresh();
      state = store.getState();
    }
    const retry = request.clone();
    const response = await send(request, isAuthCall ? null : state.token);
    if (response.status !== 401 || isAuthCall || !state.token) {
      return response;
    }
    return (await refresh()) ? send(retry, store.getState().token) : response;
  }

  // Same-origin: /api/* is passed through to the API by next.config.ts. An absolute base keeps Request valid outside a page.
  const client = createClient<paths>({ baseUrl: typeof window === "undefined" ? "" : window.location.origin, fetch: fetchWithSession, credentials: "same-origin" });

  /** Records a session returned by a sign-in style endpoint. */
  function accept(body: B) {
    store.signIn(body.accessToken, body.expiresIn, principalOf(body));
  }

  return { store, client, refresh, accept };
}

export type StaffPrincipal = Schemas["UserResponse"];
export type PortalPrincipal = { customer: Schemas["CustomerSummary"]; business: Schemas["BusinessPreview"] };

/** Owners and team members. */
export const staff = createSession<StaffPrincipal, Schemas["SessionResponse"]>(
  "/api/v1/auth/refresh",
  (body) => body.user,
);

/** Customers signed in to one business's portal. */
export const portal = createSession<PortalPrincipal, Schemas["PortalSession"]>(
  "/api/v1/portal/auth/refresh",
  (body) => ({ customer: body.customer, business: body.business }),
);

export const api = staff.client;
export const portalApi = portal.client;

type FetchResult<T> = { data?: T; error?: unknown; response: Response };

/** Resolves to the response body, or throws an ApiError for any failure. */
export async function unwrap<T>(call: Promise<FetchResult<T>>): Promise<T> {
  let result: FetchResult<T>;
  try {
    result = await call;
  } catch (error) {
    throw error instanceof ApiError ? error : ApiError.network();
  }
  if (result.error !== undefined || !result.response.ok) {
    throw ApiError.fromProblem(result.response.status, result.error);
  }
  return result.data as T;
}
