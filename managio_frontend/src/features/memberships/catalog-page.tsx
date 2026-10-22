"use client";

import { Ellipsis, Plus, Tags } from "lucide-react";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog, DialogBody, DialogFooter } from "@/components/ui/dialog";
import { Checkbox, Field, FormError, Input, Select, Textarea } from "@/components/ui/field";
import { EmptyState, PageHeader, Panel, Skeleton, Table, TD, TH, THead, TR } from "@/components/ui/layout";
import { Menu, MenuItem, Tabs } from "@/components/ui/menu";
import { Pill, Tag } from "@/components/ui/pill";
import { useBusiness } from "@/features/business/context";
import type { Schemas } from "@/lib/api/client";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { formatMoney, formatNumber } from "@/lib/format";
import { durationLabel, useAdjustStock, useDeleteOffering, useOfferings, useSaveOffering, useUpdateOffering, type OfferingKind } from "./api";

type Offering = Schemas["OfferingResponse"];

/** What the business sells: plans that run for a period, services, and products with optional stock (MEM-7, BIL-3). */
export function CatalogPage() {
  const business = useBusiness();
  const kinds: OfferingKind[] = [
    ...(business.has("MEMBERSHIPS") ? (["PLAN"] as const) : []),
    ...(business.has("CATALOG") ? (["SERVICE", "PRODUCT"] as const) : []),
  ];
  const [kind, setKind] = useState<OfferingKind>(kinds[0] ?? "PLAN");
  const [showInactive, setShowInactive] = useState(false);
  const [editing, setEditing] = useState<Offering | "new" | null>(null);
  const [stock, setStock] = useState<Offering | null>(null);
  const offerings = useOfferings(kind, showInactive);
  const update = useUpdateOffering();
  const remove = useDeleteOffering();
  const canManage = business.can("offerings.manage");
  const names: Record<OfferingKind, string> = { PLAN: business.labels.plans, SERVICE: "Services", PRODUCT: "Products" };
  const singular: Record<OfferingKind, string> = { PLAN: business.labels.plan.toLowerCase(), SERVICE: "service", PRODUCT: "product" };

  if (kinds.length === 0) {
    return (
      <>
        <PageHeader title="What you sell" />
        <EmptyState title="Plans and products are turned off" body="An owner can turn on Memberships or Products and services in Settings." />
      </>
    );
  }

  const toggleActive = async (offering: Offering) => {
    try {
      await update.mutateAsync({ offeringId: offering.id, body: { ...toRequest(offering), active: !offering.active } });
      toast.success(offering.active ? `${offering.name} is hidden from new sales` : `${offering.name} is available again`);
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  const deleteOffering = async (offering: Offering) => {
    try {
      await remove.mutateAsync(offering.id);
      toast.success(`Deleted ${offering.name}`);
    } catch (error) {
      toast.error(error instanceof ApiError && error.is("offering.in_use") ? `${offering.name} has been sold before, so it can only be deactivated.` : errorMessage(error));
    }
  };

  return (
    <>
      <PageHeader
        title={business.has("CATALOG") ? `${business.labels.plans} and products` : business.labels.plans}
        subtitle="Prices here are before tax. Changing a price doesn't change what people already bought."
        actions={canManage && <Button variant="primary" icon={<Plus className="h-4 w-4" />} onClick={() => setEditing("new")}>Add {singular[kind]}</Button>}
      />
      {kinds.length > 1 && <Tabs value={kind} onValueChange={(v) => setKind(v as OfferingKind)} items={kinds.map((k) => ({ value: k, label: names[k] }))} />}
      <Panel className="mt-4" actions={<Checkbox label="Show inactive" checked={showInactive} onChange={(e) => setShowInactive(e.target.checked)} />} title={names[kind]}>
        {offerings.isPending ? (
          <div className="space-y-2 p-5">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-9 w-full" />)}</div>
        ) : (offerings.data ?? []).length === 0 ? (
          <EmptyState
            icon={<Tags className="h-5 w-5" />}
            title={`No ${names[kind].toLowerCase()} yet`}
            body={kind === "PLAN" ? `Add the ${business.labels.plans.toLowerCase()} you offer, for example a 1-month or 3-month plan.` : undefined}
            action={canManage && <Button variant="primary" onClick={() => setEditing("new")}>Add {singular[kind]}</Button>}
          />
        ) : (
          <Table>
            <THead>
              <tr>
                <TH>Name</TH>
                {kind === "PLAN" && <TH>Runs for</TH>}
                {kind === "PLAN" && <TH>Paid in</TH>}
                {kind === "PRODUCT" && <TH>Stock</TH>}
                <TH className="text-right">Price</TH>
                <TH className="text-right">Tax</TH>
                <TH><span className="sr-only">Actions</span></TH>
              </tr>
            </THead>
            <tbody>
              {(offerings.data ?? []).map((o) => (
                <TR key={o.id} className={o.active ? undefined : "text-faint"}>
                  <TD>
                    <span className="font-medium">{o.name}</span>
                    {!o.active && <Pill className="ml-2">Inactive</Pill>}
                    {o.sku && <Tag className="ml-2">{o.sku}</Tag>}
                    {o.description && <span className="block text-[12px] text-muted">{o.description}</span>}
                  </TD>
                  {kind === "PLAN" && <TD className="text-muted">{durationLabel(o)}</TD>}
                  {kind === "PLAN" && <TD className="text-muted">{o.installments > 1 ? `${o.installments} monthly instalments` : "One payment"}</TD>}
                  {kind === "PRODUCT" && (
                    <TD className="tabular-nums">
                      {o.trackStock ? <span className={(o.stockQuantity ?? 0) <= 0 ? "font-medium text-bad" : ""}>{formatNumber(o.stockQuantity ?? 0)}</span> : <span className="text-faint">Not counted</span>}
                    </TD>
                  )}
                  <TD className="text-right tabular-nums">{formatMoney(o.price)}</TD>
                  <TD className="text-right tabular-nums text-muted">{o.taxRate ? `${o.taxRate}%` : "—"}</TD>
                  <TD className="w-10 text-right">
                    {canManage && (
                      <Menu trigger={<Button size="icon" variant="ghost" aria-label={`Actions for ${o.name}`}><Ellipsis className="h-4 w-4" /></Button>}>
                        <MenuItem onSelect={() => setEditing(o)}>Edit</MenuItem>
                        {o.trackStock && <MenuItem onSelect={() => setStock(o)}>Adjust stock</MenuItem>}
                        <MenuItem onSelect={() => toggleActive(o)}>{o.active ? "Deactivate" : "Activate"}</MenuItem>
                        <MenuItem danger onSelect={() => deleteOffering(o)}>Delete</MenuItem>
                      </Menu>
                    )}
                  </TD>
                </TR>
              ))}
            </tbody>
          </Table>
        )}
      </Panel>
      <Dialog
        open={editing != null}
        onOpenChange={(o) => !o && setEditing(null)}
        title={editing === "new" ? `Add a ${singular[kind]}` : editing ? `Edit ${editing.name}` : ""}
      >
        {editing && <OfferingForm kind={kind} offering={editing === "new" ? undefined : editing} onDone={() => setEditing(null)} />}
      </Dialog>
      <Dialog open={stock != null} onOpenChange={(o) => !o && setStock(null)} title={stock ? `Adjust stock of ${stock.name}` : ""} size="sm">
        {stock && <StockForm offering={stock} onDone={() => setStock(null)} />}
      </Dialog>
    </>
  );
}

function toRequest(o: Offering): Schemas["OfferingRequest"] {
  return {
    kind: o.kind,
    name: o.name,
    description: o.description ?? undefined,
    price: o.price,
    taxRate: o.taxRate,
    durationValue: o.durationValue ?? undefined,
    durationUnit: o.durationUnit ?? undefined,
    installments: o.installments,
    sku: o.sku ?? undefined,
    trackStock: o.trackStock,
    active: o.active,
  };
}

function OfferingForm({ kind, offering, onDone }: { kind: OfferingKind; offering?: Offering; onDone: () => void }) {
  const business = useBusiness();
  const save = useSaveOffering(offering?.id);
  const [values, setValues] = useState({
    name: offering?.name ?? "",
    description: offering?.description ?? "",
    price: offering ? String(offering.price) : "",
    taxRate: offering ? String(offering.taxRate) : "0",
    durationValue: offering?.durationValue ? String(offering.durationValue) : "1",
    durationUnit: offering?.durationUnit ?? "MONTHS",
    installments: offering ? String(offering.installments) : "1",
    sku: offering?.sku ?? "",
    trackStock: offering?.trackStock ?? false,
    initialStock: "",
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const set = (key: keyof typeof values) => (e: { target: { value: string } }) => setValues((v) => ({ ...v, [key]: e.target.value }));

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setErrors({});
    setFormError(null);
    const local: Record<string, string> = {};
    if (!values.name.trim()) local.name = "Enter a name";
    if (!/^\d+(\.\d{1,2})?$/.test(values.price)) local.price = "Enter a price like 1500";
    if (!/^\d+(\.\d{1,2})?$/.test(values.taxRate)) local.taxRate = "Enter a rate like 18";
    if (kind === "PLAN" && !/^\d+$/.test(values.durationValue)) local.durationValue = "Enter a whole number";
    if (Object.keys(local).length) {
      setErrors(local);
      return;
    }
    try {
      await save.mutateAsync({
        kind,
        name: values.name.trim(),
        description: values.description.trim() || undefined,
        price: Number(values.price),
        taxRate: Number(values.taxRate),
        durationValue: kind === "PLAN" ? Number(values.durationValue) : undefined,
        durationUnit: kind === "PLAN" ? (values.durationUnit as "DAYS" | "MONTHS") : undefined,
        installments: kind === "PLAN" ? Number(values.installments) : undefined,
        sku: values.sku.trim() || undefined,
        trackStock: kind === "PRODUCT" ? values.trackStock : undefined,
        initialStock: !offering && values.trackStock && values.initialStock ? Number(values.initialStock) : undefined,
        active: offering?.active ?? true,
      });
      toast.success(offering ? `Saved ${values.name.trim()}` : `Added ${values.name.trim()}`);
      onDone();
    } catch (error) {
      if (error instanceof ApiError && Object.keys(error.fieldErrors).length) {
        setErrors(error.fieldErrors);
      } else {
        setFormError(errorMessage(error));
      }
    }
  };

  return (
    <form onSubmit={submit} className="flex min-h-0 flex-1 flex-col" noValidate>
      <DialogBody className="space-y-4">
        <FormError message={formError} />
        <Field label="Name" error={errors.name}>
          <Input autoFocus value={values.name} onChange={set("name")} placeholder={kind === "PLAN" ? "Monthly" : ""} />
        </Field>
        <Field label="Description" optional>
          <Textarea rows={2} value={values.description} onChange={set("description")} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Price (before tax)" error={errors.price}>
            <Input inputMode="decimal" value={values.price} onChange={set("price")} />
          </Field>
          <Field label="Tax rate %" hint="0 if you don't charge GST" error={errors.taxRate}>
            <Input inputMode="decimal" value={values.taxRate} onChange={set("taxRate")} />
          </Field>
        </div>
        {kind === "PLAN" && (
          <div className="grid gap-4 sm:grid-cols-3">
            <Field label="Runs for" error={errors.durationValue}>
              <Input inputMode="numeric" value={values.durationValue} onChange={set("durationValue")} />
            </Field>
            <Field label="Unit">
              <Select value={values.durationUnit} onChange={set("durationUnit")}>
                <option value="MONTHS">Months</option>
                <option value="DAYS">Days</option>
              </Select>
            </Field>
            <Field label="Paid in" hint="Monthly instalments" error={errors.installments}>
              <Select value={values.installments} onChange={set("installments")}>
                {Array.from({ length: 12 }, (_, i) => i + 1).map((n) => (
                  <option key={n} value={n}>{n === 1 ? "One payment" : `${n} parts`}</option>
                ))}
              </Select>
            </Field>
          </div>
        )}
        {kind === "PRODUCT" && (
          <div className="space-y-4">
            <Field label="Code (SKU)" optional>
              <Input value={values.sku} onChange={set("sku")} />
            </Field>
            {business.has("INVENTORY") && (
              <>
                <Checkbox
                  label="Count stock"
                  hint="Sales take items out of stock and stop when it runs out"
                  checked={values.trackStock}
                  onChange={(e) => setValues((v) => ({ ...v, trackStock: e.target.checked }))}
                />
                {values.trackStock && !offering && (
                  <Field label="Starting stock" error={errors.initialStock} className="sm:w-1/2">
                    <Input inputMode="numeric" value={values.initialStock} onChange={set("initialStock")} />
                  </Field>
                )}
              </>
            )}
          </div>
        )}
      </DialogBody>
      <DialogFooter>
        <Button onClick={onDone}>Cancel</Button>
        <Button type="submit" variant="primary" loading={save.isPending}>{offering ? "Save" : "Add"}</Button>
      </DialogFooter>
    </form>
  );
}

function StockForm({ offering, onDone }: { offering: Offering; onDone: () => void }) {
  const adjust = useAdjustStock();
  const [change, setChange] = useState("");
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!/^-?\d+$/.test(change) || Number(change) === 0) {
      setError("Enter how many came in (like 20) or went out (like -3)");
      return;
    }
    try {
      await adjust.mutateAsync({ offeringId: offering.id, change: Number(change), reason: reason.trim() || undefined });
      toast.success(`Stock of ${offering.name} updated`);
      onDone();
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <form onSubmit={submit} className="flex min-h-0 flex-1 flex-col" noValidate>
      <DialogBody className="space-y-4">
        <FormError message={error} />
        <p className="text-[13px] text-muted">In stock now: <span className="font-semibold text-ink tabular-nums">{formatNumber(offering.stockQuantity ?? 0)}</span></p>
        <Field label="Change" hint="Positive for new stock, negative for damaged or lost items">
          <Input autoFocus inputMode="numeric" value={change} onChange={(e) => setChange(e.target.value)} />
        </Field>
        <Field label="Reason" optional>
          <Input value={reason} onChange={(e) => setReason(e.target.value)} />
        </Field>
      </DialogBody>
      <DialogFooter>
        <Button onClick={onDone}>Cancel</Button>
        <Button type="submit" variant="primary" loading={adjust.isPending}>Update stock</Button>
      </DialogFooter>
    </form>
  );
}
