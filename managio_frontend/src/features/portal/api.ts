import { portal, portalApi, unwrap, type Schemas } from "@/lib/api/client";

/** Customer portal sign-in and account actions (POR-1 to POR-6). */

export function businessPreview(code: string) {
  return unwrap(portalApi.GET("/api/v1/portal/auth/business/{code}", { params: { path: { code } } }));
}

export async function portalSignIn(body: Schemas["SignInBody"]) {
  const session = await unwrap(portalApi.POST("/api/v1/portal/auth/sign-in", { body }));
  portal.accept(session);
  return session;
}

export async function portalSignUp(body: Schemas["SignUpBody"]) {
  const session = await unwrap(portalApi.POST("/api/v1/portal/auth/sign-up", { body }));
  portal.accept(session);
  return session;
}

export async function portalSetPassword(token: string, password: string) {
  const session = await unwrap(portalApi.POST("/api/v1/portal/auth/set-password", { body: { token, password } }));
  portal.accept(session);
  return session;
}

export function portalRequestReset(businessCode: string, identifier: string) {
  return unwrap(portalApi.POST("/api/v1/portal/auth/password-reset", { body: { businessCode, identifier } }));
}

export async function portalSignOut() {
  try {
    await unwrap(portalApi.POST("/api/v1/portal/auth/sign-out"));
  } finally {
    portal.store.signOut();
  }
}

export async function portalChangePassword(body: Schemas["ChangePasswordBody"]) {
  const session = await unwrap(portalApi.POST("/api/v1/portal/me/password", { body }));
  portal.accept(session);
  return session;
}
