"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery } from "@tanstack/react-query";
import { CircleCheck, MailCheck } from "lucide-react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Field, FormError, Input } from "@/components/ui/field";
import { LoadingBlock } from "@/components/ui/spinner";
import { useStaffSession } from "@/lib/auth/use-session";
import { errorMessage } from "@/lib/api/errors";
import { applyApiError } from "@/lib/forms";
import { formatDate } from "@/lib/format";
import { acceptInvitation, confirmPasswordReset, previewInvitation, requestPasswordReset, signInWithGoogle, signUp, verifyEmail } from "./api";
import { AgreementNote, AuthFrame, AuthHeading } from "./auth-frame";
import { GoogleButton, googleEnabled } from "./google-button";

const password = z.string().min(8, "Use at least 8 characters").max(72, "Use 72 characters or fewer");

/* ── Sign up (IDN-1) ─────────────────────────────────────────────────────────────────── */

const signUpSchema = z.object({
  fullName: z.string().trim().min(1, "Enter your name").max(150),
  email: z.email("Enter a valid email"),
  password,
});

export function SignUpPage() {
  const router = useRouter();
  const [formError, setFormError] = useState<string | null>(null);
  const { register, handleSubmit, formState, setError } = useForm<z.infer<typeof signUpSchema>>({ resolver: zodResolver(signUpSchema) });

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await signUp(values);
      router.replace("/app/new");
    } catch (error) {
      setFormError(applyApiError(error, setError, ["fullName", "email", "password"]));
    }
  });

  const google = async (credential: string) => {
    setFormError(null);
    try {
      await signInWithGoogle(credential);
      router.replace("/app");
    } catch (error) {
      setFormError(errorMessage(error));
    }
  };

  return (
    <AuthFrame footer={<>Already have an account? <Link href="/sign-in" className="font-semibold text-cobalt hover:underline">Sign in</Link></>}>
      <AuthHeading title="Create your account" subtitle="One account for every business you run. You'll set up your first business next." />
      <form onSubmit={submit} className="space-y-4" noValidate>
        <FormError message={formError} />
        <Field label="Your name" error={formState.errors.fullName?.message}>
          <Input autoComplete="name" className="h-11 text-[15px]" {...register("fullName")} />
        </Field>
        <Field label="Email" error={formState.errors.email?.message}>
          <Input type="email" autoComplete="email" className="h-11 text-[15px]" {...register("email")} />
        </Field>
        <Field label="Password" hint="At least 8 characters" error={formState.errors.password?.message}>
          <Input type="password" autoComplete="new-password" className="h-11 text-[15px]" {...register("password")} />
        </Field>
        <Button type="submit" variant="primary" size="lg" className="w-full justify-center" loading={formState.isSubmitting}>
          Create account
        </Button>
        {googleEnabled && (
          <>
            <div className="flex items-center gap-3 text-[12px] text-faint">
              <span className="h-px flex-1 bg-line" /> or <span className="h-px flex-1 bg-line" />
            </div>
            <GoogleButton onCredential={google} text="signup_with" />
          </>
        )}
        <AgreementNote />
      </form>
    </AuthFrame>
  );
}

/* ── Forgot and reset password (IDN-4) ───────────────────────────────────────────────── */

export function ForgotPasswordPage() {
  const [sentTo, setSentTo] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const { register, handleSubmit, formState } = useForm<{ email: string }>({ resolver: zodResolver(z.object({ email: z.email("Enter a valid email") })) });

  const submit = handleSubmit(async ({ email }) => {
    setFormError(null);
    try {
      await requestPasswordReset(email);
      setSentTo(email);
    } catch (error) {
      setFormError(errorMessage(error));
    }
  });

  return (
    <AuthFrame footer={<Link href="/sign-in" className="font-semibold text-cobalt hover:underline">Back to sign in</Link>}>
      {sentTo ? (
        <div className="text-center">
          <MailCheck className="mx-auto h-10 w-10 text-ok" aria-hidden />
          <AuthHeading title="Check your email" subtitle={`If ${sentTo} has a Managio account, a reset link is on its way. It works for 1 hour.`} />
        </div>
      ) : (
        <>
          <AuthHeading title="Reset your password" subtitle="Enter the email you sign in with and we'll send you a link." />
          <form onSubmit={submit} className="space-y-4" noValidate>
            <FormError message={formError} />
            <Field label="Email" error={formState.errors.email?.message}>
              <Input type="email" autoComplete="email" className="h-11 text-[15px]" {...register("email")} />
            </Field>
            <Button type="submit" variant="primary" size="lg" className="w-full justify-center" loading={formState.isSubmitting}>
              Send reset link
            </Button>
          </form>
        </>
      )}
    </AuthFrame>
  );
}

