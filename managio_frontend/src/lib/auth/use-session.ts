"use client";

import { useEffect, useSyncExternalStore } from "react";
import { portal, staff } from "@/lib/api/client";
import type { SessionState, TokenStore } from "./token-store";

const UNKNOWN = { status: "unknown", token: null, expiresAt: 0, principal: null } as const;

function useStore<P>(store: TokenStore<P>): SessionState<P> {
  return useSyncExternalStore(store.subscribe, store.getState, () => UNKNOWN as SessionState<P>);
}

/**
 * The staff session. On first use it asks the API to restore the session from the refresh cookie, so a
 * reload keeps the person signed in without the token ever being stored in the browser.
 */
export function useStaffSession() {
  const state = useStore(staff.store);
  useEffect(() => {
    if (staff.store.getState().status === "unknown") {
      void staff.refresh();
    }
  }, []);
  return state;
}

/** The portal customer session; restored the same way from its own cookie. */
export function usePortalSession() {
  const state = useStore(portal.store);
  useEffect(() => {
    if (portal.store.getState().status === "unknown") {
      void portal.refresh();
    }
  }, []);
  return state;
}
