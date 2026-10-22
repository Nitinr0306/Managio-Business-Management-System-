"use client";

import type { Schemas } from "@/lib/api/client";
import { cn } from "@/lib/cn";
import { formatNumber, formatShortDate } from "@/lib/format";

/** Bar height as a share of the tallest bar, leaving headroom; empty days keep a sliver so the axis reads. */
function height(value: number, max: number) {
  return value === 0 ? 2 : Math.max(6, (value / (max * 1.1)) * 100);
}

/** New businesses and new accounts per week, side by side. Screen readers get the same numbers as a table. */
export function GrowthChart({ weeks }: { weeks: Schemas["SignupWeek"][] }) {
  const max = Math.max(1, ...weeks.flatMap((w) => [w.users, w.businesses]));
  return (
    <figure>
      <div aria-hidden className="flex h-[160px] items-end gap-2 border-b border-line">
        {weeks.map((w) => (
          <div key={w.weekStart} className="group relative flex h-full min-w-0 flex-1 items-end justify-center gap-[2px]">
            <div className="w-1/2 max-w-3 rounded-t-[3px] bg-cobalt" style={{ height: `${height(w.businesses, max)}%` }} />
            <div className="w-1/2 max-w-3 rounded-t-[3px] bg-line-strong" style={{ height: `${height(w.users, max)}%` }} />
            <div className="pointer-events-none absolute bottom-full left-1/2 z-10 mb-1 hidden -translate-x-1/2 whitespace-nowrap rounded-md bg-ink px-2 py-1 text-[12px] text-white group-hover:block">
              Week of {formatShortDate(w.weekStart)}: {w.businesses} businesses, {w.users} people
            </div>
          </div>
        ))}
      </div>
      <div aria-hidden className="mt-2 flex gap-2 text-[11px] text-faint">
        {weeks.map((w, i) => (
          <span key={w.weekStart} className={cn("min-w-0 flex-1 whitespace-nowrap", i % 3 !== 0 && "invisible")}>
            {formatShortDate(w.weekStart)}
          </span>
        ))}
      </div>
      <figcaption className="mt-3 flex flex-wrap gap-5 text-[12px] text-muted">
        <Legend className="bg-cobalt">New businesses</Legend>
        <Legend className="bg-line-strong">New accounts</Legend>
      </figcaption>
      <div className="sr-only">
        <table>
          <caption>New businesses and accounts per week</caption>
          <thead>
            <tr>
              <th scope="col">Week starting</th>
              <th scope="col">Businesses</th>
              <th scope="col">Accounts</th>
            </tr>
          </thead>
          <tbody>
            {weeks.map((w) => (
              <tr key={w.weekStart}>
                <td>{formatShortDate(w.weekStart)}</td>
                <td>{w.businesses}</td>
                <td>{w.users}</td>
              </tr>
            ))}
          </tbody>
      </table>
      </div>
    </figure>
  );
}

/** Businesses that recorded something each day; the tooltip adds what was recorded and who used the app. */
export function ActivityChart({ days }: { days: Schemas["ActivityDay"][] }) {
  const max = Math.max(1, ...days.map((d) => d.activeBusinesses));
  return (
    <figure>
      <div aria-hidden className="flex h-[140px] items-end gap-[3px] border-b border-line sm:gap-[4px]">
        {days.map((d, i) => (
          <div key={d.day} className="group relative flex h-full min-w-0 flex-1 items-end">
            <div
              className={cn("w-full rounded-t-[2px]", d.activeBusinesses === 0 ? "bg-line" : i === days.length - 1 ? "bg-cobalt" : "bg-cobalt/55")}
              style={{ height: `${height(d.activeBusinesses, max)}%` }}
            />
            <div
              className={cn(
                "pointer-events-none absolute bottom-full z-10 mb-1 hidden whitespace-nowrap rounded-md bg-ink px-2 py-1 text-[12px] text-white group-hover:block",
                i > days.length / 2 ? "right-0" : "left-0",
              )}
            >
              {formatShortDate(d.day)}: {d.activeBusinesses} businesses recorded {d.events} things; {d.activeUsers} people used the app
            </div>
          </div>
        ))}
      </div>
      <div aria-hidden className="mt-2 flex gap-[3px] text-[11px] text-faint sm:gap-[4px]">
        {days.map((d, i) => (
          <span key={d.day} className={cn("min-w-0 flex-1 whitespace-nowrap", i % 7 !== 0 && "invisible")}>
            {formatShortDate(d.day)}
          </span>
        ))}
      </div>
      <div className="sr-only">
        <table>
          <caption>Activity per day</caption>
          <thead>
            <tr>
              <th scope="col">Day</th>
              <th scope="col">Businesses that recorded something</th>
              <th scope="col">Things recorded</th>
              <th scope="col">People who used the app</th>
            </tr>
          </thead>
          <tbody>
            {days.map((d) => (
              <tr key={d.day}>
                <td>{formatShortDate(d.day)}</td>
                <td>{d.activeBusinesses}</td>
                <td>{d.events}</td>
                <td>{d.activeUsers}</td>
              </tr>
            ))}
          </tbody>
      </table>
      </div>
    </figure>
  );
}

/** Horizontal bars for parts of a whole, each with its count and share. */
export function ShareList({ items, total, unit }: { items: { label: string; count: number }[]; total: number; unit?: string }) {
  return (
    <ul className="space-y-3">
      {items.map((item) => {
        const share = total === 0 ? 0 : Math.round((item.count / total) * 100);
        return (
          <li key={item.label}>
            <div className="flex items-baseline justify-between gap-3 text-[13px]">
              <span className="min-w-0 truncate text-ink">{item.label}</span>
              <span className="shrink-0 tabular-nums text-muted">
                {formatNumber(item.count)}
                {unit && ` ${unit}`} <span className="text-faint">({share}%)</span>
              </span>
            </div>
            <div aria-hidden className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-canvas">
              <div className="h-full rounded-full bg-cobalt" style={{ width: `${share}%` }} />
            </div>
          </li>
        );
      })}
    </ul>
  );
}

function Legend({ className, children }: { className: string; children: string }) {
  return (
    <span className="inline-flex items-center gap-1.5">
      <span className={cn("h-2.5 w-2.5 rounded-sm", className)} aria-hidden />
      {children}
    </span>
  );
}
