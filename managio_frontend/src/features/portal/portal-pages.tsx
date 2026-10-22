"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { CircleCheck, LogOut, MailCheck } from "lucide-react";
import Link from "next/link";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { useState, type FormEvent } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Field, FormError, Input } from "@/components/ui/field";
import { EmptyState, Panel, Skeleton, Table, TD, TH, THead, TR } from "@/components/ui/layout";
import { Pagination } from "@/components/ui/pagination";
import { Pill } from "@/components/ui/pill";
import { LoadingBlock } from "@/components/ui/spinner";
import { AgreementNote, AuthFrame, AuthHeading } from "@/features/auth/auth-frame";
import { INVOICE_STATE, methodLabel } from "@/features/billing/api";
import { MEMBERSHIP_STATE, type MembershipState } from "@/features/customers/api";
import { MEMBERSHIP_ITEM_STATE, type MembershipItemState } from "@/features/memberships/api";
import { portalApi, unwrap } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import { usePortalSession } from "@/lib/auth/use-session";
import { applyApiError } from "@/lib/forms";
import { formatDate, formatDateTime, formatMoney } from "@/lib/format";
import { businessPreview, portalChangePassword, portalRequestReset, portalSetPassword, portalSignIn, portalSignOut, portalSignUp } from "./api";

/* ── /portal: ask for the business code ───────────────────────────────────────────────── */

export function PortalCodePage() {
  const router = useRouter();
  const [code, setCode] = useState("");
  return (
    <AuthFrame>
      <AuthHeading title="Your account with a business" subtitle="Enter the business code. It's on your invitation email, or ask at the front desk." />
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (code.trim()) router.push(`/portal/${code.trim().toUpperCase()}`);
        }}
        className="space-y-4"
      >
        <Field label="Business code">
          <Input autoFocus value={code} onChange={(e) => setCode(e.target.value)} className="h-11 text-[15px] uppercase tracking-wide" />
        </Field>
        <Button type="submit" variant="primary" size="lg" className="w-full justify-center">Continue</Button>
      </form>
    </AuthFrame>
  );
}

/* ── /portal/[code]: sign in or the customer's home ───────────────────────────────────── */

export function PortalPage() {
  const { code } = useParams<{ code: string }>();
  const session = usePortalSession();
  const upper = code.toUpperCase();
  if (session.status === "unknown") return <div className="grid min-h-screen place-items-center"><LoadingBlock /></div>;
  if (session.status === "signed-in" && session.principal?.business.code === upper) return <PortalHome />;
  return <PortalSignIn code={upper} />;
}

const signInSchema = z.object({
  identifier: z.string().trim().min(1, "Enter your email or phone number"),
  password: z.string().min(1, "Enter your password"),
});
const signUpSchema = z.object({
  fullName: z.string().trim().min(1, "Enter your name"),
  email: z.union([z.literal(""), z.email("Enter a valid email")]),
  phone: z.string().trim().max(20),
  password: z.string().min(8, "Use at least 8 characters").max(72),
}).refine((v) => v.email || v.phone, { message: "Enter an email or a phone number", path: ["email"] });

function PortalSignIn({ code }: { code: string }) {
  const params = useSearchParams();
  const preview = useQuery({ queryKey: ["portal", "business", code], queryFn: () => businessPreview(code), retry: false });
  const [mode, setMode] = useState<"sign-in" | "sign-up" | "reset">(params.get("reset") ? "reset" : "sign-in");

  if (preview.isPending) return <AuthFrame><LoadingBlock /></AuthFrame>;
  if (preview.isError) {
    return (
      <AuthFrame footer={<Link href="/portal" className="font-semibold text-cobalt hover:underline">Try another code</Link>}>
        <AuthHeading title="We couldn't find that business" subtitle={`There's no business with the code ${code}. Check the code and try again.`} />
      </AuthFrame>
    );
  }
  const business = preview.data;
  return (
    <AuthFrame
      footer={
        mode === "sign-in" ? (
          business.signUpOpen ? (
            <>New here? <button type="button" className="font-semibold text-cobalt hover:underline" onClick={() => setMode("sign-up")}>Create your account</button></>
          ) : (
            <>No password yet? Ask {business.name} to invite you.</>
          )
        ) : (
          <button type="button" className="font-semibold text-cobalt hover:underline" onClick={() => setMode("sign-in")}>Back to sign in</button>
        )
      }
    >
      <AuthHeading
        title={mode === "sign-up" ? `Join ${business.name}` : mode === "reset" ? "Reset your password" : business.name}
        subtitle={mode === "sign-in" ? "See your plan, what you owe and your receipts." : undefined}
      />
      {mode === "sign-in" && <SignInForm code={code} onForgot={() => setMode("reset")} />}
      {mode === "sign-up" && <SignUpForm code={code} />}
      {mode === "reset" && <ResetForm code={code} />}
    </AuthFrame>
  );
}

