import { api, staff, unwrap, type Schemas } from "@/lib/api/client";

/** Staff account actions (IDN-1 to IDN-8). Each successful sign-in style call starts the in-memory session. */

export async function signIn(body: Schemas["SignInRequest"]) {
  const session = await unwrap(api.POST("/api/v1/auth/sign-in", { body }));
  staff.accept(session);
  return session;
}

export async function signUp(body: Schemas["SignUpRequest"]) {
  const session = await unwrap(api.POST("/api/v1/auth/sign-up", { body }));
  staff.accept(session);
  return session;
}

export async function signInWithGoogle(credential: string) {
  const session = await unwrap(api.POST("/api/v1/auth/google", { body: { credential } }));
  staff.accept(session);
  return session;
}

export async function signOut() {
  try {
    await unwrap(api.POST("/api/v1/auth/sign-out"));
  } finally {
    staff.store.signOut();
  }
}

export function requestPasswordReset(email: string) {
  return unwrap(api.POST("/api/v1/auth/password-reset", { body: { email } }));
}

export function confirmPasswordReset(token: string, password: string) {
  return unwrap(api.POST("/api/v1/auth/password-reset/confirm", { body: { token, password } }));
}

export function verifyEmail(token: string) {
  return unwrap(api.POST("/api/v1/auth/verify-email", { body: { token } }));
}

export function resendVerificationEmail() {
  return unwrap(api.POST("/api/v1/me/verification-email"));
}

export function previewInvitation(token: string) {
  return unwrap(api.POST("/api/v1/invitations/preview", { body: { token } }));
}

export async function acceptInvitation(body: Schemas["AcceptRequest"]) {
  const accepted = await unwrap(api.POST("/api/v1/invitations/accept", { body }));
  if (accepted.session) {
    staff.accept(accepted.session);
  }
  return accepted;
}
