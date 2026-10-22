"use client";

import { Plus, Trash2 } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Controller, useFieldArray, useForm, useWatch } from "react-hook-form";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog, DialogBody, DialogFooter } from "@/components/ui/dialog";
import { Checkbox, Field, FormError, Input, Select } from "@/components/ui/field";
import { useBusiness } from "@/features/business/context";
import { CustomerPicker, type PickedCustomer } from "@/features/customers/customer-picker";
import { useOfferings } from "@/features/memberships/api";
import type { Schemas } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import { formatMoney, localToday } from "@/lib/format";
import { PAYMENT_METHODS, useCreateInvoice, useSell, type PaymentMethod } from "./api";

type Line = { offeringId: string; description: string; quantity: string; unitPrice: string; taxRate: string };
type Values = {
  customer: PickedCustomer | null;
  lines: Line[];
  discount: string;
  dueOn: string;
  notes: string;
  takePayment: boolean;
  method: PaymentMethod;
  amount: string;
  reference: string;
};

const emptyLine: Line = { offeringId: "", description: "", quantity: "1", unitPrice: "", taxRate: "" };
const money = /^\d+(\.\d{1,2})?$/;

/**
 * A counter sale (invoice and payment together, BIL-3) or an invoice to be paid later. Lines can come from
 * the catalogue (price and tax filled in, stock counted) or be typed in.
 */
export function SaleDialog({ open, onOpenChange, customer }: { open: boolean; onOpenChange: (open: boolean) => void; customer?: PickedCustomer }) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange} title="New sale or invoice" size="lg">
      <SaleForm initialCustomer={customer ?? null} onDone={() => onOpenChange(false)} />
    </Dialog>
  );
}

