"use client";

import { ArrowRight, BookOpen, CircleCheck, Inbox, Plus } from "lucide-react";
import Link from "next/link";
import { useState, type ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { EmptyState, KpiStrip, PageHeader, Panel, Skeleton, Table, TD, TH, THead, TR } from "@/components/ui/layout";
import { Pill } from "@/components/ui/pill";
import { useBusiness } from "@/features/business/context";
import { methodLabel, usePayments } from "@/features/billing/api";
import { CustomerDialog } from "@/features/customers/customer-dialog";
import { useCustomerPanel } from "@/features/customers/panel-context";
import { MEMBERSHIP_ITEM_STATE, useMemberships } from "@/features/memberships/api";
import { PRIORITY, useTasks, type Task } from "@/features/operations/api";
import { useRunway, useToday } from "@/features/reports/api";
import { RunwayChart } from "@/features/reports/runway-chart";
import { useStaffSession } from "@/lib/auth/use-session";
import { errorMessage } from "@/lib/api/errors";
import { formatLongDate, formatMoney, formatNumber, formatShortDate, formatTime, plural } from "@/lib/format";

function greeting(hour: number) {
  if (hour < 12) return "Good morning";
  if (hour < 17) return "Good afternoon";
  return "Good evening";
}

/** The home screen: what needs doing today, shaped by what the person's role may see (REP-3). */
export function TodayPage() {
  const business = useBusiness();
  const session = useStaffSession();
  const panel = useCustomerPanel();
  const today = useToday();
  const [adding, setAdding] = useState(false);

  const showRenewals = business.can("customers.view") && business.has("MEMBERSHIPS");
  const runway = useRunway(30, showRenewals);
  const ending = useMemberships({ view: "ENDING_SOON", needsRenewal: true, size: 8 }, showRenewals);
  const lapsed = useMemberships({ view: "EXPIRED", needsRenewal: true, size: 6 }, showRenewals);
  const date = today.data?.date;
  const collections = usePayments({ from: date, to: date, size: 8 }, Boolean(date) && business.can("billing.view"));
  const tasks = useTasks({ mine: true, view: "OPEN", size: 6 });

  const firstName = session.principal?.fullName.split(" ")[0] ?? "";
  const t = today.data;

  if (today.isError) {
    return <EmptyState title="Today couldn't load" body={errorMessage(today.error)} action={<Button onClick={() => today.refetch()}>Try again</Button>} />;
  }

  const kpis: Parameters<typeof KpiStrip>[0]["items"] = [];
  if (t?.takings) {
    kpis.push({ label: "Collected today", value: formatMoney(t.takings.received), note: `${formatMoney(t.takings.spent)} spent`, tone: "muted" });
  }
  if (t?.dues) {
    kpis.push({ label: "Waiting to be paid", value: formatMoney(t.dues.openAmount), note: plural(t.dues.openInvoices, "open invoice"), tone: "muted" });
    kpis.push({
      label: "Overdue",
      value: formatMoney(t.dues.overdueAmount),
      note: t.dues.overdueInvoices ? plural(t.dues.overdueInvoices, "invoice") + " past due" : "Nothing late",
      tone: t.dues.overdueInvoices ? "bad" : "ok",
    });
  }
  if (t?.renewals) {
    kpis.push({
      label: "Ending in 7 days",
      value: formatNumber(t.renewals.endingThisWeek),
      note: `${formatNumber(t.renewals.endingToday)} today`,
      tone: t.renewals.endingThisWeek ? "due" : "muted",
    });
    kpis.push({
      label: "Lapsed, not renewed",
      value: formatNumber(t.renewals.lapsedLast30Days),
      note: t.renewals.lapsedLast30Days ? "Last 30 days, worth a call" : "None in the last 30 days",
      tone: t.renewals.lapsedLast30Days ? "bad" : "muted",
    });
  }
  if (t?.newCustomersThisMonth != null && kpis.length < 5) {
    kpis.push({ label: `New ${business.labels.customers.toLowerCase()} this month`, value: formatNumber(t.newCustomersThisMonth) });
  }

  const actionRows = [...(ending.data?.items ?? []), ...(lapsed.data?.items ?? [])];
  const runwayDays = runway.data?.days ?? [];
  const thisWeek = runwayDays.slice(0, 7).reduce((sum, d) => sum + d.ending, 0);
  const thirty = runwayDays.reduce((sum, d) => sum + d.ending, 0);
  const totalCollected = (collections.data?.items ?? []).filter((p) => !p.voided).reduce((sum, p) => sum + (p.kind === "REFUND" ? -p.amount : p.amount), 0);

  return (
    <>
      <PageHeader
        title={`${greeting(new Date().getHours())}${firstName ? `, ${firstName}` : ""}`}
        subtitle={t ? `${formatLongDate(t.date)} · ${business.name}` : business.name}
        actions={
          business.can("customers.manage") ? (
            <Button variant="primary" icon={<Plus className="h-4 w-4" />} onClick={() => setAdding(true)}>
              Add {business.labels.customer.toLowerCase()}
            </Button>
          ) : undefined
        }
      />

      {today.isPending ? (
        <Skeleton className="h-[98px] w-full" />
      ) : (
        kpis.length > 0 && <KpiStrip items={kpis} />
      )}

      <Attention business={business} today={t} />

      {showRenewals && (
        <Panel className="mt-5" bodyClassName="px-5 pb-5 pt-4" aria-labelledby="runway-title">
          <div className="flex flex-wrap items-baseline justify-between gap-2">
            <div>
              <h2 id="runway-title" className="text-[15px] font-semibold">Renewal runway</h2>
              <p className="text-[13px] text-muted">{business.labels.plans} ending each day that nobody has renewed yet, next 30 days</p>
            </div>
            <p className="text-[13px] text-muted">
              <span className="font-semibold text-ink">{formatNumber(thisWeek)}</span> this week,{" "}
              <span className="font-semibold text-ink">{formatNumber(thirty)}</span> in 30 days
            </p>
          </div>
          {runway.isPending ? <Skeleton className="mt-8 h-[170px] w-full" /> : <RunwayChart days={runwayDays} />}
        </Panel>
      )}

      <div className="mt-5 grid gap-5 xl:grid-cols-[1.45fr_1fr]">
        {showRenewals ? (
          <Panel
            title="Needs a renewal"
            actions={<Link href={`/app/${business.id}/renewals`} className="text-[13px] font-medium text-cobalt hover:underline">All renewals</Link>}
          >
            {ending.isPending ? (
              <div className="space-y-2 p-5">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-8 w-full" />)}</div>
            ) : actionRows.length === 0 ? (
              <EmptyState icon={<CircleCheck className="h-5 w-5" />} title="Everyone is covered" body={`No ${business.labels.plans.toLowerCase()} end in the next 7 days without a renewal.`} />
            ) : (
              <Table>
                <THead>
                  <tr>
                    <TH>{business.labels.customer}</TH>
                    <TH>{business.labels.plan}</TH>
                    <TH>Ends</TH>
                    <TH>Status</TH>
                    <TH className="text-right"><span className="sr-only">Action</span></TH>
                  </tr>
                </THead>
                <tbody>
                  {actionRows.map((m) => (
                    <TR key={m.id} onClick={() => panel.open(m.customerId)}>
                      <TD className="font-medium">{m.customerName}</TD>
                      <TD className="text-muted">{m.planName}</TD>
                      <TD className="tabular-nums text-muted">{formatShortDate(m.endsOn)}</TD>
                      <TD><Pill tone={MEMBERSHIP_ITEM_STATE[m.state].tone}>{MEMBERSHIP_ITEM_STATE[m.state].label}</Pill></TD>
                      <TD className="text-right">
                        <button type="button" className="text-[13px] font-semibold text-cobalt hover:underline">Open</button>
                      </TD>
                    </TR>
                  ))}
                </tbody>
              </Table>
            )}
          </Panel>
        ) : (
          <MyTasks tasks={tasks.data?.items ?? []} loading={tasks.isPending} />
        )}

        <div className="space-y-5">
          {business.can("billing.view") && (
            <Panel title="Today's collections" actions={<span className="text-[15px] font-semibold tabular-nums">{formatMoney(totalCollected)}</span>}>
              {(collections.data?.items ?? []).length === 0 ? (
                <p className="px-5 py-6 text-[13px] text-muted">No payments yet today.</p>
              ) : (
                <ul className="divide-y divide-line text-[13px]">
                  {(collections.data?.items ?? []).map((p) => (
                    <li key={p.id} className="flex h-11 items-center gap-4 px-5">
                      <span className="w-14 tabular-nums text-faint">{formatTime(p.receivedAt)}</span>
                      <span className="min-w-0 flex-1 truncate font-medium">{p.customer?.name ?? "Walk-in"}</span>
                      <span className="w-20 truncate text-muted">{methodLabel(p.method)}</span>
                      <span className={p.kind === "REFUND" || p.voided ? "w-20 text-right tabular-nums text-faint line-through" : "w-20 text-right font-medium tabular-nums"}>
                        {p.kind === "REFUND" ? "−" : ""}{formatMoney(p.amount)}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </Panel>
          )}
          {showRenewals && <MyTasks tasks={tasks.data?.items ?? []} loading={tasks.isPending} />}
        </div>
      </div>

      <CustomerDialog open={adding} onOpenChange={setAdding} onSaved={(c) => panel.open(c.id)} />
    </>
  );
}

function Attention({ business, today }: { business: ReturnType<typeof useBusiness>; today: ReturnType<typeof useToday>["data"] }) {
  if (!today) return null;
  const items: { icon: ReactNode; text: ReactNode; href: string; cta: string }[] = [];
  if (today.pendingApprovals) {
    items.push({
      icon: <Inbox className="h-4 w-4" />,
      text: <>{plural(today.pendingApprovals, "request")} waiting for your decision</>,
      href: `/app/${business.id}/requests?status=PENDING`,
      cta: "Review",
    });
  }
  if (today.dayClosed === false) {
    items.push({
      icon: <BookOpen className="h-4 w-4" />,
      text: <>Today&apos;s cash hasn&apos;t been counted yet</>,
      href: `/app/${business.id}/day-book`,
      cta: "Close the day",
    });
  }
  if (items.length === 0) return null;
  return (
    <ul className="mt-5 grid gap-3 md:grid-cols-2">
      {items.map((item) => (
        <li key={item.href}>
          <Link href={item.href} className="flex items-center gap-3 rounded-[var(--radius-panel)] border border-line bg-surface px-4 py-3 hover:border-line-strong">
            <span className="grid h-8 w-8 place-items-center rounded-full bg-cobalt-soft text-cobalt">{item.icon}</span>
            <span className="flex-1 text-[13.5px] text-ink">{item.text}</span>
            <span className="inline-flex items-center gap-1 text-[13px] font-semibold text-cobalt">
              {item.cta} <ArrowRight className="h-3.5 w-3.5" aria-hidden />
            </span>
          </Link>
        </li>
      ))}
    </ul>
  );
}

function MyTasks({ tasks, loading }: { tasks: Task[]; loading: boolean }) {
  const business = useBusiness();
  return (
    <Panel title="Your tasks" actions={<Link href={`/app/${business.id}/tasks`} className="text-[13px] font-medium text-cobalt hover:underline">All tasks</Link>}>
      {loading ? (
        <div className="space-y-2 p-5">{[0, 1].map((i) => <Skeleton key={i} className="h-8 w-full" />)}</div>
      ) : tasks.length === 0 ? (
        <p className="px-5 py-6 text-[13px] text-muted">Nothing assigned to you. Enjoy the quiet.</p>
      ) : (
        <ul className="divide-y divide-line">
          {tasks.map((task) => (
            <li key={task.id} className="flex items-center gap-3 px-5 py-2.5 text-[13.5px]">
              <span className="min-w-0 flex-1">
                <span className="block truncate font-medium text-ink">{task.title}</span>
                <span className={task.overdue ? "text-[12px] font-medium text-bad" : "text-[12px] text-muted"}>
                  {task.dueOn ? (task.overdue ? `Was due ${formatShortDate(task.dueOn)}` : `Due ${formatShortDate(task.dueOn)}`) : "No due date"}
                </span>
              </span>
              {task.priority !== "NORMAL" && task.priority !== "LOW" && <Pill tone={PRIORITY[task.priority].tone}>{PRIORITY[task.priority].label}</Pill>}
            </li>
          ))}
        </ul>
      )}
    </Panel>
  );
}
