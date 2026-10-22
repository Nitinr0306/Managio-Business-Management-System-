"use client";

import { ErrorView } from "@/components/feedback/error-view";

/** Keeps the sidebar and top bar when one page fails, so people can go somewhere else. */
export default function Error({ error, retry }: { error: unknown; retry: () => void }) {
  return <ErrorView error={error} retry={retry} compact />;
}