function SignInForm({ code, onForgot }: { code: string; onForgot: () => void }) {
  const [formError, setFormError] = useState<string | null>(null);
  const { register, handleSubmit, formState, setError } = useForm<z.infer<typeof signInSchema>>({ resolver: zodResolver(signInSchema) });
  const submit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await portalSignIn({ businessCode: code, ...values });
    } catch (error) {
      setFormError(applyApiError(error, setError, ["identifier", "password"]));
    }
  });
  return (
    <form onSubmit={submit} className="space-y-4" noValidate>
      <FormError message={formError} />
      <Field label="Email or phone" error={formState.errors.identifier?.message}>
        <Input autoComplete="username" className="h-11 text-[15px]" {...register("identifier")} />
      </Field>
      <div>
        <div className="mb-1.5 flex items-baseline justify-between">
          <label htmlFor="portal-password" className="text-[13px] font-medium text-ink">Password</label>
          <button type="button" onClick={onForgot} className="text-[13px] font-semibold text-cobalt hover:underline">Forgot password?</button>
        </div>
        <Input id="portal-password" type="password" autoComplete="current-password" className="h-11 text-[15px]" aria-invalid={formState.errors.password ? true : undefined} {...register("password")} />
        {formState.errors.password && <p className="mt-1.5 text-[12.5px] font-medium text-bad">{formState.errors.password.message}</p>}
      </div>
      <Button type="submit" variant="primary" size="lg" className="w-full justify-center" loading={formState.isSubmitting}>Sign in</Button>
    </form>
  );
}

function SignUpForm({ code }: { code: string }) {
  const [formError, setFormError] = useState<string | null>(null);
  const { register, handleSubmit, formState, setError } = useForm<z.infer<typeof signUpSchema>>({
    resolver: zodResolver(signUpSchema),
    defaultValues: { fullName: "", email: "", phone: "", password: "" },
  });
  const submit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await portalSignUp({ businessCode: code, fullName: values.fullName, email: values.email || undefined, phone: values.phone || undefined, password: values.password });
    } catch (error) {
      setFormError(applyApiError(error, setError, ["fullName", "email", "phone", "password"]));
    }
  });
  return (
    <form onSubmit={submit} className="space-y-4" noValidate>
      <FormError message={formError} />
      <Field label="Your name" error={formState.errors.fullName?.message}>
        <Input autoComplete="name" className="h-11 text-[15px]" {...register("fullName")} />
      </Field>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Email" error={formState.errors.email?.message}>
          <Input type="email" autoComplete="email" className="h-11 text-[15px]" {...register("email")} />
        </Field>
        <Field label="Phone" optional error={formState.errors.phone?.message}>
          <Input type="tel" autoComplete="tel" className="h-11 text-[15px]" {...register("phone")} />
        </Field>
      </div>
      <Field label="Password" hint="At least 8 characters" error={formState.errors.password?.message}>
        <Input type="password" autoComplete="new-password" className="h-11 text-[15px]" {...register("password")} />
      </Field>
      <Button type="submit" variant="primary" size="lg" className="w-full justify-center" loading={formState.isSubmitting}>Create account</Button>
      <AgreementNote />
    </form>
  );
}

function ResetForm({ code }: { code: string }) {
  const [identifier, setIdentifier] = useState("");
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!identifier.trim()) return;
    setBusy(true);
    setError(null);
    try {
      await portalRequestReset(code, identifier.trim());
      setSent(true);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };
  if (sent) {
    return (
      <div className="text-center">
        <MailCheck className="mx-auto h-10 w-10 text-ok" aria-hidden />
        <p className="mt-3 text-[14px] text-muted">If that matches an account with an email, a reset link is on its way. It works for 1 hour.</p>
      </div>
    );
  }
  return (
    <form onSubmit={submit} className="space-y-4" noValidate>
      <FormError message={error} />
      <Field label="Your email" hint="We'll send a link to choose a new password">
        <Input type="email" autoFocus className="h-11 text-[15px]" value={identifier} onChange={(e) => setIdentifier(e.target.value)} />
      </Field>
      <Button type="submit" variant="primary" size="lg" className="w-full justify-center" loading={busy}>Send reset link</Button>
    </form>
  );
}

/* ── /portal/[code]/set-password ──────────────────────────────────────────────────────── */

