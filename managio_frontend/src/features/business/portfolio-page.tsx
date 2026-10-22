"use client";

import { ArrowUpRight, Plus } from "lucide-react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect } from "react";
import { FullPageLoading } from "@/components/shell/require-staff";
import { Button } from "@/components/ui/button";
import { EmptyState, KpiStrip, Panel, Table, TD, TH, THead, TR } from "@/components/ui/layout";
import { Pill } from "@/components/ui/pill";
import { usePortfolio } from "@/features/reports/api";
import { useStaffSession } from "@/lib/auth/use-session";
import { errorMessage } from "@/lib/api/errors";
import { formatMoney, formatMonth, formatNumber, initials } from "@/lib/format";
import { BUSINESS_TYPE_LABELS, type BusinessType } from "./access";
import { useMyBusinesses } from "./context";
import { Logo } from "@/components/brand/logo";

/**
 * /app: someone with one business goes straight to it; someone with several sees the portfolio (D-001, from
 * Variant C, REP-2); someone with none starts by creating one.
 */
export function PortfolioPage() {
  const router = useRouter();
  const params = useSearchParams();
  const businesses = useMyBusinesses();
  const forcePortfolio = params.get("view") === "all";
  const count = businesses.data?.length;
  const onlyBusinessId = count === 1 ? businesses.data?.[0]?.id : undefined;

  useEffect(() => {
    if (count === 0) router.replace("/app/new");
    if (onlyBusinessId != null && !forcePortfolio) router.replace(`/app/${onlyBusinessId}`);
  }, [count, forcePortfolio, onlyBusinessId, router]);

  if (businesses.isPending || count === 0 || (count === 1 && !forcePortfolio)) {
    return <FullPageLoading />;
  }
  if (businesses.isError) {
    return <EmptyState title="Your businesses couldn't load" body={errorMessage(businesses.error)} />;
  }
  return <Portfolio />;
}

function Portfolio() {
  const session = useStaffSession();
  const portfolio = usePortfolio();
  const data = portfolio.data;
  const anyMoney = data?.businesses.some((b) => b.revenueThisMonth != null);

  return (
    <div className="min-h-screen bg-canvas">
      <header className="border-b border-line bg-surface">
        <div className="mx-auto flex h-14 max-w-[1180px] items-center justify-between px-4 sm:px-8">
          <Logo size="sm" />
          <span className="text-[13px] text-muted">{session.principal?.fullName}</span>
        </div>
      </header>
      <main className="mx-auto max-w-[1180px] px-4 py-8 sm:px-8">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-[24px] font-semibold leading-8 text-ink">Your businesses</h1>
            <p className="mt-0.5 text-[13px] text-muted">{data ? `This month so far, from ${formatMonth(data.monthStart)}` : "Loading this month's figures"}</p>
          </div>
          <Button asChild variant="primary" icon={<Plus className="h-4 w-4" />}>
            <Link href="/app/new">Add a business</Link>
          </Button>
        </div>

        {data && anyMoney && (
          <KpiStrip
            items={[
              { label: "Revenue this month", value: formatMoney(data.totals.revenueThisMonth), note: `Across ${data.totals.businessesCounted} businesses` },
              { label: "Expenses this month", value: formatMoney(data.totals.expensesThisMonth) },
              {
                label: "Profit this month",
                value: formatMoney(data.totals.profitThisMonth),
                tone: data.totals.profitThisMonth >= 0 ? "ok" : "bad",
                note: data.totals.profitThisMonth >= 0 ? "Ahead so far" : "Behind so far",
              },
            ]}
          />
        )}

        <Panel className="mt-5" title="Where attention is needed">
          {portfolio.isError ? (
            <p className="px-5 py-6 text-[13.5px] text-bad">{errorMessage(portfolio.error)}</p>
          ) : (
            <Table>
              <THead>
                <tr>
                  <TH>Business</TH>
                  <TH className="text-right">Revenue</TH>
                  <TH className="text-right">Profit</TH>
                  <TH className="text-right">Ending this week</TH>
                  <TH className="text-right">Overdue</TH>
                  <TH className="text-right">Waiting on you</TH>
                  <TH><span className="sr-only">Open</span></TH>
                </tr>
              </THead>
              <tbody>
                {(data?.businesses ?? []).map((b) => (
                  <TR key={b.id}>
                    <TD>
                      <Link href={`/app/${b.id}`} className="flex items-center gap-3">
                        <span className="grid h-8 w-8 shrink-0 place-items-center rounded-md bg-ink text-[12px] font-bold text-white">{initials(b.name)}</span>
                        <span className="min-w-0">
                          <span className="block truncate font-semibold text-ink hover:underline">{b.name}</span>
                          <span className="text-[12px] text-muted">{BUSINESS_TYPE_LABELS[b.type as BusinessType] ?? b.type}{b.owner ? " · Owner" : ""}</span>
                        </span>
                      </Link>
                    </TD>
                    <TD className="text-right tabular-nums">{b.revenueThisMonth == null ? <Muted /> : formatMoney(b.revenueThisMonth)}</TD>
                    <TD className="text-right tabular-nums">
                      {b.profitThisMonth == null ? <Muted /> : <span className={b.profitThisMonth < 0 ? "text-bad" : "text-ink"}>{formatMoney(b.profitThisMonth)}</span>}
                    </TD>
                    <TD className="text-right tabular-nums">
                      {b.endingThisWeek == null ? <Muted /> : b.endingThisWeek > 0 ? <Pill tone="due">{formatNumber(b.endingThisWeek)}</Pill> : "0"}
                    </TD>
                    <TD className="text-right tabular-nums">
                      {b.overdueAmount == null ? <Muted /> : b.overdueAmount > 0 ? <span className="font-medium text-bad">{formatMoney(b.overdueAmount)}</span> : "—"}
                    </TD>
                    <TD className="text-right tabular-nums">
                      {(b.pendingApprovals ?? 0) + b.myOpenTasks > 0
                        ? [b.pendingApprovals ? `${b.pendingApprovals} requests` : null, b.myOpenTasks ? `${b.myOpenTasks} tasks` : null].filter(Boolean).join(", ")
                        : "—"}
                    </TD>
                    <TD className="text-right">
                      <Link href={`/app/${b.id}`} className="inline-flex items-center gap-1 text-[13px] font-semibold text-cobalt hover:underline">
                        Open <ArrowUpRight className="h-3.5 w-3.5" aria-hidden />
                      </Link>
                    </TD>
                  </TR>
                ))}
              </tbody>
            </Table>
          )}
        </Panel>
        <p className="mt-3 text-[12.5px] text-faint">Figures your role in a business doesn&apos;t allow are left out and not counted in the totals.</p>
      </main>
    </div>
  );
}

function Muted() {
  return <span className="text-faint" title="Your role here doesn't include this">·</span>;
}
