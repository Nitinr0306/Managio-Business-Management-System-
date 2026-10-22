"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import { errorMessage } from "@/lib/api/errors";

/** What a crashed screen shows: the error in plain words and a way forward. */
export function ErrorView({ error, retry, compact = false }: { error: unknown; retry: () => void; compact?: boolean }) {
  return (
    <div className={compact ? "py-16" : "grid min-h-screen place-items-center bg-canvas px-4"}>
      <div className="mx-auto max-w-[440px] text-center">
        <p className="text-[18px] font-semibold text-ink">This screen hit a problem</p>
        <p className="mt-2 text-[14px] text-muted">{errorMessage(error)}</p>
        <div className="mt-5 flex justify-center gap-2">
          <Button variant="primary" onClick={retry}>Try again</Button>
          <Button asChild>
            <Link href="/app">Go to your businesses</Link>
          </Button>
        </div>
      </div>
    </div>
  );
}