export function PortalSetPasswordPage() {
  const { code } = useParams<{ code: string }>();
  const params = useSearchParams();
  const router = useRouter();
  const token = params.get("token") ?? "";
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (password.length < 8) {
      setError("Use at least 8 characters");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const session = await portalSetPassword(token, password);
      router.replace(`/portal/${session.business.code}`);
    } catch (e) {
      setError(errorMessage(e));
      setBusy(false);
    }
  };

  return (
    <AuthFrame footer={<Link href={`/portal/${code}`} className="font-semibold text-cobalt hover:underline">Go to sign in</Link>}>
      {!token ? (
        <AuthHeading title="This link is incomplete" subtitle="Open the link from your email again." />
      ) : (
        <>
          <AuthHeading title="Choose your password" subtitle="You'll use it with your email or phone to sign in." />
          <form onSubmit={submit} className="space-y-4" noValidate>
            <FormError message={error} />
            <Field label="Password" hint="At least 8 characters">
              <Input type="password" autoFocus autoComplete="new-password" className="h-11 text-[15px]" value={password} onChange={(e) => setPassword(e.target.value)} />
            </Field>
            <Button type="submit" variant="primary" size="lg" className="w-full justify-center" loading={busy}>Save and sign in</Button>
          </form>
        </>
      )}
    </AuthFrame>
  );
}

/* ── The customer's home (POR-3) ──────────────────────────────────────────────────────── */

