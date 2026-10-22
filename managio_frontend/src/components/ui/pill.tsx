import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

export type Tone = "ok" | "due" | "bad" | "neutral" | "info";

const tones: Record<Tone, { text: string; dot: string }> = {
  ok: { text: "text-ok border-ok/30", dot: "bg-ok" },
  due: { text: "text-due border-due/35", dot: "bg-due" },
  bad: { text: "text-bad border-bad/30", dot: "bg-bad" },
  neutral: { text: "text-muted border-line", dot: "bg-faint" },
  info: { text: "text-cobalt border-cobalt/30", dot: "bg-cobalt" },
};

/** A status pill. Colour always means the same: green fine, amber needs attention soon, red needs action now. */
export function Pill({ tone = "neutral", children, className }: { tone?: Tone; children: ReactNode; className?: string }) {
  return (
    <span
      className={cn(
        "inline-flex h-6 items-center gap-1.5 whitespace-nowrap rounded-full border bg-surface px-2 text-[12px] font-medium",
        tones[tone].text,
        className,
      )}
    >
      <span className={cn("h-1.5 w-1.5 rounded-full", tones[tone].dot)} aria-hidden />
      {children}
    </span>
  );
}

/** A quiet tag for categories and kinds, without status meaning. */
export function Tag({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <span className={cn("inline-flex h-5 items-center rounded bg-canvas px-1.5 text-[11.5px] font-medium text-muted", className)}>
      {children}
    </span>
  );
}
