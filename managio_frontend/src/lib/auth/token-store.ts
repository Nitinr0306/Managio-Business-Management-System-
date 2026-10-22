/**
 * Holds one signed-in session in memory: the short-lived access token and who it belongs to. The refresh
 * token never reaches JavaScript; it lives in an HttpOnly cookie and is exchanged through /refresh (D-009).
 * React reads the store with useSyncExternalStore.
 */
export type SessionState<P> = {
  status: "unknown" | "signed-in" | "signed-out";
  token: string | null;
  expiresAt: number;
  principal: P | null;
};

export type TokenStore<P> = {
  getState(): SessionState<P>;
  subscribe(listener: () => void): () => void;
  signIn(token: string, expiresInSeconds: number, principal: P): void;
  updatePrincipal(principal: P): void;
  signOut(): void;
};

export function createTokenStore<P>(): TokenStore<P> {
  let state: SessionState<P> = { status: "unknown", token: null, expiresAt: 0, principal: null };
  const listeners = new Set<() => void>();
  const emit = () => listeners.forEach((listener) => listener());

  return {
    getState: () => state,
    subscribe(listener) {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    signIn(token, expiresInSeconds, principal) {
      state = { status: "signed-in", token, expiresAt: Date.now() + expiresInSeconds * 1000, principal };
      emit();
    },
    updatePrincipal(principal) {
      state = { ...state, principal };
      emit();
    },
    signOut() {
      state = { status: "signed-out", token: null, expiresAt: 0, principal: null };
      emit();
    },
  };
}
