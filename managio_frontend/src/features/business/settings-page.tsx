"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Copy } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState, useSyncExternalStore, type FormEvent } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Checkbox, Field, FormError, Input, Select } from "@/components/ui/field";
import { EmptyState, PageHeader, Panel } from "@/components/ui/layout";
import { ReasonDialog } from "@/components/ui/reason-dialog";
import { LoadingBlock } from "@/components/ui/spinner";
import { api, unwrap, type Schemas } from "@/lib/api/client";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { BUSINESS_TYPE_LABELS, MODULE_LABELS, type BusinessType, type ModuleKey } from "./access";
import { businessKey, myBusinessesKey, useBusiness } from "./context";

const TIME_ZONES = ["Asia/Kolkata", "Asia/Dubai", "Asia/Singapore", "Asia/Kathmandu", "Asia/Dhaka", "Europe/London", "America/New_York", "Australia/Sydney"];
const MODULES: ModuleKey[] = ["MEMBERSHIPS", "CATALOG", "INVENTORY", "GROUPS", "ATTENDANCE", "PAYROLL"];

/** Business settings (BUS-2 to BUS-4): details printed on invoices, which features are on, and portal options. */
export function SettingsPage() {
  const business = useBusiness();
  const details = useQuery({
    queryKey: businessKey(business.id, "settings"),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}", { params: { path: { businessId: business.id } } })),
  });

  if (details.isPending) return <LoadingBlock />;
  if (details.isError) return <EmptyState title="Settings couldn't load" body={errorMessage(details.error)} />;
  return (
    <>
      <PageHeader title="Settings" subtitle={`${details.data.name} · ${details.data.code}`} />
      <div className="grid gap-5 xl:grid-cols-[1.3fr_1fr]">
        <ProfileForm data={details.data} />
        <div className="space-y-5">
          <PortalBox data={details.data} />
          <FeaturesForm data={details.data} />
          {business.owner && <ArchiveBox name={details.data.name} />}
        </div>
      </div>
    </>
  );
}

function useInvalidateBusiness() {
  const queryClient = useQueryClient();
  const business = useBusiness();
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: businessKey(business.id) }),
      queryClient.invalidateQueries({ queryKey: myBusinessesKey }),
    ]);
}

