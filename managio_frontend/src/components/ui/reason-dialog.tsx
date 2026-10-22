"use client";

import { useState, type FormEvent, type ReactNode } from "react";
import { errorMessage } from "@/lib/api/errors";
import { Button } from "./button";
import { Dialog, DialogBody, DialogFooter } from "./dialog";
import { Field, FormError, Textarea } from "./field";

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: ReactNode;
  description?: ReactNode;
  label?: string;
  confirmLabel: string;
  /** Voids, cancellations and rejections keep the record with a reason instead of deleting it. */
  requireReason?: boolean;
  danger?: boolean;
  onConfirm: (reason: string) => Promise<unknown>;
};

/** A confirmation that asks why. Used for voids, refunds' reasons, cancellations and rejections. */
export function ReasonDialog({ open, onOpenChange, title, description, label = "Reason", confirmLabel, requireReason = true, danger = true, onConfirm }: Props) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange} title={title} description={description} size="sm">
      <ReasonForm
        label={label}
        confirmLabel={confirmLabel}
        requireReason={requireReason}
        danger={danger}
        onConfirm={onConfirm}
        onDone={() => onOpenChange(false)}
      />
    </Dialog>
  );
}

function ReasonForm({ label, confirmLabel, requireReason, danger, onConfirm, onDone }: Required<Pick<Props, "label" | "confirmLabel" | "requireReason" | "danger" | "onConfirm">> & { onDone: () => void }) {
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const missing = requireReason && reason.trim() === "";

  const confirm = async (event: FormEvent) => {
    event.preventDefault();
    if (missing) {
      setError(`Enter the ${label.toLowerCase()}`);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await onConfirm(reason.trim());
      onDone();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <form onSubmit={confirm} className="flex min-h-0 flex-1 flex-col" noValidate>
      <DialogBody className="space-y-3">
        <FormError message={error} />
        <Field label={label} optional={!requireReason}>
          <Textarea autoFocus rows={3} value={reason} onChange={(e) => setReason(e.target.value)} maxLength={300} />
        </Field>
      </DialogBody>
      <DialogFooter>
        <Button onClick={onDone}>Keep it</Button>
        <Button type="submit" variant={danger ? "danger" : "primary"} loading={busy}>
          {confirmLabel}
        </Button>
      </DialogFooter>
    </form>
  );
}
