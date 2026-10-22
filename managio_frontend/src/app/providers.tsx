"use client";

import "@/lib/zod-config";
import { QueryClientProvider } from "@tanstack/react-query";
import { useEffect, useState, type ReactNode } from "react";
import { Toaster } from "sonner";
import { portal, staff } from "@/lib/api/client";
import { makeQueryClient } from "@/lib/query-client";

/**
 * App-wide client providers. Signing out (here, in another tab, or because the session expired) drops every
 * cached response, so the next person on this browser never sees the previous person's data.
 */
export function Providers({ children }: { children: ReactNode }) {
  const [queryClient] = useState(makeQueryClient);

  useEffect(() => {
    const channel = typeof BroadcastChannel === "undefined" ? null : new BroadcastChannel("managio-session");
    const watch = (store: typeof staff.store | typeof portal.store, name: string) => {
      let previous = store.getState().status;
      return store.subscribe(() => {
        const status = store.getState().status;
        if (previous === "signed-in" && status === "signed-out") {
          queryClient.clear();
          channel?.postMessage({ type: "signed-out", session: name });
        }
        previous = status;
      });
    };
    const stopStaff = watch(staff.store, "staff");
    const stopPortal = watch(portal.store, "portal");
    if (channel) {
      channel.onmessage = (event: MessageEvent<{ type: string; session: string }>) => {
        if (event.data.type !== "signed-out") return;
        const store = event.data.session === "portal" ? portal.store : staff.store;
        if (store.getState().status === "signed-in") store.signOut();
      };
    }
    return () => {
      stopStaff();
      stopPortal();
      channel?.close();
    };
  }, [queryClient]);

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <Toaster position="bottom-right" toastOptions={{ className: "font-sans" }} closeButton />
    </QueryClientProvider>
  );
}
