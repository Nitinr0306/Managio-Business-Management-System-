"use client";

import { Banknote, Ellipsis } from "lucide-react";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog, DialogBody, DialogFooter } from "@/components/ui/dialog";
import { Field, FormError, Input, Select } from "@/components/ui/field";
import { EmptyState, KpiStrip, PageHeader, Panel, Skeleton, Table, TD, TH, THead, TR } from "@/components/ui/layout";
import { Menu, MenuItem } from "@/components/ui/menu";
import { Pill } from "@/components/ui/pill";
import { PAYMENT_METHODS, type PaymentMethod } from "@/features/billing/api";
import type { Schemas } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import { formatMoney, formatMonth, localToday } from "@/lib/format";
import { useAdjustSalary, usePaySalary, usePayroll, usePreparePayroll } from "./api";

type Entry = Schemas["PayrollRow"];
const STATE: Record<Entry["state"], { label: string; tone: "ok" | "due" | "bad" }> = {
  PAID: { label: "Paid", tone: "ok" },
  PARTLY_PAID: { label: "Part paid", tone: "due" },
  UNPAID: { label: "Unpaid", tone: "bad" },
};

/**
 * Payroll (PAY-1, PAY-2): prepare a month's salaries from each person's monthly salary, then pay them, in parts
 * if needed. Every payment is recorded as a Salaries expense, so profit and the day book stay right.
 */
