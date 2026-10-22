"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Dialog, DialogBody, DialogFooter } from "@/components/ui/dialog";
import { Checkbox, Field, FormError, Input, Select } from "@/components/ui/field";
import { LoadingBlock } from "@/components/ui/spinner";
import { useBusiness } from "@/features/business/context";
import { PAYMENT_METHODS, type PaymentMethod } from "@/features/billing/api";
import type { Schemas } from "@/lib/api/client";
import { applyApiError } from "@/lib/forms";
import { formatDate, formatMoney, localToday } from "@/lib/format";
import { durationLabel, useOfferings, useRenewMembership, useStartMembership } from "./api";

const amount = z.string().regex(/^\d*(\.\d{0,2})?$/, "Enter an amount like 1500 or 1500.50");
const schema = z.object({
  offeringId: z.string().min(1, "Choose a plan"),
  startsOn: z.string(),
  discount: amount,
  payNow: z.boolean(),
  method: z.string(),
  amount,
  reference: z.string().max(100, "Use 100 characters or fewer"),
});
type Values = z.infer<typeof schema>;

type Renewable = Pick<Schemas["MembershipResponse"], "id" | "offeringId" | "planName" | "endsOn" | "state">;
type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  customer: { id: number; fullName: string };
  /** Renew this membership instead of starting a new one. */
  renewFrom?: Renewable;
};

/**
 * Starting or renewing a membership creates its invoice(s) (MEM-3, MEM-4) and can take the first payment in
 * the same step, which is what the front desk does most of the day.
 */
export function MembershipDialog({ open, onOpenChange, customer, renewFrom }: Props) {
  const business = useBusiness();
  const title = renewFrom ? `Renew ${customer.fullName}` : `Start a ${business.labels.plan.toLowerCase()} for ${customer.fullName}`;
  const description = renewFrom
    ? renewFrom.state === "EXPIRED"
      ? `Their ${renewFrom.planName} ended on ${formatDate(renewFrom.endsOn)}. The renewal starts today.`
      : `The renewal starts the day after ${formatDate(renewFrom.endsOn)}, so no days are lost.`
    : undefined;
  return (
    <Dialog open={open} onOpenChange={onOpenChange} title={title} description={description}>
      <MembershipForm customer={customer} renewFrom={renewFrom} onDone={() => onOpenChange(false)} />
    </Dialog>
  );
}

function MembershipForm({ customer, renewFrom, onDone }: { customer: Props["customer"]; renewFrom?: Renewable; onDone: () => void }) {
  const plans = useOfferings("PLAN");
  if (plans.isPending) return <LoadingBlock />;
  return <MembershipFields customer={customer} renewFrom={renewFrom} onDone={onDone} plans={plans.data ?? []} />;
}

