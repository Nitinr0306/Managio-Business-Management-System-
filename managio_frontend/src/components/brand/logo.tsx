import { cn } from "@/lib/cn";

/** The "m" of the mark, drawn on a 32-unit square. The same geometry as src/app/icon.svg and the generated icons. */
const M_PATH = "M9 22v-8.5a3.5 3.5 0 0 1 7 0V22m0-8.5a3.5 3.5 0 0 1 7 0V22";

/**
 * Managio's mark: a white "m" on a cobalt square, or the reverse on dark backgrounds. Decorative wherever the
 * wordmark or a label next to it already names Managio.
 */
export function LogoMark({ tone = "brand", className }: { tone?: "brand" | "on-dark"; className?: string }) {
  const square = tone === "brand" ? "var(--color-cobalt)" : "#ffffff";
  const letter = tone === "brand" ? "#ffffff" : "var(--color-cobalt)";
  return (
    <svg viewBox="0 0 32 32" aria-hidden focusable="false" className={cn("h-7 w-7 shrink-0", className)}>
      <rect width="32" height="32" rx="8" fill={square} />
      <path d={M_PATH} fill="none" stroke={letter} strokeWidth="3.2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

/** Mark and wordmark together. The wordmark is real text, so it is the accessible name of a link around it. */
export function Logo({ tone = "brand", size = "md", className }: { tone?: "brand" | "on-dark"; size?: "sm" | "md" | "lg"; className?: string }) {
  const sizes = {
    sm: { mark: "h-6 w-6", text: "text-[16px]" },
    md: { mark: "h-7 w-7", text: "text-[18px]" },
    lg: { mark: "h-8 w-8", text: "text-[21px]" },
  }[size];
  return (
    <span className={cn("inline-flex items-center gap-2", className)}>
      <LogoMark tone={tone} className={sizes.mark} />
      <span className={cn("font-bold leading-none tracking-[-0.02em]", sizes.text, tone === "on-dark" ? "text-white" : "text-ink")}>managio</span>
    </span>
  );
}
