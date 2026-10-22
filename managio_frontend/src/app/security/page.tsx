import type { Metadata } from "next";
import Link from "next/link";
import { LegalPage, LegalSection } from "@/components/site/legal-page";
import { publicPageMetadata, SITE } from "@/lib/site";

// Each protection listed here is implemented and covered by automated tests (docs/STATUS.md, docs/TESTING.md).
// Do not add claims of certifications or audits Managio does not have.

export const instant = true;

export const metadata: Metadata = publicPageMetadata("/security", "Security", "How Managio protects accounts and business data, and how to report a security issue.");

export default function Page() {
  const email = SITE.supportEmail;
  return (
    <LegalPage
      title="Security"
      intro={<p>Businesses trust Managio with their customers&apos; details and their money records. This is how we look after them.</p>}
    >
      <LegalSection id="accounts" title="Signing in">
        <ul>
          <li>Passwords are stored only as BCrypt hashes. An unknown email and a wrong password get the same answer.</li>
          <li>Five wrong passwords in a row lock the account for 15 minutes, whichever way you then try to sign in.</li>
          <li>Sign-in, sign-up and password-reset attempts are rate-limited per visitor, and reset emails per recipient.</li>
          <li>
            Google sign-in tokens are checked for Google&apos;s signature, the right application, expiry and a verified email before
            anyone is signed in.
          </li>
        </ul>
      </LegalSection>

      <LegalSection id="sessions" title="Sessions">
        <ul>
          <li>The access token lasts 15 minutes and is kept only in the page&apos;s memory.</li>
          <li>
            The session cookie is HttpOnly, Secure and SameSite, is replaced each time it is used, and a reused old copy ends the whole
            session. Signing out, changing or resetting your password ends sessions.
          </li>
        </ul>
      </LegalSection>

      <LegalSection id="data" title={"Your business's data"}>
        <ul>
          <li>Every business is walled off: every request is checked against the person&apos;s membership and role on the server.</li>
          <li>Team members see only what their role allows; customers in the portal see only their own records.</li>
          <li>Payments, invoices and expenses are voided with a reason, never deleted, and changes are recorded in the activity log.</li>
          <li>Recording the same payment twice by accident, for example after a dropped connection, is prevented on the server.</li>
          <li>Managio does not process payments and never stores card numbers.</li>
        </ul>
      </LegalSection>

      <LegalSection id="platform" title="The platform">
        <ul>
          <li>Pages and the API are served over HTTPS only, with a strict content security policy and other browser protections.</li>
          <li>Secrets live in the hosting providers&apos; protected settings, never in the code.</li>
          <li>We check the libraries Managio is built on for known vulnerabilities and keep them updated.</li>
        </ul>
      </LegalSection>

      <LegalSection id="report" title="Reporting a vulnerability">
        <p>
          If you think you have found a security problem, email <a href={`mailto:${email}`}>{email}</a> with &ldquo;Security&rdquo; in the
          subject, the steps to reproduce it and what you think the impact is. Please:
        </p>
        <ul>
          <li>test only with accounts and businesses you created yourself;</li>
          <li>do not access, change or delete other people&apos;s data, and stop if you come across any;</li>
          <li>do not run tests that slow the service down for others, or send spam;</li>
          <li>give us a reasonable chance to fix the problem before telling anyone else.</li>
        </ul>
        <p>
          We will confirm we received your report and keep you updated. There is no paid bug bounty. See also our{" "}
          <Link href="/privacy">Privacy Policy</Link>.
        </p>
      </LegalSection>
    </LegalPage>
  );
}
