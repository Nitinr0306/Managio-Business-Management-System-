"use client";

import type { Schemas } from "@/lib/api/client";
import { cn } from "@/lib/cn";
import { formatMoney, formatShortDate } from "@/lib/format";

/**
 * The renewal runway (Variant A's signature): one bar per day for the memberships that end that day and
 * have not been renewed. The first seven days are amber, because that is the week to act in.
 */
export function RunwayChart({ days }: { days: Schemas["RunwayDay"][] }) {
  const max = Math.max(1, ...days.map((d) => d.ending));
  const total = days.reduce((sum, d) => sum + d.ending, 0);
  const label = `Memberships ending each day over the next ${days.length} days: ${total} in total`;
  return (
    <figure className="mt-8">
      <div className="relative">
        {total === 0 && (
          <p className="absolute inset-x-0 top-1/3 text-center text-[13px] text-muted">
            Nothing ends in the next {days.length} days without a renewal.
          </p>
        )}
        <div role="img" aria-label={label} className="flex h-[148px] items-end gap-[3px] border-b border-line sm:gap-[5px]">
          {days.map((d, i) => {
            const height = d.ending === 0 ? 3 : Math.max(8, (d.ending / (max * 1.15)) * 100);
            return (
              <div key={d.date} className="group relative flex h-full flex-1 items-end">
                {i === 0 && (
                  <>
                    <span className="absolute -top-5 left-0 text-[11px] font-semibold text-ink">Today</span>
                    <span className="absolute inset-y-0 left-0 w-px bg-ink/40" aria-hidden />
                  </>
                )}
                <div
                  className={cn(
                    "w-full rounded-t-[3px] transition-colors",
                    d.ending === 0 ? "bg-line" : i < 7 ? "bg-due-fill" : "bg-line-strong group-hover:bg-[#aeb6c3]",
                  )}
                  style={{ height: `${height}%` }}
                />
                <div
                  className="pointer-events-none absolute left-1/2 z-10 hidden -translate-x-1/2 whitespace-nowrap rounded-md bg-ink px-2 py-1 text-[12px] text-white group-hover:block"
                  style={{ bottom: `calc(${height}% + 8px)` }}
                >
                  {formatShortDate(d.date)}: {d.ending === 0 ? "none" : `${d.ending} ending · ${formatMoney(d.value)}`}
                </div>
              </div>
            );
          })}
        </div>
        <div className="mt-2 flex gap-[3px] text-[11px] text-faint sm:gap-[5px]" aria-hidden>
          {days.map((d, i) => (
            <span key={d.date} className={cn("min-w-0 flex-1 overflow-visible whitespace-nowrap", i % 7 !== 0 && "invisible")}>
              {formatShortDate(d.date)}
            </span>
          ))}
        </div>
      </div>
      <figcaption className="mt-4 flex items-center gap-5 text-[12px] text-muted">
        <span className="inline-flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-sm bg-due-fill" aria-hidden />
          This week
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-sm bg-line-strong" aria-hidden />
          Later
        </span>
      </figcaption>
    </figure>
  );
}