function MembershipFields({ customer, renewFrom, onDone, plans }: {
  customer: Props["customer"];
  renewFrom?: Renewable;
  onDone: () => void;
  plans: Schemas["OfferingResponse"][];
}) {
  const business = useBusiness();
  const start = useStartMembership();
  const renew = useRenewMembership();
  const [formError, setFormError] = useState<string | null>(null);
  const canTakePayment = business.can("billing.collect");
  const canDiscount = business.can("billing.discount");
  const renewsSamePlan = renewFrom && plans.some((p) => p.id === renewFrom.offeringId);

  const { register, handleSubmit, formState, setError, control } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      offeringId: renewsSamePlan ? String(renewFrom.offeringId) : plans.length === 1 ? String(plans[0].id) : "",
      startsOn: localToday(),
      discount: "",
      payNow: canTakePayment,
      method: "CASH",
      amount: "",
      reference: "",
    },
  });

  const offeringId = useWatch({ control, name: "offeringId" });
  const payNow = useWatch({ control, name: "payNow" });
  const discount = Number(useWatch({ control, name: "discount" }) || 0);
  const plan = plans.find((p) => String(p.id) === offeringId);
  const total = plan ? Math.max(plan.price - discount, 0) : 0;
  const firstInstallment = plan && plan.installments > 1 ? Math.round((total / plan.installments) * 100) / 100 : total;
  const takingPayment = payNow && canTakePayment && total > 0;

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    const payment = takingPayment
      ? { method: values.method as PaymentMethod, amount: values.amount ? Number(values.amount) : undefined, reference: values.reference || undefined }
      : undefined;
    const discountValue = values.discount ? Number(values.discount) : undefined;
    try {
      if (renewFrom) {
        await renew.mutateAsync({ membershipId: renewFrom.id, body: { offeringId: Number(values.offeringId), discount: discountValue, payNow: payment } });
        toast.success(`Renewed ${customer.fullName}'s ${plan?.name ?? "plan"}`);
      } else {
        await start.mutateAsync({
          customerId: customer.id,
          offeringId: Number(values.offeringId),
          startsOn: values.startsOn || undefined,
          discount: discountValue,
          payNow: payment,
        });
        toast.success(`Started ${plan?.name ?? "the plan"} for ${customer.fullName}`);
      }
      onDone();
    } catch (error) {
      setFormError(applyApiError(error, setError, ["offeringId", "startsOn", "discount", "amount", "method", "reference"]));
    }
  });

  if (plans.length === 0) {
    return (
      <>
        <DialogBody>
          <p className="text-[13.5px] text-muted">
            There are no {business.labels.plans.toLowerCase()} yet. Add one under {business.labels.plans} first.
          </p>
        </DialogBody>
        <DialogFooter>
          <Button onClick={onDone}>Close</Button>
        </DialogFooter>
      </>
    );
  }

  return (
    <form onSubmit={submit} className="flex min-h-0 flex-1 flex-col" noValidate>
      <DialogBody className="space-y-4">
        <FormError message={formError} />
        <Field label={business.labels.plan} error={formState.errors.offeringId?.message}>
          <Select {...register("offeringId")}>
            <option value="">Choose…</option>
            {plans.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} · {durationLabel(p)} · {formatMoney(p.price)}
                {p.installments > 1 ? ` in ${p.installments} parts` : ""}
              </option>
            ))}
          </Select>
        </Field>
        {(!renewFrom || canDiscount) && (
          <div className="grid gap-4 sm:grid-cols-2">
            {!renewFrom && (
              <Field label="Starts on" error={formState.errors.startsOn?.message}>
                <Input type="date" {...register("startsOn")} />
              </Field>
            )}
            {canDiscount && (
              <Field label="Discount" optional error={formState.errors.discount?.message}>
                <Input inputMode="decimal" placeholder="0" {...register("discount")} />
              </Field>
            )}
          </div>
        )}
        {plan && (
          <p className="rounded-[var(--radius-control)] bg-canvas px-3 py-2.5 text-[13px] text-muted">
            Total <span className="font-semibold text-ink">{formatMoney(total)}</span>
            {plan.installments > 1 && <> in {plan.installments} invoices of about {formatMoney(firstInstallment)}, one a month</>}
          </p>
        )}
        {canTakePayment && (
          <div className="space-y-3 border-t border-line pt-4">
            <Checkbox label="Take payment now" {...register("payNow")} />
            {payNow && (
              <div className="grid gap-4 sm:grid-cols-3">
                <Field label="Method">
                  <Select {...register("method")}>
                    {PAYMENT_METHODS.map((m) => (
                      <option key={m.value} value={m.value}>
                        {m.label}
                      </option>
                    ))}
                  </Select>
                </Field>
                <Field label="Amount" hint={plan ? `Full: ${formatMoney(firstInstallment)}` : undefined} error={formState.errors.amount?.message}>
                  <Input inputMode="decimal" placeholder={plan ? String(firstInstallment) : ""} {...register("amount")} />
                </Field>
                <Field label="Reference" optional error={formState.errors.reference?.message}>
                  <Input placeholder="UPI ref, cheque no." {...register("reference")} />
                </Field>
              </div>
            )}
          </div>
        )}
      </DialogBody>
      <DialogFooter>
        <Button onClick={onDone}>Cancel</Button>
        <Button type="submit" variant="primary" loading={start.isPending || renew.isPending}>
          {renewFrom ? "Renew" : "Start"}
          {takingPayment ? ` and take ${formatMoney(firstInstallment)}` : ""}
        </Button>
      </DialogFooter>
    </form>
  );
}
