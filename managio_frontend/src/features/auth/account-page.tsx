"use client";

import { useMutation } from "@tanstack/react-query";
import { ArrowLeft, CircleCheck, MailWarning } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Field, FormError, Input } from "@/components/ui/field";
import { Panel } from "@/components/ui/layout";
import { api, staff, unwrap } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import { useStaffSession } from "@/lib/auth/use-session";
import { resendVerificationEmail, signOut } from "./api";
import { Logo } from "@/components/brand/logo";

/** Your own account (IDN-6): name and phone, password, email confirmation. It's the same in every business. */
export function AccountPage() {
  const router = useRouter();
  const session = useStaffSession();
  const user = session.principal;
  if (!user) return null;

  return (
    <div className="min-h-screen bg-canvas">
      <header className="border-b border-line bg-surface">
        <div className="mx-auto flex h-14 max-w-[880px] items-center justify-between px-4 sm:px-8">
          <button type="button" onClick={() => router.back()} className="inline-flex items-center gap-1.5 text-[13px] font-medium text-muted hover:text-ink">
            <ArrowLeft className="h-4 w-4" aria-hidden /> Back
          </button>
          <Logo size="sm" />
        </div>
      </header>
      <main className="mx-auto max-w-[880px] space-y-5 px-4 py-8 sm:px-8">
        <div>
          <h1 className="text-[24px] font-semibold">Your account</h1>
          <p className="text-[13px] text-muted">{user.email}</p>
        </div>
        {!user.emailVerified && <VerifyBanner />}
        <ProfileForm key={`${user.fullName}-${user.phone}`} fullName={user.fullName} phone={user.phone ?? ""} />
        <PasswordForm hasPassword={user.hasPassword} />
        <Panel title="Sign-in methods" bodyClassName="px-5 py-4 space-y-2 text-[13.5px]">
          <p className="flex items-center gap-2"><CircleCheck className="h-4 w-4 text-ok" aria-hidden /> Email {user.hasPassword ? "and password" : "(no password set yet)"}</p>
          <p className="flex items-center gap-2">
            {user.googleLinked ? <CircleCheck className="h-4 w-4 text-ok" aria-hidden /> : <span className="inline-block h-4 w-4 rounded-full border border-line-strong" aria-hidden />}
            Google {user.googleLinked ? "is linked" : "isn't linked. Sign in with Google once using this email to link it."}
          </p>
        </Panel>
        <Button
          variant="danger-outline"
          onClick={async () => {
            await signOut().catch(() => undefined);
            router.replace("/sign-in");
          }}
        >
          Sign out
        </Button>
      </main>
    </div>
  );
}

function VerifyBanner() {
  const resend = useMutation({ mutationFn: resendVerificationEmail });
  return (
    <div className="flex flex-wrap items-center gap-3 rounded-[var(--radius-panel)] border border-due/35 bg-due-soft px-4 py-3 text-[13.5px]">
      <MailWarning className="h-5 w-5 text-due" aria-hidden />
      <span className="flex-1">Confirm your email to invite your team. The link is in the email we sent when you signed up.</span>
      <Button size="sm" loading={resend.isPending} onClick={() => resend.mutate(undefined, { onSuccess: () => toast.success("Sent. Check your inbox."), onError: (e) => toast.error(errorMessage(e)) })}>
        Send it again
      </Button>
    </div>
  );
}

function ProfileForm({ fullName, phone }: { fullName: string; phone: string }) {
  const [values, setValues] = useState({ fullName, phone });
  const [error, setError] = useState<string | null>(null);
  const save = useMutation({
    mutationFn: () => unwrap(api.PATCH("/api/v1/me", { body: { fullName: values.fullName.trim(), phone: values.phone.trim() || undefined } })),
    onSuccess: (user) => staff.store.updatePrincipal(user),
  });
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    if (!values.fullName.trim()) {
      setError("Enter your name");
      return;
    }
    try {
      await save.mutateAsync();
      toast.success("Saved");
    } catch (e) {
      setError(errorMessage(e));
    }
  };
  return (
    <Panel title="Profile" bodyClassName="px-5 py-4">
      <form onSubmit={submit} className="space-y-4" noValidate>
        <FormError message={error} />
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Name">
            <Input value={values.fullName} onChange={(e) => setValues((v) => ({ ...v, fullName: e.target.value }))} />
          </Field>
          <Field label="Phone" optional>
            <Input type="tel" value={values.phone} onChange={(e) => setValues((v) => ({ ...v, phone: e.target.value }))} />
          </Field>
        </div>
        <Button type="submit" variant="primary" loading={save.isPending}>Save</Button>
      </form>
    </Panel>
  );
}

function PasswordForm({ hasPassword }: { hasPassword: boolean }) {
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [error, setError] = useState<string | null>(null);
  const change = useMutation({
    mutationFn: () => unwrap(api.POST("/api/v1/me/password", { body: { currentPassword: hasPassword ? current : undefined, newPassword: next } })),
    onSuccess: (session) => staff.accept(session),
  });
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    if (next.length < 8) {
      setError("Use at least 8 characters");
      return;
    }
    try {
      await change.mutateAsync();
      setCurrent("");
      setNext("");
      toast.success("Password changed. You've been signed out on your other devices.");
    } catch (e) {
      setError(errorMessage(e));
    }
  };
  return (
    <Panel title={hasPassword ? "Change password" : "Set a password"} bodyClassName="px-5 py-4">
      <form onSubmit={submit} className="space-y-4" noValidate>
        <FormError message={error} />
        <div className="grid gap-4 sm:grid-cols-2">
          {hasPassword && (
            <Field label="Current password">
              <Input type="password" autoComplete="current-password" value={current} onChange={(e) => setCurrent(e.target.value)} />
            </Field>
          )}
          <Field label="New password" hint="At least 8 characters">
            <Input type="password" autoComplete="new-password" value={next} onChange={(e) => setNext(e.target.value)} />
          </Field>
        </div>
        <Button type="submit" variant="primary" loading={change.isPending}>{hasPassword ? "Change password" : "Set password"}</Button>
      </form>
    </Panel>
  );
}
