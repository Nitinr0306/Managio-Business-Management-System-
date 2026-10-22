"use client";

import { useState } from "react";
import { Input } from "@/components/ui/field";
import { EmptyState, KpiStrip, PageHeader, Panel, Skeleton, Table, TD, TH, THead, TR } from "@/components/ui/layout";
import { Segmented } from "@/components/ui/menu";
import { methodLabel, type PaymentMethod } from "@/features/billing/api";
import type { Schemas } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import { formatDate, formatMoney, formatMonth, formatShortDate, localToday } from "@/lib/format";
import { useProfitAndLoss } from "./api";

type Range = "month" | "last-month" | "quarter" | "year" | "custom";

function rangeDates(range: Range, today: string): { from: string; to: string } {
  const [y, m] = today.split("-").map(Number);
  const iso = (year: number, month: number, day: number) => new Date(Date.UTC(year, month - 1, day)).toISOString().slice(0, 10);
  switch (range) {
    case "month":
      return { from: iso(y, m, 1), to: today };
    case "last-month":
      return { from: iso(y, m - 1, 1), to: iso(y, m, 0) };
    case "quarter":
      return { from: iso(y, m - 2, 1), to: today };
    case "year":
      return { from: iso(y, m - 11, 1), to: today };
    default:
      return { from: iso(y, m, 1), to: today };
  }
}

/** Profit and loss on a cash basis (REP-1): money received minus money spent, by the day it moved. */
export function ReportsPage() {
  const today = localToday();
  const [range, setRange] = useState<Range>("month");
  const [custom, setCustom] = useState(() => rangeDates("month", today));
  const { from, to } = range === "custom" ? custom : rangeDates(range, today);
  const report = useProfitAndLoss(from, to);
  const data = report.data;

  return (
    <>
      <PageHeader title="Reports" subtitle="Profit and loss from money that actually came in and went out. Voided records don't count." />
      <div className="mb-5 flex flex-wrap items-end gap-3">
        <Segmented
          label="Period"
          value={range}
          onChange={setRange}
          options={[
            { value: "month", label: "This month" },
            { value: "last-month", label: "Last month" },
            { value: "quarter", label: "Last 3 months" },
            { value: "year", label: "Last 12 months" },
            { value: "custom", label: "Custom" },
          ]}
        />
        {range === "custom" && (
          <div className="flex items-end gap-2">
            <Input type="date" aria-label="From" value={custom.from} max={custom.to} onChange={(e) => setCustom((c) => ({ ...c, from: e.target.value }))} className="w-[160px]" />
            <Input type="date" aria-label="To" value={custom.to} min={custom.from} max={today} onChange={(e) => setCustom((c) => ({ ...c, to: e.target.value }))} className="w-[160px]" />
          </div>
        )}
      </div>

      {report.isPending ? (
        <Skeleton className="h-[98px] w-full" />
      ) : report.isError ? (
        <EmptyState title="The report couldn't load" body={errorMessage(report.error)} />
      ) : data ? (
        <>
          <KpiStrip
            items={[
              { label: "Money in", value: formatMoney(data.revenue), note: `${formatDate(data.from)} to ${formatDate(data.to)}` },
              { label: "Money out", value: formatMoney(data.expenses) },
              { label: "Profit", value: formatMoney(data.profit), tone: data.profit >= 0 ? "ok" : "bad", note: data.profit >= 0 ? "More came in than went out" : "More went out than came in" },
            ]}
          />
          <Panel className="mt-5" title={data.granularity === "DAY" ? "Day by day" : "Month by month"} bodyClassName="px-5 pb-5 pt-4">
            {data.series.length === 0 ? <p className="py-8 text-center text-[13px] text-muted">No money moved in this period.</p> : <SeriesChart data={data} />}
          </Panel>
          <div className="mt-5 grid gap-5 lg:grid-cols-2">
            <Panel title="Money in, by method">
              <Breakdown rows={Object.entries(data.revenueByMethod).map(([method, amount]) => ({ label: methodLabel(method as PaymentMethod), amount }))} total={data.revenue} />
            </Panel>
            <Panel title="Money out, by category">
              <Breakdown rows={data.expensesByCategory.map((c) => ({ label: c.category, amount: c.amount }))} total={data.expenses} />
            </Panel>
          </div>
        </>
      ) : null}
    </>
  );
}

