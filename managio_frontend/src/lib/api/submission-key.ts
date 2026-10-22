import { useRef } from "react";

/**
 * An Idempotency-Key for a money action (docs/API.md, "Retries"). The same key is sent until the action
 * succeeds, so a retry after a lost response or a second click replays the first result on the server
 * instead of recording the payment twice. The next submission gets a fresh key.
 */
export function useSubmissionKey() {
  const key = useRef<string | null>(null);
  return {
    header: () => {
      key.current ??= newKey();
      return { "Idempotency-Key": key.current };
    },
    renew: () => {
      key.current = null;
    },
  };
}

/** 32 random hex characters; getRandomValues works on plain-http pages too, unlike randomUUID. */
function newKey() {
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  return Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("");
}