export function PayrollPage() {
  const [month, setMonth] = useState(localToday().slice(0, 7));
  const monthStart = `${month}-01`;
  const payroll = usePayroll(monthStart);
  const prepare = usePreparePayroll();
  const [paying, setPaying] = useState<Entry | null>(null);
  const [adjusting, setAdjusting] = useState<Entry | null>(null);
  const entries = payroll.data ?? [];
  const totals = entries.reduce((t, e) => ({ amount: t.amount + e.amount, paid: t.paid + e.paid, remaining: t.remaining + e.remaining }), { amount: 0, paid: 0, remaining: 0 });

  const prepareMonth = async () => {
    try {
      const prepared = await prepare.mutateAsync(monthStart);
      toast.success(`${formatMonth(monthStart)} payroll is ready: ${prepared.length} ${prepared.length === 1 ? "person" : "people"}`);
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  return (
    <>
      <PageHeader
        title="Payroll"
        subtitle="Salaries for each month. Set monthly salaries on the Team page."
        actions={
          <>
            <label className="flex items-center gap-2 text-[13px] text-muted">
              Month
              <Input type="month" value={month} max={localToday().slice(0, 7)} onChange={(e) => setMonth(e.target.value || localToday().slice(0, 7))} className="w-[160px]" />
            </label>
            <Button variant="primary" loading={prepare.isPending} onClick={prepareMonth}>Prepare {formatMonth(monthStart)}</Button>
          </>
        }
      />
      {entries.length > 0 && (
        <div className="mb-5">
          <KpiStrip
            items={[
              { label: "Salaries this month", value: formatMoney(totals.amount) },
              { label: "Paid", value: formatMoney(totals.paid), tone: "ok" },
              { label: "Still to pay", value: formatMoney(totals.remaining), note: totals.remaining > 0 ? "Pay before the month ends" : "All paid", tone: totals.remaining > 0 ? "due" : "ok" },
            ]}
          />
        </div>
      )}
      <Panel>
        {payroll.isPending ? (
          <div className="space-y-2 p-5">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-10 w-full" />)}</div>
        ) : payroll.isError ? (
          <EmptyState title="Payroll couldn't load" body={errorMessage(payroll.error)} />
        ) : entries.length === 0 ? (
          <EmptyState
            icon={<Banknote className="h-5 w-5" />}
            title={`${formatMonth(monthStart)} isn't prepared yet`}
            body="Preparing adds everyone on the team who has a monthly salary. People who join later are added when you prepare again."
            action={<Button variant="primary" loading={prepare.isPending} onClick={prepareMonth}>Prepare {formatMonth(monthStart)}</Button>}
          />
        ) : (
          <Table>
            <THead>
              <tr>
                <TH>Person</TH>
                <TH className="text-right">Salary</TH>
                <TH className="text-right">Paid</TH>
                <TH className="text-right">Still to pay</TH>
                <TH>Status</TH>
                <TH><span className="sr-only">Actions</span></TH>
              </tr>
            </THead>
            <tbody>
              {entries.map((e) => (
                <TR key={e.id}>
                  <TD>
                    <span className="font-medium">{e.fullName}</span>
                    {e.title && <span className="block text-[12px] text-muted">{e.title}</span>}
                    {e.notes && <span className="block text-[12px] text-muted">{e.notes}</span>}
                  </TD>
                  <TD className="text-right tabular-nums">{formatMoney(e.amount)}</TD>
                  <TD className="text-right tabular-nums">{formatMoney(e.paid)}</TD>
                  <TD className="text-right font-medium tabular-nums">{e.remaining > 0 ? formatMoney(e.remaining) : <span className="font-normal text-faint">—</span>}</TD>
                  <TD><Pill tone={STATE[e.state].tone}>{STATE[e.state].label}</Pill></TD>
                  <TD className="text-right">
                    <div className="flex justify-end gap-1">
                      {e.remaining > 0 && <Button size="sm" variant="primary" onClick={() => setPaying(e)}>Pay</Button>}
                      <Menu trigger={<Button size="icon" variant="ghost" aria-label={`More for ${e.fullName}`}><Ellipsis className="h-4 w-4" /></Button>}>
                        <MenuItem onSelect={() => setAdjusting(e)}>Bonus or deduction</MenuItem>
                      </Menu>
                    </div>
                  </TD>
                </TR>
              ))}
            </tbody>
          </Table>
        )}
      </Panel>
      <Dialog open={paying != null} onOpenChange={(o) => !o && setPaying(null)} title={paying ? `Pay ${paying.fullName}` : ""} size="sm">
        {paying && <PayForm entry={paying} onDone={() => setPaying(null)} />}
      </Dialog>
      <Dialog open={adjusting != null} onOpenChange={(o) => !o && setAdjusting(null)} title={adjusting ? `Change ${adjusting.fullName}'s salary for ${formatMonth(adjusting.period)}` : ""} size="sm">
        {adjusting && <AdjustForm entry={adjusting} onDone={() => setAdjusting(null)} />}
      </Dialog>
    </>
  );
}

function PayForm({ entry, onDone }: { entry: Entry; onDone: () => void }) {
  const pay = usePaySalary();
  const [amount, setAmount] = useState("");
  const [method, setMethod] = useState<PaymentMethod>("BANK_TRANSFER");
  const [paidOn, setPaidOn] = useState(localToday());
  const [error, setError] = useState<string | null>(null);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    if (amount && !/^\d+(\.\d{1,2})?$/.test(amount)) {
      setError("Enter the amount like 12000");
      return;
    }
    try {
      await pay.mutateAsync({ entryId: entry.id, body: { amount: amount ? Number(amount) : undefined, method, paidOn } });
      toast.success(`Paid ${entry.fullName}`, { description: "Recorded as a Salaries expense" });
      onDone();
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <form onSubmit={submit} className="flex min-h-0 flex-1 flex-col" noValidate>
      <DialogBody className="space-y-4">
        <FormError message={error} />
        <Field label="Amount" hint={`Empty pays the rest: ${formatMoney(entry.remaining)}`}>
          <Input autoFocus inputMode="decimal" placeholder={String(entry.remaining)} value={amount} onChange={(e) => setAmount(e.target.value)} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Paid by">
            <Select value={method} onChange={(e) => setMethod(e.target.value as PaymentMethod)}>
              {PAYMENT_METHODS.map((m) => (
                <option key={m.value} value={m.value}>{m.label}</option>
              ))}
            </Select>
          </Field>
          <Field label="Paid on">
            <Input type="date" max={localToday()} value={paidOn} onChange={(e) => setPaidOn(e.target.value)} />
          </Field>
        </div>
      </DialogBody>
      <DialogFooter>
        <Button onClick={onDone}>Cancel</Button>
        <Button type="submit" variant="primary" loading={pay.isPending}>Record payment</Button>
      </DialogFooter>
    </form>
  );
}

function AdjustForm({ entry, onDone }: { entry: Entry; onDone: () => void }) {
  const adjust = useAdjustSalary();
  const [amount, setAmount] = useState(String(entry.amount));
  const [notes, setNotes] = useState(entry.notes ?? "");
  const [error, setError] = useState<string | null>(null);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    if (!/^\d+(\.\d{1,2})?$/.test(amount)) {
      setError("Enter the amount like 27000");
      return;
    }
    try {
      await adjust.mutateAsync({ entryId: entry.id, body: { amount: Number(amount), notes: notes.trim() || undefined } });
      toast.success("Salary for the month updated");
      onDone();
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <form onSubmit={submit} className="flex min-h-0 flex-1 flex-col" noValidate>
      <DialogBody className="space-y-4">
        <FormError message={error} />
        <Field label="Salary for this month" hint={`Can't go below what's already paid (${formatMoney(entry.paid)})`}>
          <Input autoFocus inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} />
        </Field>
        <Field label="Why" optional hint="For example: Diwali bonus, or 2 days unpaid leave">
          <Input value={notes} onChange={(e) => setNotes(e.target.value)} />
        </Field>
      </DialogBody>
      <DialogFooter>
        <Button onClick={onDone}>Cancel</Button>
        <Button type="submit" variant="primary" loading={adjust.isPending}>Save</Button>
      </DialogFooter>
    </form>
  );
}
