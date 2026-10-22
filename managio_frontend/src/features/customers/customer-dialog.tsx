"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Dialog, DialogBody, DialogFooter } from "@/components/ui/dialog";
import { Field, FormError, Input, Select, Textarea } from "@/components/ui/field";
import { useBusiness } from "@/features/business/context";
import { ApiError } from "@/lib/api/errors";
import type { Schemas } from "@/lib/api/client";
import { applyApiError } from "@/lib/forms";
import { useSaveCustomer } from "./api";

const schema = z.object({
  fullName: z.string().trim().min(1, "Enter a name").max(150, "Use 150 characters or fewer"),
  phone: z.string().trim().max(20, "Use 20 characters or fewer").optional(),
  email: z.union([z.literal(""), z.email("Enter a valid email")]).optional(),
  dateOfBirth: z.string().optional(),
  gender: z.string().optional(),
  address: z.string().max(300).optional(),
  guardianName: z.string().max(150).optional(),
  guardianPhone: z.string().max(20).optional(),
  notes: z.string().max(2000).optional(),
  joinedOn: z.string().optional(),
});
type Values = z.infer<typeof schema>;
const FIELDS = ["fullName", "phone", "email", "dateOfBirth", "gender", "address", "guardianName", "guardianPhone", "notes", "joinedOn"] as const;

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  customer?: Schemas["CustomerResponse"];
  onSaved?: (customer: Schemas["CustomerResponse"]) => void;
};

const toValues = (c?: Schemas["CustomerResponse"]): Values => ({
  fullName: c?.fullName ?? "",
  phone: c?.phone ?? "",
  email: c?.email ?? "",
  dateOfBirth: c?.dateOfBirth ?? "",
  gender: c?.gender ?? "",
  address: c?.address ?? "",
  guardianName: c?.guardianName ?? "",
  guardianPhone: c?.guardianPhone ?? "",
  notes: c?.notes ?? "",
  joinedOn: c?.joinedOn ?? "",
});

/** Adds or edits a customer. A phone number another customer already has needs a second, deliberate click (CUS-4). */
export function CustomerDialog({ open, onOpenChange, customer, onSaved }: Props) {
  const business = useBusiness();
  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title={customer ? `Edit ${customer.fullName}` : `Add a ${business.labels.customer.toLowerCase()}`}
    >
      <CustomerForm customer={customer} onSaved={onSaved} onDone={() => onOpenChange(false)} />
    </Dialog>
  );
}

function CustomerForm({ customer, onSaved, onDone }: Pick<Props, "customer" | "onSaved"> & { onDone: () => void }) {
  const business = useBusiness();
  const save = useSaveCustomer(customer?.id);
  const [formError, setFormError] = useState<string | null>(null);
  const [duplicate, setDuplicate] = useState<string | null>(null);
  const { register, handleSubmit, formState, setError, getValues } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: toValues(customer),
  });
  const showGuardian = business.type === "SCHOOL" || business.type === "COACHING" || Boolean(customer?.guardianName);
  const noun = business.labels.customer.toLowerCase();

  const send = async (values: Values, confirmDuplicate: boolean) => {
    setFormError(null);
    const body: Schemas["CustomerRequest"] = { fullName: values.fullName, confirmDuplicate };
    for (const key of FIELDS) {
      if (key !== "fullName" && values[key]) {
        (body as Record<string, unknown>)[key] = values[key];
      }
    }
    try {
      const saved = await save.mutateAsync(body);
      toast.success(customer ? `Saved ${saved.fullName}` : `Added ${saved.fullName} as #${saved.number}`);
      onDone();
      onSaved?.(saved);
    } catch (error) {
      if (error instanceof ApiError && error.is("customer.duplicate_phone")) {
        setDuplicate(error.message);
        return;
      }
      setFormError(applyApiError(error, setError, FIELDS));
    }
  };

  const submit = handleSubmit((values) => send(values, false));

  return (
    <form onSubmit={submit} className="flex min-h-0 flex-1 flex-col" noValidate>
      <DialogBody className="space-y-4">
        <FormError message={formError} />
        {duplicate && (
          <div role="alert" className="rounded-[var(--radius-control)] border border-due/35 bg-due-soft px-3 py-2 text-[13px] text-ink">
            {duplicate}
          </div>
        )}
        <Field label="Full name" error={formState.errors.fullName?.message}>
          <Input autoFocus autoComplete="off" {...register("fullName")} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Phone" optional error={formState.errors.phone?.message}>
            <Input type="tel" inputMode="tel" placeholder="98765 43210" {...register("phone")} />
          </Field>
          <Field label="Email" optional hint="Needed to invite them to the portal" error={formState.errors.email?.message}>
            <Input type="email" {...register("email")} />
          </Field>
          <Field label="Date of birth" optional error={formState.errors.dateOfBirth?.message}>
            <Input type="date" {...register("dateOfBirth")} />
          </Field>
          <Field label="Gender" optional>
            <Select {...register("gender")}>
              <option value="">Not given</option>
              <option value="FEMALE">Female</option>
              <option value="MALE">Male</option>
              <option value="OTHER">Other</option>
            </Select>
          </Field>
        </div>
        {showGuardian && (
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Parent or guardian" optional>
              <Input {...register("guardianName")} />
            </Field>
            <Field label="Guardian's phone" optional>
              <Input type="tel" inputMode="tel" {...register("guardianPhone")} />
            </Field>
          </div>
        )}
        <Field label="Address" optional>
          <Input {...register("address")} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Joined on" optional hint="Defaults to today" error={formState.errors.joinedOn?.message}>
            <Input type="date" {...register("joinedOn")} />
          </Field>
        </div>
        <Field label="Notes" optional>
          <Textarea {...register("notes")} />
        </Field>
      </DialogBody>
      <DialogFooter>
        {duplicate ? (
          <>
            <Button onClick={() => setDuplicate(null)}>Change the phone</Button>
            <Button variant="primary" loading={save.isPending} onClick={() => send(getValues(), true)}>
              {customer ? "Save anyway" : "Add anyway"}
            </Button>
          </>
        ) : (
          <>
            <Button onClick={onDone}>Cancel</Button>
            <Button type="submit" variant="primary" loading={save.isPending}>
              {customer ? "Save" : `Add ${noun}`}
            </Button>
          </>
        )}
      </DialogFooter>
    </form>
  );
}
