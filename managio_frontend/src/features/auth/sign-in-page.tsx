"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { BadgeCheck, IdCard, Store, UserRound, type LucideIcon } from "lucide-react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Field, FormError, Input } from "@/components/ui/field";
import { portalSignIn } from "@/features/portal/api";
import { cn } from "@/lib/cn";
import { applyApiError } from "@/lib/forms";
import { signIn, signInWithGoogle } from "./api";
import { AuthFrame, AuthHeading } from "./auth-frame";
import { GoogleButton, googleEnabled } from "./google-button";
import { errorMessage } from "@/lib/api/errors";

type Who = "owner" | "staff" | "member";

const ROLES: { who: Who; title: string; caption: string; icon: LucideIcon }[] = [
  { who: "owner", title: "I run a business", caption: "Owner", icon: Store },
  { who: "staff", title: "I work here", caption: "Staff", icon: IdCard },
  { who: "member", title: "I'm a customer", caption: "Member, client, student", icon: UserRound },
];

const staffSchema = z.object({
  email: z.email("Enter a valid email"),
  password: z.string().min(1, "Enter your password"),
});

const memberSchema = z.object({
  businessCode: z.string().trim().min(1, "Enter the business code"),
  identifier: z.string().trim().min(1, "Enter your email or phone number"),
  password: z.string().min(1, "Enter your password"),
});

/** Where to go after signing in: only our own app pages or an invitation link, never another site. */
function safeNext(next: string | null) {
  const allowed = ["/app", "/admin", "/invite?"].some((prefix) => next?.startsWith(prefix));
  return next && allowed && !next.startsWith("//") ? next : "/app";
}

/** "Who's signing in?" (D-001, from Variant B): owners and staff use one account; customers use their business's portal. */
export function SignInPage() {
  const params = useSearchParams();
  const [who, setWho] = useState<Who>(params.get("as") === "member" ? "member" : "owner");

  return (
    <AuthFrame
      wide
      footer={
        who === "member" ? (
          <>Don&apos;t have a password yet? Ask the business to invite you, or use the link in your invitation email.</>
        ) : (
          <>
            Running a business? <Link href="/sign-up" className="font-semibold text-cobalt hover:underline">Create an account</Link>
          </>
        )
      }
    >
      <AuthHeading title="Who's signing in?" />
      <div role="radiogroup" aria-label="Who is signing in" className="grid gap-3 sm:grid-cols-3">
        {ROLES.map(({ who: value, title, caption, icon: Icon }) => {
          const active = who === value;
          return (
            <button
              key={value}
              type="button"
              role="radio"
              aria-checked={active}
              onClick={() => setWho(value)}
              className={cn(
                "relative rounded-xl border p-4 text-left transition-colors",
                active ? "border-cobalt bg-cobalt-soft/60 shadow-[0_0_0_1px_var(--color-cobalt)]" : "border-line hover:border-cobalt/40",
              )}
            >
              {active && <BadgeCheck className="absolute right-3 top-3 h-5 w-5 text-cobalt" aria-hidden />}
              <Icon className="h-6 w-6 text-cobalt" aria-hidden />
              <div className="mt-3 text-[15px] font-semibold text-ink">{title}</div>
              <div className="text-[13px] text-muted">{caption}</div>
            </button>
          );
        })}
      </div>
      <div className="mt-7">{who === "member" ? <MemberForm /> : <StaffForm who={who} next={safeNext(params.get("next"))} />}</div>
    </AuthFrame>
  );
}

