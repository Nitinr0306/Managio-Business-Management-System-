import { expect, type APIRequestContext, type Page } from "@playwright/test";

/**
 * Reads the API as the person signed in on `page`, to check what a UI action really stored. It exchanges the
 * page's own refresh cookie for an access token, exactly as the app does.
 */
export async function apiAs(page: Page) {
  const refreshed = await page.request.post("/api/v1/auth/refresh", { data: {} });
  expect(refreshed.ok(), "the page should have a session").toBeTruthy();
  const { accessToken } = (await refreshed.json()) as { accessToken: string };
  return client(page.request, accessToken);
}

export function client(request: APIRequestContext, token: string) {
  const headers = { Authorization: `Bearer ${token}` };
  return {
    token,
    async get<T = unknown>(path: string): Promise<{ status: number; body: T }> {
      const response = await request.get(`/api/v1${path}`, { headers });
      const text = await response.text();
      return { status: response.status(), body: (text ? JSON.parse(text) : undefined) as T };
    },
    async post<T = unknown>(path: string, data: unknown = {}): Promise<{ status: number; body: T }> {
      const response = await request.post(`/api/v1${path}`, { headers, data });
      const text = await response.text();
      return { status: response.status(), body: (text ? JSON.parse(text) : undefined) as T };
    },
  };
}

export const businessIdFrom = (page: Page) => Number(new URL(page.url()).pathname.split("/")[2]);