function ProfileForm({ data }: { data: Schemas["BusinessResponse"] }) {
  const business = useBusiness();
  const invalidate = useInvalidateBusiness();
  const [values, setValues] = useState({
    name: data.name,
    type: data.type,
    phone: data.phone ?? "",
    email: data.email ?? "",
    addressLine: data.addressLine ?? "",
    city: data.city ?? "",
    state: data.state ?? "",
    postalCode: data.postalCode ?? "",
    country: data.country,
    gstin: data.gstin ?? "",
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const save = useMutation({
    mutationFn: (body: Schemas["UpdateBusinessRequest"]) =>
      unwrap(api.PATCH("/api/v1/businesses/{businessId}", { params: { path: { businessId: business.id } }, body })),
    onSuccess: invalidate,
  });
  const set = (key: keyof typeof values) => (e: { target: { value: string } }) => setValues((v) => ({ ...v, [key]: e.target.value }));

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setErrors({});
    setFormError(null);
    const optional = (value: string) => value.trim() || undefined;
    try {
      await save.mutateAsync({
        name: values.name.trim(),
        type: values.type,
        phone: optional(values.phone),
        email: optional(values.email),
        addressLine: optional(values.addressLine),
        city: optional(values.city),
        state: optional(values.state),
        postalCode: optional(values.postalCode),
        country: optional(values.country),
        gstin: optional(values.gstin.toUpperCase()),
      });
      toast.success("Business details saved");
    } catch (error) {
      if (error instanceof ApiError && Object.keys(error.fieldErrors).length) setErrors(error.fieldErrors);
      else setFormError(errorMessage(error));
    }
  };

  return (
    <Panel title="Business details" bodyClassName="px-5 py-4">
      <form onSubmit={submit} className="space-y-4" noValidate>
        <FormError message={formError} />
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Name" error={errors.name}>
            <Input value={values.name} onChange={set("name")} />
          </Field>
          <Field label="Kind of business" hint="Changes the words used, not your data">
            <Select value={values.type} onChange={set("type")}>
              {(Object.keys(BUSINESS_TYPE_LABELS) as BusinessType[]).map((t) => (
                <option key={t} value={t}>{BUSINESS_TYPE_LABELS[t]}</option>
              ))}
            </Select>
          </Field>
          <Field label="Phone" optional error={errors.phone}>
            <Input type="tel" value={values.phone} onChange={set("phone")} />
          </Field>
          <Field label="Email" optional error={errors.email}>
            <Input type="email" value={values.email} onChange={set("email")} />
          </Field>
        </div>
        <Field label="Address" optional error={errors.addressLine}>
          <Input value={values.addressLine} onChange={set("addressLine")} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-3">
          <Field label="City" optional error={errors.city}>
            <Input value={values.city} onChange={set("city")} />
          </Field>
          <Field label="State" optional error={errors.state}>
            <Input value={values.state} onChange={set("state")} />
          </Field>
          <Field label="PIN code" optional error={errors.postalCode}>
            <Input inputMode="numeric" value={values.postalCode} onChange={set("postalCode")} />
          </Field>
          <Field label="Country" error={errors.country}>
            <Input value={values.country} onChange={set("country")} />
          </Field>
          <Field label="GSTIN" optional hint="Printed on invoices" error={errors.gstin} className="sm:col-span-2">
            <Input value={values.gstin} onChange={set("gstin")} className="uppercase" maxLength={15} />
          </Field>
        </div>
        <Button type="submit" variant="primary" loading={save.isPending}>Save details</Button>
      </form>
    </Panel>
  );
}

function FeaturesForm({ data }: { data: Schemas["BusinessResponse"] }) {
  const business = useBusiness();
  const invalidate = useInvalidateBusiness();
  const [modules, setModules] = useState<Set<ModuleKey>>(new Set(data.modules));
  const [timezone, setTimezone] = useState(data.timezone);
  const [invoicePrefix, setInvoicePrefix] = useState(data.invoicePrefix);
  const [receiptPrefix, setReceiptPrefix] = useState(data.receiptPrefix);
  const [allowCustomerSignup, setAllowSignup] = useState(data.allowCustomerSignup);
  const [renewalReminders, setReminders] = useState(data.renewalReminders);
  const [error, setError] = useState<string | null>(null);
  const save = useMutation({
    mutationFn: (body: Schemas["UpdateSettingsRequest"]) =>
      unwrap(api.PUT("/api/v1/businesses/{businessId}/settings", { params: { path: { businessId: business.id } }, body })),
    onSuccess: invalidate,
  });

  const toggle = (module: ModuleKey) =>
    setModules((current) => {
      const next = new Set(current);
      if (next.has(module)) next.delete(module);
      else next.add(module);
      if (module === "CATALOG" && !next.has("CATALOG")) next.delete("INVENTORY");
      return next;
    });

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    try {
      await save.mutateAsync({
        modules: [...modules],
        timezone,
        invoicePrefix: invoicePrefix.trim().toUpperCase(),
        receiptPrefix: receiptPrefix.trim().toUpperCase(),
        allowCustomerSignup,
        renewalReminders,
      });
      toast.success("Settings saved");
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <Panel title="Features and numbering" bodyClassName="px-5 py-4">
      <form onSubmit={submit} className="space-y-5" noValidate>
        <FormError message={error} />
        <fieldset className="space-y-3">
          <legend className="mb-1 text-[13px] font-semibold text-ink">Turned on</legend>
          {MODULES.map((m) => (
            <Checkbox
              key={m}
              label={MODULE_LABELS[m].name}
              hint={MODULE_LABELS[m].description}
              checked={modules.has(m)}
              disabled={m === "INVENTORY" && !modules.has("CATALOG")}
              onChange={() => toggle(m)}
            />
          ))}
        </fieldset>
        <div className="grid gap-4 sm:grid-cols-3">
          <Field label="Time zone" className="sm:col-span-3">
            <Select value={timezone} onChange={(e) => setTimezone(e.target.value)}>
              {[...new Set([timezone, ...TIME_ZONES])].map((z) => (
                <option key={z} value={z}>{z.replace("_", " ")}</option>
              ))}
            </Select>
          </Field>
          <Field label="Invoice prefix" hint="INV-0042">
            <Input value={invoicePrefix} onChange={(e) => setInvoicePrefix(e.target.value)} maxLength={10} className="uppercase" />
          </Field>
          <Field label="Receipt prefix" hint="RCT-0108">
            <Input value={receiptPrefix} onChange={(e) => setReceiptPrefix(e.target.value)} maxLength={10} className="uppercase" />
          </Field>
        </div>
        <fieldset className="space-y-3">
          <legend className="mb-1 text-[13px] font-semibold text-ink">Customers</legend>
          <Checkbox
            label="Renewal reminder emails"
            hint="Customers with an email hear from you 3 days before their plan ends"
            checked={renewalReminders}
            onChange={(e) => setReminders(e.target.checked)}
          />
          <Checkbox
            label="Let customers sign themselves up"
            hint="Anyone with your business code can create a portal account. Staff invitations always work."
            checked={allowCustomerSignup}
            onChange={(e) => setAllowSignup(e.target.checked)}
          />
        </fieldset>
        <Button type="submit" variant="primary" loading={save.isPending}>Save settings</Button>
      </form>
    </Panel>
  );
}

const noSubscription = () => () => {};

function PortalBox({ data }: { data: Schemas["BusinessResponse"] }) {
  const origin = useSyncExternalStore(noSubscription, () => window.location.origin, () => "");
  const link = `${origin}/portal/${data.code}`;
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(link);
      toast.success("Link copied");
    } catch {
      toast.error("Couldn't copy. Select the link and copy it instead.");
    }
  };
  return (
    <Panel title="Customer portal" bodyClassName="px-5 py-4 space-y-3 text-[13.5px]">
      <p className="text-muted">
        Customers sign in with your business code <strong className="tabular-nums tracking-wide text-ink">{data.code}</strong> and see their plan, dues and receipts.
      </p>
      <div className="flex items-center gap-2">
        <Input readOnly value={link} aria-label="Portal link" onFocus={(e) => e.target.select()} />
        <Button size="icon" aria-label="Copy portal link" onClick={copy}>
          <Copy className="h-4 w-4" />
        </Button>
      </div>
    </Panel>
  );
}

function ArchiveBox({ name }: { name: string }) {
  const business = useBusiness();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  return (
    <Panel title="Archive this business" bodyClassName="px-5 py-4 space-y-3 text-[13.5px]">
      <p className="text-muted">Archiving hides {name} from everyone, including you. Its records are kept. Only the owner can do this.</p>
      <Button variant="danger-outline" onClick={() => setOpen(true)}>Archive {name}</Button>
      <ReasonDialog
        open={open}
        onOpenChange={setOpen}
        title={`Archive ${name}?`}
        description="Type the business name to confirm. Everyone loses access straight away."
        label="Business name"
        confirmLabel="Archive business"
        onConfirm={async (typed) => {
          if (typed.trim().toLowerCase() !== name.trim().toLowerCase()) {
            throw new Error(`Type "${name}" exactly to confirm`);
          }
          await unwrap(api.POST("/api/v1/businesses/{businessId}/archive", { params: { path: { businessId: business.id } } }));
          await queryClient.invalidateQueries({ queryKey: myBusinessesKey });
          toast.success(`${name} is archived`);
          router.replace("/app");
        }}
      />
    </Panel>
  );
}