function Breakdown({ rows, total }: { rows: { label: string; amount: number }[]; total: number }) {
  if (rows.length === 0) return <p className="px-5 py-6 text-[13px] text-muted">Nothing in this period.</p>;
  return (
    <Table>
      <THead>
        <tr>
          <TH>Where</TH>
          <TH className="w-1/3"><span className="sr-only">Share</span></TH>
          <TH className="text-right">Amount</TH>
        </tr>
      </THead>
      <tbody>
        {rows.map((row) => (
          <TR key={row.label}>
            <TD>{row.label}</TD>
            <TD>
              <div className="h-1.5 rounded-full bg-line" aria-hidden>
                <div className="h-1.5 rounded-full bg-cobalt" style={{ width: `${total > 0 ? Math.max(2, (Math.abs(row.amount) / total) * 100) : 0}%` }} />
              </div>
            </TD>
            <TD className="text-right tabular-nums">{formatMoney(row.amount)}</TD>
          </TR>
        ))}
      </tbody>
    </Table>
  );
}

/** Paired bars per period (in and out), drawn to one scale, with the profit written under each pair. */
function SeriesChart({ data }: { data: Schemas["ProfitAndLoss"] }) {
  const points = data.series;
  const max = Math.max(1, ...points.flatMap((p) => [p.revenue, p.expenses]));
  const height = 180;
  const label = (bucket: string) => (data.granularity === "DAY" ? formatShortDate(bucket) : formatMonth(bucket));
  const every = Math.max(1, Math.ceil(points.length / 12));
  const ticks = [0, 0.5, 1].map((f) => Math.round(max * f));

  return (
    <figure>
      <div className="flex gap-3">
        <div className="relative w-14 shrink-0 text-right text-[11px] tabular-nums text-faint" style={{ height }} aria-hidden>
          {ticks.map((t) => (
            <span key={t} className="absolute right-0 -translate-y-1/2" style={{ top: height - (t / max) * height }}>
              {formatMoney(t)}
            </span>
          ))}
        </div>
        <div className="min-w-0 flex-1 overflow-x-auto">
          <div
            role="img"
            aria-label={`Money in and out per ${data.granularity === "DAY" ? "day" : "month"} from ${formatDate(data.from)} to ${formatDate(data.to)}`}
            className="flex items-end gap-2 border-b border-line"
            style={{ height, minWidth: points.length * 22 }}
          >
            {points.map((p) => (
              <div key={p.bucket} className="group relative flex h-full flex-1 items-end justify-center gap-[2px]">
                <span className="w-1/2 max-w-4 rounded-t-[2px] bg-cobalt" style={{ height: `${(p.revenue / max) * 100}%` }} />
                <span className="w-1/2 max-w-4 rounded-t-[2px] bg-line-strong" style={{ height: `${(p.expenses / max) * 100}%` }} />
                <span className="pointer-events-none absolute bottom-full z-10 mb-1 hidden whitespace-nowrap rounded-md bg-ink px-2 py-1 text-[12px] text-white group-hover:block">
                  {label(p.bucket)}: in {formatMoney(p.revenue)}, out {formatMoney(p.expenses)}, profit {formatMoney(p.profit)}
                </span>
              </div>
            ))}
          </div>
          <div className="mt-2 flex gap-2 text-[11px] text-faint" style={{ minWidth: points.length * 22 }} aria-hidden>
            {points.map((p, i) => (
              <span key={p.bucket} className={`flex-1 truncate text-center ${i % every === 0 ? "" : "invisible"}`}>
                {label(p.bucket)}
              </span>
            ))}
          </div>
        </div>
      </div>
      <figcaption className="mt-3 flex gap-5 text-[12px] text-muted">
        <span className="inline-flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-sm bg-cobalt" aria-hidden /> Money in</span>
        <span className="inline-flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-sm bg-line-strong" aria-hidden /> Money out</span>
      </figcaption>
    </figure>
  );
}