export function ResetPasswordPage() {
  const params = useSearchParams();
  const token = params.get("token") ?? "";
  const [done, setDone] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const { register, handleSubmit, formState, setError } = useForm<{ password: string }>({ resolver: zodResolver(z.object({ password })) });

  const submit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await confirmPasswordReset(token, values.password);
      setDone(true);
    } catch (error) {
      setFormError(applyApiError(error, setError, ["password"]));
    }
  });

  return (
    <AuthFrame>
      {done ? (
        <div className="text-center">
          <CircleCheck className="mx-auto h-10 w-10 text-ok" aria-hidden />
          <AuthHeading title="Password changed" subtitle="You've been signed out everywhere else. Sign in with your new password." />
          <Button asChild variant="primary" size="lg"><Link href="/sign-in">Sign in</Link></Button>
        </div>
      ) : !token ? (
        <AuthHeading title="This link is incomplete" subtitle="Open the link from your email again, or request a new one." />
      ) : (
        <>
          <AuthHeading title="Choose a new password" />
          <form onSubmit={submit} className="space-y-4" noValidate>
            <FormError message={formError} />
            <Field label="New password" hint="At least 8 characters" error={formState.errors.password?.message}>
              <Input type="password" autoComplete="new-password" className="h-11 text-[15px]" {...register("password")} />
            </Field>
            <Button type="submit" variant="primary" size="lg" className="w-full justify-center" loading={formState.isSubmitting}>
              Save password
            </Button>
          </form>
        </>
      )}
    </AuthFrame>
  );
}

/* ── Email verification (IDN-1) ───────────────────────────────────────────────────────── */

export function VerifyEmailPage() {
  const params = useSearchParams();
  const token = params.get("token") ?? "";
  const verify = useMutation({ mutationFn: () => verifyEmail(token) });
  const started = useRef(false);

  useEffect(() => {
    if (token && !started.current) {
      started.current = true;
      verify.mutate();
    }
  }, [token, verify]);

  return (
    <AuthFrame>
      {!token ? (
        <AuthHeading title="This link is incomplete" subtitle="Open the link from your email again." />
      ) : verify.isPending || verify.isIdle ? (
        <LoadingBlock label="Confirming your email" />
      ) : verify.isError ? (
        <AuthHeading title="That link didn't work" subtitle={errorMessage(verify.error)} />
      ) : (
        <div className="text-center">
          <CircleCheck className="mx-auto h-10 w-10 text-ok" aria-hidden />
          <AuthHeading title="Email confirmed" subtitle={`Thanks, ${verify.data.fullName.split(" ")[0]}. You're all set.`} />
          <Button asChild variant="primary" size="lg"><Link href="/app">Go to Managio</Link></Button>
        </div>
      )}
    </AuthFrame>
  );
}

/* ── Invitations (TEAM-5, IDN-8) ──────────────────────────────────────────────────────── */

const acceptSchema = z.object({ fullName: z.string().trim().min(1, "Enter your name").max(150), password });