function StaffForm({ who, next }: { who: Who; next: string }) {
  const router = useRouter();
  const [formError, setFormError] = useState<string | null>(null);
  const { register, handleSubmit, formState, setError } = useForm<z.infer<typeof staffSchema>>({ resolver: zodResolver(staffSchema) });

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await signIn(values);
      router.replace(next);
    } catch (error) {
      setFormError(applyApiError(error, setError, ["email", "password"]));
    }
  });

  const google = async (credential: string) => {
    setFormError(null);
    try {
      await signInWithGoogle(credential);
      router.replace(next);
    } catch (error) {
      setFormError(errorMessage(error));
    }
  };

  return (
    <form onSubmit={submit} className="space-y-4" noValidate>
      <FormError message={formError} />
      {who === "staff" && (
        <p className="text-[13.5px] text-muted">Use the email your invitation was sent to. One account works for every business you&apos;re part of.</p>
      )}
      <Field label="Email" error={formState.errors.email?.message}>
        <Input type="email" autoComplete="email" className="h-11 text-[15px]" {...register("email")} />
      </Field>
      <div>
        <div className="mb-1.5 flex items-baseline justify-between">
          <label htmlFor="password" className="text-[13px] font-medium text-ink">Password</label>
          <Link href="/forgot-password" className="text-[13px] font-semibold text-cobalt hover:underline">Forgot password?</Link>
        </div>
        <Input id="password" type="password" autoComplete="current-password" className="h-11 text-[15px]"
          aria-invalid={formState.errors.password ? true : undefined} {...register("password")} />
        {formState.errors.password && <p className="mt-1.5 text-[12.5px] font-medium text-bad">{formState.errors.password.message}</p>}
      </div>
      <Button type="submit" variant="primary" size="lg" className="w-full justify-center" loading={formState.isSubmitting}>
        Sign in
      </Button>
      {googleEnabled && (
        <>
          <div className="flex items-center gap-3 text-[12px] text-faint">
            <span className="h-px flex-1 bg-line" /> or <span className="h-px flex-1 bg-line" />
          </div>
          <GoogleButton onCredential={google} text="signin_with" />
        </>
      )}
    </form>
  );
}

function MemberForm() {
  const router = useRouter();
  const [formError, setFormError] = useState<string | null>(null);
  const { register, handleSubmit, formState, setError, getValues } = useForm<z.infer<typeof memberSchema>>({ resolver: zodResolver(memberSchema) });

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      const session = await portalSignIn({ ...values, businessCode: values.businessCode.toUpperCase() });
      router.replace(`/portal/${session.business.code}`);
    } catch (error) {
      setFormError(applyApiError(error, setError, ["businessCode", "identifier", "password"]));
    }
  });

  return (
    <form onSubmit={submit} className="space-y-4" noValidate>
      <FormError message={formError} />
      <Field label="Business code" hint="Eight characters, like 4821IRWD. It's on your invitation email and at the front desk." error={formState.errors.businessCode?.message}>
        <Input autoComplete="organization" className="h-11 text-[15px] uppercase tracking-wide tabular-nums" {...register("businessCode")} />
      </Field>
      <Field label="Email or phone" error={formState.errors.identifier?.message}>
        <Input autoComplete="username" className="h-11 text-[15px]" {...register("identifier")} />
      </Field>
      <div>
        <div className="mb-1.5 flex items-baseline justify-between">
          <label htmlFor="member-password" className="text-[13px] font-medium text-ink">Password</label>
          <button
            type="button"
            className="text-[13px] font-semibold text-cobalt hover:underline"
            onClick={() => {
              const code = getValues("businessCode")?.trim().toUpperCase();
              router.push(code ? `/portal/${code}?reset=1` : "/portal");
            }}
          >
            Forgot password?
          </button>
        </div>
        <Input id="member-password" type="password" autoComplete="current-password" className="h-11 text-[15px]"
          aria-invalid={formState.errors.password ? true : undefined} {...register("password")} />
        {formState.errors.password && <p className="mt-1.5 text-[12.5px] font-medium text-bad">{formState.errors.password.message}</p>}
      </div>
      <Button type="submit" variant="primary" size="lg" className="w-full justify-center" loading={formState.isSubmitting}>
        Sign in
      </Button>
    </form>
  );
}
