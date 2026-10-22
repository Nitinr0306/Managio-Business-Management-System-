"use client";

import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog, DialogBody, DialogFooter } from "@/components/ui/dialog";
import { Field, FormError, Input, Select, Textarea } from "@/components/ui/field";
import type { Schemas } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import { formatMoney } from "@/lib/format";
import { PAYMENT_METHODS, useRefund, type PaymentMethod } from "./api";

/** Gives money back against a payment (BIL-5). The refund is its own record; the payment stays. */
export function RefundDialog({ payment, onClose }: { payment: Schemas["PaymentResponse"] | null; onClose: () => void }) {
  return (
    <Dialog open={payment != null} onOpenChange={(o) => !o && onClose()} title={payment ? `Refund from ${payment.receiptNumber}` : "Refund"} size="sm">
      {payment && <RefundForm payment={payment} onDone={onClose} />}
    </Dialog>
  );
}

function RefundForm({ payment, onDone }: { payment: Schemas["PaymentResponse"]; onDone: () => void }) {
  const refund = useRefund();
  const [amount, setAmount] = useState("");
  const [method, setMethod] = useState<PaymentMethod>(payment.method);
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    if (amount && !/^\d+(\.\d{1,2})?$/.test(amount)) {
      setError("Enter the amount like 500 or 499.50");
      return;
    }
    if (!reason.trim()) {
      setError("Say why the money is going back");
      return;
    }
    try {
      const result = await refund.mutateAsync({
        paymentId: payment.id,
        body: { amount: amount ? Number(amount) : undefined, method, reason: reason.trim() },
      });
      toast.success(`Refunded ${formatMoney(result.amount)}`, { description: result.receiptNumber });
      onDone();
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <form onSubmit={submit} className="flex min-h-0 flex-1 flex-col" noValidate>
      <DialogBody className="space-y-4">
        <FormError message={error} />
        <p className="text-[13px] text-muted">
          {payment.customer?.name ?? "Walk-in"} paid {formatMoney(payment.amount)} by {payment.method.toLowerCase().replace("_", " ")}.
        </p>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Amount" hint={`Empty means all of it`}>
            <Input inputMode="decimal" placeholder={String(payment.amount)} value={amount} onChange={(e) => setAmount(e.target.value)} />
          </Field>
          <Field label="Paid back by">
            <Select value={method} onChange={(e) => setMethod(e.target.value as PaymentMethod)}>
              {PAYMENT_METHODS.map((m) => (
                <option key={m.value} value={m.value}>{m.label}</option>
              ))}
            </Select>
          </Field>
        </div>
        <Field label="Reason">
          <Textarea rows={2} value={reason} onChange={(e) => setReason(e.target.value)} maxLength={300} />
        </Field>
      </DialogBody>
      <DialogFooter>
        <Button onClick={onDone}>Keep it</Button>
        <Button type="submit" variant="danger" loading={refund.isPending}>Refund</Button>
      </DialogFooter>
    </form>
  );
}
