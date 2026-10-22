"use client";

import { ErrorView } from "@/components/feedback/error-view";

export default function Error({ error, retry }: { error: unknown; retry: () => void }) {
  return <ErrorView error={error} retry={retry} />;
}