function SaleForm({ initialCustomer, onDone }: { initialCustomer: PickedCustomer | null; onDone: () => void }) {
  const business = useBusiness();
  const router = useRouter();
  const catalog = useOfferings();
  const sell = useSell();
  const create = useCreateInvoice();
  const [formError, setFormError] = useState<string | null>(null);
  const { control, register, handleSubmit, setValue } = useForm<Values>({
    defaultValues: {
      customer: initialCustomer,
      lines: [{ ...emptyLine }],
      discount: "",
      dueOn: localToday(),
      notes: "",
      takePayment: true,
      method: "CASH",
      amount: "",
      reference: "",
    },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "lines" });
  const lines = useWatch({ control, name: "lines" });
  const discount = Number(useWatch({ control, name: "discount" }) || 0);
  const takePayment = useWatch({ control, name: "takePayment" });
  const items = (catalog.data ?? []).filter((o) => o.kind !== "PLAN");

  const subtotal = lines.reduce((sum, l) => sum + (Number(l.quantity) || 0) * (Number(l.unitPrice) || 0), 0);
  const tax = lines.reduce((sum, l) => {
    const amount = (Number(l.quantity) || 0) * (Number(l.unitPrice) || 0);
    const share = subtotal > 0 ? amount / subtotal : 0;
    return sum + (amount - discount * share) * ((Number(l.taxRate) || 0) / 100);
  }, 0);
  const total = Math.max(subtotal - discount, 0) + tax;

  const pick = (index: number, offeringId: string) => {
    const offering = items.find((o) => String(o.id) === offeringId);
    setValue(`lines.${index}.offeringId`, offeringId);
    if (offering) {
      setValue(`lines.${index}.description`, offering.name);
      setValue(`lines.${index}.unitPrice`, String(offering.price));
      setValue(`lines.${index}.taxRate`, String(offering.taxRate));
    }
  };

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    const problems: string[] = [];
    const body: Schemas["InvoiceBody"] = {
      customerId: values.customer?.id,
      lines: values.lines.map((l, i) => {
        if (!l.offeringId && !l.description.trim()) problems.push(`Line ${i + 1}: describe what was sold`);
        if (!l.offeringId && !money.test(l.unitPrice)) problems.push(`Line ${i + 1}: enter a price`);
        return {
          offeringId: l.offeringId ? Number(l.offeringId) : undefined,
          description: l.description.trim() || undefined,
          quantity: Number(l.quantity) || 1,
          unitPrice: l.unitPrice ? Number(l.unitPrice) : undefined,
          taxRate: l.taxRate ? Number(l.taxRate) : undefined,
        };
      }),
      discount: values.discount ? Number(values.discount) : undefined,
      notes: values.notes.trim() || undefined,
    };
    if (values.discount && !money.test(values.discount)) problems.push("Enter the discount like 100 or 99.50");
    if (values.takePayment && values.amount && !money.test(values.amount)) problems.push("Enter the amount like 1500");
    if (problems.length) {
      setFormError(problems.join(". ") + ".");
      return;
    }
    try {
      if (values.takePayment) {
        const sale = await sell.mutateAsync({
          invoice: body,
          payment: { method: values.method, amount: values.amount ? Number(values.amount) : undefined, reference: values.reference || undefined },
        });
        toast.success(`Sold ${formatMoney(sale.invoice.total)}`, { description: sale.payment ? `Receipt ${sale.payment.receiptNumber}` : sale.invoice.invoiceNumber });
        onDone();
      } else {
        const invoice = await create.mutateAsync({ ...body, dueOn: values.dueOn || undefined });
        toast.success(`Invoice ${invoice.invoiceNumber} created`);
        onDone();
        router.push(`/app/${business.id}/billing/invoices/${invoice.id}`);
      }
    } catch (error) {
      setFormError(errorMessage(error));
    }
  });

  return (
    <form onSubmit={submit} className="flex min-h-0 flex-1 flex-col" noValidate>
      <DialogBody className="space-y-5">
        <FormError message={formError} />
        <Field label={business.labels.customer} optional hint="Leave empty for a walk-in sale">
          <Controller control={control} name="customer" render={({ field }) => <CustomerPicker value={field.value} onChange={field.onChange} />} />
        </Field>

        <fieldset>
          <legend className="mb-2 text-[13px] font-medium text-ink">What was sold</legend>
          <div className="space-y-2">
            {fields.map((field, index) => (
              <div key={field.id} className="grid grid-cols-[1fr_64px_96px_64px_32px] items-end gap-2 sm:grid-cols-[180px_1fr_64px_100px_68px_32px]">
                <div className="col-span-5 sm:col-span-1">
                  <label className="sr-only" htmlFor={`item-${index}`}>Item from catalogue</label>
                  <Select id={`item-${index}`} value={lines[index]?.offeringId ?? ""} onChange={(e) => pick(index, e.target.value)}>
                    <option value="">Type it in</option>
                    {items.map((o) => (
                      <option key={o.id} value={o.id}>
                        {o.name} · {formatMoney(o.price)}
                        {o.trackStock ? ` · ${o.stockQuantity ?? 0} in stock` : ""}
                      </option>
                    ))}
                  </Select>
                </div>
                <Input aria-label={`Line ${index + 1} description`} placeholder="Description" {...register(`lines.${index}.description`)} />
                <Input aria-label={`Line ${index + 1} quantity`} inputMode="decimal" placeholder="Qty" {...register(`lines.${index}.quantity`)} />
                <Input
                  aria-label={`Line ${index + 1} price`}
                  inputMode="decimal"
                  placeholder="Price"
                  readOnly={Boolean(lines[index]?.offeringId) && !business.can("billing.discount")}
                  {...register(`lines.${index}.unitPrice`)}
                />
                <Input aria-label={`Line ${index + 1} tax rate`} inputMode="decimal" placeholder="Tax %" {...register(`lines.${index}.taxRate`)} />
                <Button size="icon" variant="ghost" aria-label={`Remove line ${index + 1}`} disabled={fields.length === 1} onClick={() => remove(index)}>
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            ))}
          </div>
          <Button size="sm" variant="ghost" className="mt-2" icon={<Plus className="h-4 w-4" />} onClick={() => append({ ...emptyLine })}>
            Add a line
          </Button>
        </fieldset>

        <div className="grid gap-4 sm:grid-cols-3">
          {business.can("billing.discount") && (
            <Field label="Discount" optional>
              <Input inputMode="decimal" placeholder="0" {...register("discount")} />
            </Field>
          )}
          <Field label="Note" optional className="sm:col-span-2">
            <Input {...register("notes")} />
          </Field>
        </div>

        <div className="flex flex-wrap items-baseline justify-end gap-x-6 gap-y-1 rounded-[var(--radius-control)] bg-canvas px-4 py-3 text-[13px] text-muted">
          <span>Subtotal <span className="tabular-nums text-ink">{formatMoney(subtotal)}</span></span>
          {discount > 0 && <span>Discount <span className="tabular-nums text-ink">−{formatMoney(discount)}</span></span>}
          {tax > 0 && <span>Tax <span className="tabular-nums text-ink">{formatMoney(Math.round(tax * 100) / 100)}</span></span>}
          <span className="text-[15px]">Total <span className="font-semibold tabular-nums text-ink">{formatMoney(Math.round(total * 100) / 100)}</span></span>
        </div>

        <div className="space-y-3 border-t border-line pt-4">
          <Checkbox label="Paid now" hint="Untick to create an invoice to be paid later" {...register("takePayment")} />
          {takePayment ? (
            <div className="grid gap-4 sm:grid-cols-3">
              <Field label="Method">
                <Select {...register("method")}>
                  {PAYMENT_METHODS.map((m) => (
                    <option key={m.value} value={m.value}>{m.label}</option>
                  ))}
                </Select>
              </Field>
              <Field label="Amount" hint="Empty means the full total">
                <Input inputMode="decimal" placeholder={String(Math.round(total * 100) / 100)} {...register("amount")} />
              </Field>
              <Field label="Reference" optional>
                <Input {...register("reference")} />
              </Field>
            </div>
          ) : (
            <Field label="Due on" className="sm:w-1/3">
              <Input type="date" {...register("dueOn")} />
            </Field>
          )}
        </div>
      </DialogBody>
      <DialogFooter>
        <Button onClick={onDone}>Cancel</Button>
        <Button type="submit" variant="primary" loading={sell.isPending || create.isPending}>
          {takePayment ? "Record sale" : "Create invoice"}
        </Button>
      </DialogFooter>
    </form>
  );
}