export function InvitationPage() {
  const params = useSearchParams();
  const router = useRouter();
  const token = params.get("token") ?? "";
  const session = useStaffSession();
  const preview = useQuery({ queryKey: ["invitation", token], queryFn: () => previewInvitation(token), enabled: Boolean(token), retry: false });
  const [formError, setFormError] = useState<string | null>(null);
  const { register, handleSubmit, formState, setError } = useForm<z.infer<typeof acceptSchema>>({ resolver: zodResolver(acceptSchema) });

  const finish = (businessId: number) => router.replace(`/app/${businessId}`);

  const createAndAccept = handleSubmit(async (values) => {
    setFormError(null);
    try {
      finish((await acceptInvitation({ token, ...values })).businessId);
    } catch (error) {
      setFormError(applyApiError(error, setError, ["fullName", "password"]));
    }
  });

  const [accepting, setAccepting] = useState(false);
  const acceptSignedIn = async () => {
    if (accepting) return;
    setFormError(null);
    setAccepting(true);
    try {
      finish((await acceptInvitation({ token })).businessId);
    } catch (error) {
      setFormError(errorMessage(error));
      setAccepting(false);
    }
  };

  const acceptWithGoogle = async (googleCredential: string) => {
    setFormError(null);
    try {
      finish((await acceptInvitation({ token, googleCredential })).businessId);
    } catch (error) {
      setFormError(errorMessage(error));
    }
  };

  if (!token) {
    return <AuthFrame><AuthHeading title="This link is incomplete" subtitle="Open the invitation link from your email again." /></AuthFrame>;
  }
  if (preview.isPending) {
    return <AuthFrame><LoadingBlock label="Opening your invitation" /></AuthFrame>;
  }
  if (preview.isError) {
    return (
      <AuthFrame>
        <AuthHeading title="This invitation can't be used" subtitle={`${errorMessage(preview.error)} Ask whoever invited you to send a new one.`} />
      </AuthFrame>
    );
  }

  const invite = preview.data;
  const signedInAsInvitee = session.status === "signed-in" && session.principal?.email.toLowerCase() === invite.email.toLowerCase();

  return (
    <AuthFrame>
      <AuthHeading
        title={`Join ${invite.businessName}`}
        subtitle={`${invite.inviterName} invited ${invite.email} as ${invite.roleName}. The invitation works until ${formatDate(invite.expiresAt.slice(0, 10))}.`}
      />
      <FormError message={formError} />
      {signedInAsInvitee ? (
        <Button variant="primary" size="lg" className="w-full justify-center" onClick={acceptSignedIn} loading={accepting}>
          Accept and open {invite.businessName}
        </Button>
      ) : invite.accountExists ? (
        <div className="space-y-4">
          <p className="text-[14px] text-muted">You already have a Managio account. Sign in as {invite.email} and you&apos;ll come straight back here.</p>
          <Button asChild variant="primary" size="lg" className="w-full justify-center">
            <Link href={`/sign-in?next=${encodeURIComponent(`/invite?token=${token}`)}`}>Sign in</Link>
          </Button>
          {googleEnabled && <GoogleButton onCredential={acceptWithGoogle} />}
        </div>
      ) : (
        <form onSubmit={createAndAccept} className="mt-2 space-y-4" noValidate>
          <Field label="Email">
            <Input value={invite.email} readOnly disabled className="h-11 text-[15px]" />
          </Field>
          <Field label="Your name" error={formState.errors.fullName?.message}>
            <Input autoComplete="name" className="h-11 text-[15px]" {...register("fullName")} />
          </Field>
          <Field label="Choose a password" hint="At least 8 characters" error={formState.errors.password?.message}>
            <Input type="password" autoComplete="new-password" className="h-11 text-[15px]" {...register("password")} />
          </Field>
          <Button type="submit" variant="primary" size="lg" className="w-full justify-center" loading={formState.isSubmitting}>
            Join {invite.businessName}
          </Button>
          {googleEnabled && (
            <>
              <div className="flex items-center gap-3 text-[12px] text-faint">
                <span className="h-px flex-1 bg-line" /> or <span className="h-px flex-1 bg-line" />
              </div>
              <GoogleButton onCredential={acceptWithGoogle} />
            </>
          )}
          <AgreementNote />
        </form>
      )}
    </AuthFrame>
  );
}