function PortalHome() {
  const router = useRouter();
  const me = useQuery({ queryKey: ["portal", "me"], queryFn: () => unwrap(portalApi.GET("/api/v1/portal/me")) });
  const memberships = useQuery({ queryKey: ["portal", "memberships"], queryFn: () => unwrap(portalApi.GET("/api/v1/portal/memberships")) });
  const [invoicePage, setInvoicePage] = useState(0);
  const [paymentPage, setPaymentPage] = useState(0);
  const invoices = useQuery({
    queryKey: ["portal", "invoices", invoicePage],
    queryFn: () => unwrap(portalApi.GET("/api/v1/portal/invoices", { params: { query: { page: invoicePage, size: 10 } } })),
    placeholderData: keepPreviousData,
  });
  const payments = useQuery({
    queryKey: ["portal", "payments", paymentPage],
    queryFn: () => unwrap(portalApi.GET("/api/v1/portal/payments", { params: { query: { page: paymentPage, size: 10 } } })),
    placeholderData: keepPreviousData,
  });

  if (me.isPending) return <div className="grid min-h-screen place-items-center"><LoadingBlock /></div>;
  if (me.isError) return <div className="grid min-h-screen place-items-center px-4"><EmptyState title="Your account couldn't load" body={errorMessage(me.error)} /></div>;
  const profile = me.data;
  const state = MEMBERSHIP_STATE[profile.membershipState as MembershipState] ?? MEMBERSHIP_STATE.NONE;

  return (
    <div className="min-h-screen bg-canvas">
      <header className="border-b border-line bg-surface">
        <div className="mx-auto flex h-14 max-w-[960px] items-center justify-between gap-4 px-4 sm:px-8">
          <span className="truncate text-[15px] font-semibold text-ink">{profile.business.name}</span>
          <Button
            size="sm"
            variant="ghost"
            icon={<LogOut className="h-4 w-4" />}
            onClick={async () => {
              await portalSignOut().catch(() => undefined);
              router.replace(`/portal/${profile.business.code}`);
            }}
          >
            Sign out
          </Button>
        </div>
      </header>
      <main className="mx-auto max-w-[960px] space-y-5 px-4 py-8 sm:px-8">
        <div>
          <h1 className="text-[24px] font-semibold">Hello, {profile.customer.fullName.split(" ")[0]}</h1>
          <p className="text-[13px] text-muted">Member #{profile.customer.number}</p>
        </div>
        <section className="grid gap-px overflow-hidden rounded-[var(--radius-panel)] border border-line bg-line sm:grid-cols-3" aria-label="Your standing">
          <div className="bg-surface px-5 py-4">
            <p className="text-[12.5px] text-muted">Your plan</p>
            <p className="mt-1.5"><Pill tone={state.tone}>{state.label}</Pill></p>
            {profile.coveredUntil && <p className="mt-1 text-[13px] text-muted">Covered until {formatDate(profile.coveredUntil)}</p>}
          </div>
          <div className="bg-surface px-5 py-4">
            <p className="text-[12.5px] text-muted">You owe</p>
            <p className={profile.balanceDue > 0 ? "mt-1 text-[24px] font-semibold tabular-nums text-bad" : "mt-1 text-[24px] font-semibold tabular-nums"}>{formatMoney(profile.balanceDue)}</p>
            {profile.balanceDue > 0 && <p className="text-[12.5px] text-muted">Pay at the front desk or as you usually do</p>}
          </div>
          <div className="bg-surface px-5 py-4">
            <p className="text-[12.5px] text-muted">Coming up</p>
            <p className="mt-1 text-[24px] font-semibold tabular-nums">{formatMoney(profile.upcoming)}</p>
            <p className="text-[12.5px] text-muted">Later instalments</p>
          </div>
        </section>

        <Panel title="Your plans">
          {memberships.isPending ? (
            <Skeleton className="m-5 h-16" />
          ) : (memberships.data ?? []).length === 0 ? (
            <p className="px-5 py-6 text-[13px] text-muted">No plans yet.</p>
          ) : (
            <ul className="divide-y divide-line">
              {(memberships.data ?? []).map((m) => {
                const s = MEMBERSHIP_ITEM_STATE[m.state as MembershipItemState];
                return (
                  <li key={m.id} className="flex items-center justify-between gap-3 px-5 py-3 text-[13.5px]">
                    <span>
                      <span className="block font-medium">{m.planName}</span>
                      <span className="tabular-nums text-muted">{formatDate(m.startsOn)} – {formatDate(m.endsOn)} · {formatMoney(m.price)}</span>
                    </span>
                    {s && <Pill tone={s.tone}>{s.label}</Pill>}
                  </li>
                );
              })}
            </ul>
          )}
        </Panel>

        <div className="grid gap-5 lg:grid-cols-2">
          <Panel title="Invoices">
            {(invoices.data?.items ?? []).length === 0 ? (
              <p className="px-5 py-6 text-[13px] text-muted">No invoices.</p>
            ) : (
              <>
                <Table>
                  <THead>
                    <tr>
                      <TH>Invoice</TH>
                      <TH>Due</TH>
                      <TH className="text-right">Owed</TH>
                      <TH>Status</TH>
                    </tr>
                  </THead>
                  <tbody>
                    {(invoices.data?.items ?? []).map((i) => (
                      <TR key={i.id}>
                        <TD className="tabular-nums">{i.invoiceNumber}<span className="block text-[12px] text-muted">{formatMoney(i.total)}</span></TD>
                        <TD className="tabular-nums text-muted">{formatDate(i.dueOn)}</TD>
                        <TD className="text-right tabular-nums">{i.balance > 0 ? formatMoney(i.balance) : "—"}</TD>
                        <TD><Pill tone={INVOICE_STATE[i.state].tone}>{INVOICE_STATE[i.state].label}</Pill></TD>
                      </TR>
                    ))}
                  </tbody>
                </Table>
                <Pagination page={invoicePage} size={10} total={invoices.data?.totalItems ?? 0} onPage={setInvoicePage} />
              </>
            )}
          </Panel>
          <Panel title="Receipts">
            {(payments.data?.items ?? []).length === 0 ? (
              <p className="px-5 py-6 text-[13px] text-muted">No payments yet.</p>
            ) : (
              <>
                <ul className="divide-y divide-line">
                  {(payments.data?.items ?? []).map((p) => (
                    <li key={p.id} className="flex items-center justify-between gap-3 px-5 py-3 text-[13.5px]">
                      <span>
                        <span className="block font-medium tabular-nums">{p.kind === "REFUND" ? "Refund " : ""}{p.receiptNumber}</span>
                        <span className="text-[12px] text-muted">{formatDateTime(p.receivedAt)} · {methodLabel(p.method)} · for {p.invoiceNumber}</span>
                      </span>
                      <span className="font-medium tabular-nums">{p.kind === "REFUND" ? "−" : ""}{formatMoney(p.amount)}</span>
                    </li>
                  ))}
                </ul>
                <Pagination page={paymentPage} size={10} total={payments.data?.totalItems ?? 0} onPage={setPaymentPage} />
              </>
            )}
          </Panel>
        </div>

        <ChangePassword />
      </main>
    </div>
  );
}

function ChangePassword() {
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    if (next.length < 8) {
      setError("Use at least 8 characters for the new password");
      return;
    }
    setBusy(true);
    try {
      await portalChangePassword({ currentPassword: current, newPassword: next });
      setCurrent("");
      setNext("");
      toast.success("Password changed", { icon: <CircleCheck className="h-4 w-4 text-ok" /> });
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };
  return (
    <Panel title="Change password" bodyClassName="px-5 py-4">
      <form onSubmit={submit} className="space-y-4" noValidate>
        <FormError message={error} />
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Current password">
            <Input type="password" autoComplete="current-password" value={current} onChange={(e) => setCurrent(e.target.value)} />
          </Field>
          <Field label="New password">
            <Input type="password" autoComplete="new-password" value={next} onChange={(e) => setNext(e.target.value)} />
          </Field>
        </div>
        <Button type="submit" loading={busy}>Change password</Button>
      </form>
    </Panel>
  );
}
