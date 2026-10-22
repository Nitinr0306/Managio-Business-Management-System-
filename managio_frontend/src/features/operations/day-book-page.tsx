"use client";

import { BookOpen, Lock } from "lucide-react";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Field, FormError, Input, Textarea } from "@/components/ui/field";
import { EmptyState, PageHeader, Panel, Skeleton, Table, TD, TH, THead, TR } from "@/components/ui/layout";
import { Pill } from "@/components/ui/pill";
import { methodLabel, type PaymentMethod } from "@/features/billing/api";
import type { Schemas } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import { addDays, formatDate, formatDateTime, formatLongDate, formatMoney, localToday } from "@/lib/format";
import { useCloseDay, useDayBook, useDayBookHistory } from "./api";

/**
 * The day book (DAY-1 to DAY-3): what the cash drawer should hold, the count, and the close. Closing a day
 * locks its cash: cash payments, refunds and expenses on that date can no longer be added or voided.
 */
export function DayBookPage() {
  const [date, setDate] = useState(localToday());
  const day = useDayBook(date);
  const history = useDayBookHistory(30);
  const closedDays = (history.data ?? []).flatMap((day) => (day.closed ? [{ day, closed: day.closed }] : []));

  return (
    <>
      <PageHeader
        title="Day book"
        subtitle="Count the cash at the end of the day and close it"
        actions={
          <label className="flex items-center gap-2 text-[13px] text-muted">
            Day
            <Input type="date" max={localToday()} value={date} onChange={(e) => setDate(e.target.value || localToday())} className="w-[170px]" />
          </label>
        }
      />
      <div className="grid gap-5 xl:grid-cols-[1.2fr_1fr]">
        {day.isPending ? (
          <Skeleton className="h-[360px] w-full" />
        ) : day.isError ? (
          <EmptyState title="The day couldn't load" body={errorMessage(day.error)} />
        ) : (
          <DayCard day={day.data} />
        )}
        <Panel title="Recent closes">
          {history.isPending ? (
            <div className="space-y-2 p-5">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-8 w-full" />)}</div>
          ) : closedDays.length === 0 ? (
            <EmptyState icon={<BookOpen className="h-5 w-5" />} title="No days closed yet" body="Close today when the counter shuts." />
          ) : (
            <Table>
              <THead>
                <tr>
                  <TH>Day</TH>
                  <TH className="text-right">Expected</TH>
                  <TH className="text-right">Counted</TH>
                  <TH className="text-right">Difference</TH>
                </tr>
              </THead>
              <tbody>
                {closedDays.map(({ day: d, closed }) => (
                  <TR key={d.date} onClick={() => setDate(d.date)}>
                    <TD className="tabular-nums">{formatDate(d.date)}</TD>
                    <TD className="text-right tabular-nums">{formatMoney(d.expectedCash)}</TD>
                    <TD className="text-right tabular-nums">{formatMoney(closed.countedCash)}</TD>
                    <TD className="text-right tabular-nums">
                      <Difference value={closed.difference} />
                    </TD>
                  </TR>
                ))}
              </tbody>
            </Table>
          )}
        </Panel>
      </div>
    </>
  );
}

function Difference({ value }: { value: number }) {
  if (value === 0) return <span className="text-ok">Exact</span>;
  return <span className={value < 0 ? "font-medium text-bad" : "font-medium text-due"}>{value < 0 ? "Short " : "Over "}{formatMoney(Math.abs(value))}</span>;
}

