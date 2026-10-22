"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Dialog, DialogBody, DialogFooter } from "@/components/ui/dialog";
import { Field, FormError, Input, Select } from "@/components/ui/field";
import { LoadingBlock } from "@/components/ui/spinner";
import type { Schemas } from "@/lib/api/client";
import { applyApiError } from "@/lib/forms";
import { formatDate, formatMoney, localToday } from "@/lib/format";
import { PAYMENT_METHODS, useInvoices, useRecordPayment, type PaymentMethod } from "./api";

const schema = z.object({
  invoiceId: z.string().min(1, "Choose the invoice"),
  amount: z.string().regex(/^\d*(\.\d{0,2})?$/, "Enter an amount like 1500"),
  method: z.string(),
  paidOn: z.string(),
  reference: z.string().max(100, "Use 100 characters or fewer"),
  notes: z.string().max(500, "Use 500 characters or fewer"),
});
type Values = z.infer<typeof schema>;

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  customer: { id: number; fullName: string };
  /** Pre-select this invoice; otherwise the oldest open invoice is chosen. */
  invoiceId?: number;
};

/** Records money received against one of the customer's open invoices (BIL-4). Leave the amount empty to pay it all. */
export function PaymentDialog({ open, onOpenChange, customer, invoiceId }: Props) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange} title={`Record payment from ${customer.fullName}`}>
      <PaymentLoader customer={customer} invoiceId={invoiceId} onDone={() => onOpenChange(false)} />
    </Dialog>
  );
}

function PaymentLoader({ customer, invoiceId, onDone }: { customer: Props["customer"]; invoiceId?: number; onDone: () => void }) {
  const invoices = useInvoices({ view: "OPEN", customerId: customer.id, size: 50 });
  if (invoices.isPending) return <LoadingBlock />;
  const open = (invoices.data?.items ?? []).slice().sort((a, b) => a.dueOn.localeCompare(b.dueOn));
  if (open.length === 0) {
    return (
      <>
        <DialogBody>
          <p className="text-[13.5px] text-muted">{customer.fullName} has nothing to pay right now.</p>
        </DialogBody>
        <DialogFooter>
          <Button onClick={onDone}>Close</Button>
        </DialogFooter>
      </>
    );
  }
  return <PaymentForm customer={customer} invoices={open} initialInvoiceId={invoiceId ?? open[0].id} onDone={onDone} />;
}

function PaymentForm({ customer, invoices, initialInvoiceId, onDone }: {
  customer: Props["customer"];
  invoices: Schemas["InvoiceRow"][];
  initialInvoiceId: number;
  onDone: () => void;
}) {
  const record = useRecordPayment();
  const [formError, setFormError] = useState<string | null>(null);
  const { register, handleSubmit, formState, setError, control } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { invoiceId: String(initialInvoiceId), amount: "", method: "CASH", paidOn: localToday(), reference: "", notes: "" },
  });
  const selectedId = useWatch({ control, name: "invoiceId" });
  const selected = invoices.find((i) => String(i.id) === selectedId);

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      const payment = await record.mutateAsync({
        invoiceId: Number(values.invoiceId),
        body: {
          amount: values.amount ? Number(values.amount) : undefined,
          method: values.method as PaymentMethod,
          paidOn: values.paidOn || undefined,
          reference: values.reference || undefined,
          notes: values.notes || undefined,
        },
      });
      toast.success(`Recorded ${formatMoney(payment.amount)} from ${customer.fullName}`, { description: `Receipt ${payment.receiptNumber}` });
      onDone();
    } catch (error) {
      setFormError(applyApiError(error, setError, ["amount", "method", "paidOn", "reference", "notes"]));
    }
  });

  return (
    <form onSubmit={submit} className="flex min-h-0 flex-1 flex-col" noValidate>
      <DialogBody className="space-y-4">
        <FormError message={formError} />
        <Field label="For invoice" error={formState.errors.invoiceId?.message}>
          <Select {...register("invoiceId")}>
            {invoices.map((invoice) => (
              <option key={invoice.id} value={invoice.id}>
                {invoice.invoiceNumber} · due {formatDate(invoice.dueOn)} · {formatMoney(invoice.balance)} left
              </option>
            ))}
          </Select>
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Amount" hint={selected ? `Leave empty to pay all ${formatMoney(selected.balance)}` : undefined} error={formState.errors.amount?.message}>
            <Input inputMode="decimal" placeholder={selected ? String(selected.balance) : ""} {...register("amount")} />
          </Field>
          <Field label="Method">
            <Select {...register("method")}>
              {PAYMENT_METHODS.map((m) => (
                <option key={m.value} value={m.value}>
                  {m.label}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Paid on" error={formState.errors.paidOn?.message}>
            <Input type="date" max={localToday()} {...register("paidOn")} />
          </Field>
          <Field label="Reference" optional error={formState.errors.reference?.message}>
            <Input placeholder="UPI ref, cheque no." {...register("reference")} />
          </Field>
        </div>
        <Field label="Note" optional error={formState.errors.notes?.message}>
          <Input {...register("notes")} />
        </Field>
      </DialogBody>
      <DialogFooter>
        <Button onClick={onDone}>Cancel</Button>
        <Button type="submit" variant="primary" loading={record.isPending}>
          Record payment
        </Button>
      </DialogFooter>
    </form>
  );
}
