"use client";

import { useEffect, useRef, useState } from "react";

type GoogleId = {
  accounts: {
    id: {
      initialize: (options: { client_id: string; callback: (response: { credential: string }) => void; ux_mode?: string }) => void;
      renderButton: (element: HTMLElement, options: Record<string, unknown>) => void;
    };
  };
};

declare global {
  interface Window {
    google?: GoogleId;
  }
}

const SCRIPT = "https://accounts.google.com/gsi/client";
const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;

function loadScript(): Promise<void> {
  if (window.google?.accounts) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>(`script[src="${SCRIPT}"]`);
    const script = existing ?? document.createElement("script");
    script.addEventListener("load", () => resolve(), { once: true });
    script.addEventListener("error", () => reject(new Error("Google sign-in didn't load")), { once: true });
    if (!existing) {
      script.src = SCRIPT;
      script.async = true;
      document.head.appendChild(script);
    }
  });
}

/**
 * "Continue with Google" via Google Identity Services (IDN-3). The ID token goes to the API, which checks it
 * against Google's keys. Hidden when no client ID is configured for this deployment. While the API call for a
 * credential runs, the button can't be used again and says what is happening.
 */
export function GoogleButton({ onCredential, text = "continue_with" }: { onCredential: (credential: string) => Promise<void> | void; text?: "signin_with" | "signup_with" | "continue_with" }) {
  const ref = useRef<HTMLDivElement>(null);
  const callback = useRef(onCredential);
  const busyRef = useRef(false);
  const [busy, setBusy] = useState(false);
  const [failed, setFailed] = useState(false);
  useEffect(() => {
    callback.current = onCredential;
  });

  useEffect(() => {
    if (!clientId || !ref.current) return;
    let cancelled = false;
    loadScript()
      .then(() => {
        if (cancelled || !ref.current || !window.google) return;
        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: async (response) => {
            if (busyRef.current) return;
            busyRef.current = true;
            setBusy(true);
            try {
              await callback.current(response.credential);
            } finally {
              busyRef.current = false;
              setBusy(false);
            }
          },
        });
        window.google.accounts.id.renderButton(ref.current, {
          theme: "outline",
          size: "large",
          shape: "rectangular",
          text,
          width: ref.current.offsetWidth || 360,
          logo_alignment: "center",
        });
      })
      .catch(() => setFailed(true));
    return () => {
      cancelled = true;
    };
  }, [text]);

  if (!clientId) return null;
  return (
    <div>
      <div ref={ref} aria-busy={busy} className={`flex min-h-11 w-full justify-center${busy ? " pointer-events-none opacity-60" : ""}`} />
      {busy && <p role="status" className="mt-1 text-center text-[12.5px] text-muted">Signing you in with Google…</p>}
      {failed && <p className="mt-1 text-center text-[12.5px] text-muted">Google sign-in couldn&apos;t load. Use your email and password instead.</p>}
    </div>
  );
}

export const googleEnabled = Boolean(clientId);
