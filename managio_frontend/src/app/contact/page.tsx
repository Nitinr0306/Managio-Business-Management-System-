import type { Metadata } from "next";
import Link from "next/link";
import { LegalPage, LegalSection } from "@/components/site/legal-page";
import { publicPageMetadata, SITE } from "@/lib/site";

export const instant = true;

export const metadata: Metadata = publicPageMetadata("/contact", "Contact", "How to reach Managio for help with your account, your data, or a security issue.");

export default function Page() {
  const email = SITE.supportEmail;
  return (
    <LegalPage
      title="Contact"
      intro={
        <p>
          Managio is run by {SITE.operator}. Support is by email: write to{" "}
          <a className="font-semibold text-cobalt underline underline-offset-2" href={`mailto:${email}`}>{email}</a> and you will get a
          reply by email.
        </p>
      }
    >
      <LegalSection id="help" title="Help with your account">
        <p>Tell us the email address you sign in with, what you were trying to do, and what happened instead. A screenshot helps.</p>
        <p>
          Forgotten your password? Use <Link href="/forgot-password">Reset your password</Link> on the sign-in page; it is faster than
          writing to us.
        </p>
      </LegalSection>

      <LegalSection id="customers" title="If you are a customer of a business">
        <p>
          Your plan, payments and receipts belong to the business you signed up with, so ask them first about your membership, a payment
          or your portal invitation. We can help with problems in Managio itself.
        </p>
      </LegalSection>

      <LegalSection id="data" title="Your data">
        <p>
          To ask what data we hold about you, or to delete your account, email us from your account&apos;s email address. Our{" "}
          <Link href="/privacy">Privacy Policy</Link> explains what we collect and why.
        </p>
      </LegalSection>

      <LegalSection id="security" title="Security issues">
        <p>
          Found a vulnerability? Please read our <Link href="/security">security page</Link> and email us with &ldquo;Security&rdquo; in
          the subject.
        </p>
      </LegalSection>
    </LegalPage>
  );
}
