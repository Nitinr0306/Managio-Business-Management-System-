import type { Metadata } from "next";
import Link from "next/link";
import { LegalPage, LegalSection } from "@/components/site/legal-page";
import { publicPageMetadata, SITE } from "@/lib/site";

// Every statement here describes what the code does (docs/LEGAL.md maps each one to its source). Items that need
// the owner's confirmation or a lawyer's review are listed in docs/LEGAL.md, not guessed here.

export const instant = true;

export const metadata: Metadata = publicPageMetadata("/privacy", "Privacy Policy", "What Managio collects, why, who helps us run it, how long we keep it and how to reach us about your data.");

export default function Page() {
  const email = SITE.supportEmail;
  return (
    <LegalPage
      title="Privacy Policy"
      updated={SITE.legalUpdated}
      intro={
        <p>
          Managio (managio.me) is business management software for gyms, salons, coaching centres, schools, clinics and shops. It is
          run by {SITE.operator} (&ldquo;we&rdquo;). This policy explains what personal data Managio handles, why, and what you can do
          about it. Questions go to <a className="font-medium text-cobalt underline underline-offset-2" href={`mailto:${email}`}>{email}</a>.
        </p>
      }
    >
      <LegalSection id="two-kinds" title="Two kinds of data">
        <p>
          <strong>Account data</strong> is about you as someone who uses Managio: your name, email and how you sign in. We decide how
          it is used, as described here.
        </p>
        <p>
          <strong>Business data</strong> is what a business records in Managio about its customers, team and money. The business decides
          what to record and why, and we store and process it on the business&apos;s behalf to provide the service. If you are a customer
          of a business that uses Managio, please contact that business first about your data; we will help it respond.
        </p>
      </LegalSection>

      <LegalSection id="collect" title="What we collect">
        <ul>
          <li>
            <strong>Your account:</strong> name, email address, phone number (optional), whether you confirmed your email, and when you
            last signed in. Your password is stored only as a one-way BCrypt hash; we cannot see it.
          </li>
          <li>
            <strong>Google sign-in, if you use it:</strong> Google sends us a signed token with your Google account ID, email address,
            whether Google has verified that email, and your name. We do not receive your Google password or access to your Gmail,
            contacts, files or any other Google service.
          </li>
          <li>
            <strong>Sign-in and security records:</strong> for each sign-in attempt and session, the IP address, browser (user agent),
            time and outcome; counts of failed attempts and temporary lockouts. Session tokens are stored only as hashes.
          </li>
          <li>
            <strong>Activity log:</strong> changes made in a business, who made them, when, and from which IP address, so owners can see
            what happened.
          </li>
          <li>
            <strong>Business data entered by businesses and their teams:</strong> the business&apos;s details (name, contact details,
            address, GSTIN and settings); customers (name, phone, email, and optionally date of birth, gender, address, a guardian&apos;s
            name and phone, and notes); memberships and plans; invoices, payments and refunds; expenses; team members (job title, phone,
            employee code, joining date, salary and payroll); attendance, tasks, requests and notifications.
          </li>
          <li>
            <strong>Customer portal:</strong> if a business invites you to its portal, your login (email or phone), a hash of your
            password and your sign-in records.
          </li>
        </ul>
        <p>
          Managio records payments that a business has received by cash, UPI, card, bank transfer or other means. It does not process
          payments and never receives or stores card numbers or bank login details.
        </p>
      </LegalSection>

      <LegalSection id="use" title="How we use it">
        <ul>
          <li>To provide Managio: sign you in, store and show your records to the people allowed to see them, and send the emails below.</li>
          <li>To keep Managio secure: limit repeated attempts, lock an account after repeated wrong passwords, and investigate misuse.</li>
          <li>To answer you when you contact us, and to fix problems and keep the service running, including backups.</li>
        </ul>
        <p>
          We do not sell personal data, show advertising, or use advertising or analytics trackers. Within a business, what each team
          member can see is set by the role its owner gives them.
        </p>
      </LegalSection>

      <LegalSection id="emails" title="Emails Managio sends">
        <p>
          Account emails: confirming your email address, resetting a password, and invitations to join a business&apos;s team or its
          customer portal. Businesses can also have Managio email their customers a reminder three days before a plan ends; each business
          can turn these reminders off. Emails are sent from noreply@managio.me through our email provider.
        </p>
      </LegalSection>

      <LegalSection id="cookies" title="Cookies and similar storage">
        <p>
          Managio uses only cookies that are strictly necessary to keep you signed in: <strong>mg_rt</strong> for team accounts and{" "}
          <strong>mg_prt</strong> for the customer portal. They cannot be read by page scripts, are sent only to Managio, and last up to
          30 days or until you sign out. The short-lived access token that proves who you are is kept in the page&apos;s memory, not in a
          cookie or browser storage.
        </p>
        <p>
          If you use Google sign-in, Google&apos;s sign-in script may store its own small cookie (such as <strong>g_state</strong>) to
          remember its state; Google&apos;s privacy policy covers it. Because there are no analytics or advertising cookies, Managio does
          not show a cookie banner.
        </p>
      </LegalSection>

      <LegalSection id="providers" title="Who helps us run Managio">
        <p>These service providers process data only to provide their service to us:</p>
        <ul>
          <li><strong>Vercel</strong> delivers the website and app pages.</li>
          <li><strong>Render</strong> runs Managio&apos;s application servers.</li>
          <li><strong>Neon</strong> hosts the PostgreSQL database where account and business data are stored.</li>
          <li><strong>Resend</strong> sends Managio&apos;s emails.</li>
          <li><strong>Google</strong> signs you in, only if you choose Google sign-in.</li>
        </ul>
        <p>
          These providers may process data in countries other than the one you live in. We share data with others only if the law
          requires it.
        </p>
      </LegalSection>

      <LegalSection id="retention" title="How long we keep data">
        <ul>
          <li>Account and business data are kept while the account or business exists on Managio.</li>
          <li>
            Payments, invoices and expenses are voided with a reason instead of being deleted, so a business&apos;s books stay correct.
            An archived business keeps its data.
          </li>
          <li>Expired sessions, one-time links and retry records are deleted automatically about a week after they expire.</li>
          <li>Sign-in records and the activity log are kept for security and auditing while the account or business exists.</li>
        </ul>
        <p>
          If you ask us to delete your account, we will delete it, and the data of any business you own, after confirming the request
          comes from you, except for anything we must keep to meet a legal obligation or resolve a dispute.
        </p>
      </LegalSection>

      <LegalSection id="choices" title="Your choices">
        <ul>
          <li><strong>See and correct</strong> your name and phone number on your account page at any time.</li>
          <li>
            <strong>Take your data with you:</strong> business owners can export their customer list as a CSV file. For anything else,
            email us.
          </li>
          <li>
            <strong>Delete your account</strong> or ask what data we hold about you by emailing{" "}
            <a href={`mailto:${email}`}>{email}</a> from your account&apos;s email address. Account deletion is handled by email for now.
          </li>
          <li>
            <strong>Customers of a business</strong> should ask that business to correct or delete their records; the business controls
            them.
          </li>
        </ul>
        <p>
          Depending on where you live, data-protection law (for example India&apos;s Digital Personal Data Protection Act, 2023) may give
          you further rights. We will respond to requests as the law that applies to us requires.
        </p>
      </LegalSection>

      <LegalSection id="security" title="Security">
        <p>
          Managio is served over HTTPS only, keeps every business&apos;s data separate, and checks permissions on the server for every
          request. Our <Link href="/security">security page</Link> describes the protections in place and how to report a problem.
        </p>
      </LegalSection>

      <LegalSection id="children" title="Children">
        <p>
          Managio accounts are for adults running or working in a business. Businesses such as schools and coaching centres may record
          details of students who are children, and are responsible for having permission to do so, for example from a parent or
          guardian.
        </p>
      </LegalSection>

      <LegalSection id="changes" title="Changes to this policy">
        <p>
          When this policy changes, we will update it here and change the date at the top. If a change affects how we use data you have
          already given us, we will tell account holders by email or in Managio before it takes effect.
        </p>
      </LegalSection>

      <LegalSection id="contact" title="Contact">
        <p>
          Email <a href={`mailto:${email}`}>{email}</a> with any question about this policy or your data. See also our{" "}
          <Link href="/terms">Terms of Service</Link>.
        </p>
      </LegalSection>
    </LegalPage>
  );
}