function DayCard({ day }: { day: Schemas["DayBook"] }) {
  const methods = Object.entries(day.byMethod).filter(([method]) => method !== "CASH");
  return (
    <Panel
      title={formatLongDate(day.date)}
      actions={day.closed ? <Pill tone="ok">Closed</Pill> : day.date < localToday() ? <Pill tone="due">Not closed</Pill> : <Pill tone="info">Open</Pill>}
    >
      <dl className="grid grid-cols-2 gap-px bg-line sm:grid-cols-4">
        {[
          { label: "Opening cash", value: day.openingCash, note: "From the last count" },
          { label: "Cash in", value: day.cashIn, note: "Payments less refunds" },
          { label: "Cash out", value: day.cashOut, note: "Cash expenses" },
          { label: "Should be in the drawer", value: day.expectedCash, strong: true },
        ].map((item) => (
          <div key={item.label} className="bg-surface px-5 py-4">
            <dt className="text-[12.5px] text-muted">{item.label}</dt>
            <dd className={item.strong ? "mt-1 text-[22px] font-semibold tabular-nums" : "mt-1 text-[18px] font-medium tabular-nums"}>{formatMoney(item.value)}</dd>
            {item.note && <dd className="text-[11.5px] text-faint">{item.note}</dd>}
          </div>
        ))}
      </dl>
      {methods.length > 0 && (
        <p className="border-t border-line px-5 py-3 text-[13px] text-muted">
          Also received: {methods.map(([method, amount]) => `${methodLabel(method as PaymentMethod)} ${formatMoney(amount)}`).join(" · ")}
        </p>
      )}
      <div className="border-t border-line px-5 py-5">
        {day.closed ? (
          <div className="flex items-start gap-3 text-[13.5px]">
            <Lock className="mt-0.5 h-4 w-4 text-ok" aria-hidden />
            <div>
              <p className="text-ink">
                Counted <strong className="tabular-nums">{formatMoney(day.closed.countedCash)}</strong> · <Difference value={day.closed.difference} />
              </p>
              <p className="text-muted">
                Closed by {day.closed.closedBy ?? "someone"} at {formatDateTime(day.closed.closedAt)}. Cash for this day is locked.
              </p>
              {day.closed.notes && <p className="mt-1 text-muted">“{day.closed.notes}”</p>}
            </div>
          </div>
        ) : (
          <CloseForm day={day} />
        )}
      </div>
    </Panel>
  );
}

function CloseForm({ day }: { day: Schemas["DayBook"] }) {
  const close = useCloseDay();
  const [counted, setCounted] = useState("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [confirming, setConfirming] = useState(false);
  const difference = counted && /^\d+(\.\d{1,2})?$/.test(counted) ? Number(counted) - day.expectedCash : null;

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    if (difference == null) {
      setError("Enter the cash you counted, like 4350");
      return;
    }
    if (!confirming) {
      setConfirming(true);
      return;
    }
    try {
      await close.mutateAsync({ date: day.date, countedCash: Number(counted), notes: notes.trim() || undefined });
      toast.success(`Closed ${formatDate(day.date)}`);
    } catch (e) {
      setError(errorMessage(e));
      setConfirming(false);
    }
  };

  return (
    <form onSubmit={submit} className="space-y-4" noValidate>
      <FormError message={error} />
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Cash you counted" hint={difference == null ? `Expected ${formatMoney(day.expectedCash)}` : undefined}>
          <Input inputMode="decimal" value={counted} onChange={(e) => { setCounted(e.target.value); setConfirming(false); }} />
        </Field>
        {difference != null && (
          <div className="self-end pb-2 text-[13.5px]">
            <Difference value={Math.round(difference * 100) / 100} />
          </div>
        )}
      </div>
      <Field label="Note" optional hint={difference ? "Worth saying why the count is different" : undefined}>
        <Textarea rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={500} />
      </Field>
      {confirming && (
        <p role="alert" className="rounded-[var(--radius-control)] border border-due/35 bg-due-soft px-3 py-2 text-[13px] text-ink">
          After closing, cash payments, refunds and expenses for {formatDate(day.date)} can&apos;t be added or voided. Close the day?
        </p>
      )}
      <div className="flex gap-2">
        <Button type="submit" variant="primary" loading={close.isPending}>{confirming ? "Yes, close the day" : "Close the day"}</Button>
        {confirming && <Button onClick={() => setConfirming(false)}>Not yet</Button>}
      </div>
      {day.date < addDays(localToday(), -1) && <p className="text-[12.5px] text-muted">You&apos;re closing an earlier day. Its opening cash comes from the last close before it.</p>}
    </form>
  );
}
