import type { Metadata } from "next";
import Link from "next/link";
import { LegalPage, LegalSection } from "@/components/site/legal-page";
import { publicPageMetadata, SITE } from "@/lib/site";

// Plain-language terms that match how Managio works today. Clauses marked for review in docs/LEGAL.md (liability,
// governing law) need a qualified lawyer before Managio relies on them.

export const instant = true;

export const metadata: Metadata = publicPageMetadata("/terms", "Terms of Service", "The terms for using Managio, the free business management app: accounts, your data, acceptable use and our responsibilities.");

export default function Page() {
  const email = SITE.supportEmail;
  return (
    <LegalPage
      title="Terms of Service"
      updated={SITE.legalUpdated}
      intro={
        <p>
          These terms apply to Managio at managio.me, run by {SITE.operator} (&ldquo;we&rdquo;). By creating an account or using
          Managio you agree to them. If you use Managio for a business, you agree on behalf of that business too.
        </p>
      }
    >
      <LegalSection id="service" title="The service">
        <p>
          Managio helps businesses keep track of customers, memberships, invoices, payments, expenses, staff, tasks and approvals, with a
          portal where a business&apos;s customers can see their own records. Managio is free to use. If we ever offer paid features, we
          will say so in advance, and nobody will be charged without agreeing first.
        </p>
        <p>
          Managio records money a business has received or spent; it does not move money, process card payments or give financial, tax or
          legal advice. Check figures such as tax and profit with your accountant.
        </p>
      </LegalSection>

      <LegalSection id="accounts" title="Your account">
        <ul>
          <li>Give accurate details, keep your password to yourself, and tell us if you think someone else has used your account.</li>
          <li>You are responsible for what happens under your account, and owners for the people they invite and the roles they give.</li>
          <li>Accounts are for people aged 18 or over.</li>
        </ul>
      </LegalSection>

      <LegalSection id="data" title="Your data">
        <p>
          Your business&apos;s data belongs to the business. You allow us to store, process, back up and display it only as needed to run
          Managio for you, as described in our <Link href="/privacy">Privacy Policy</Link>.
        </p>
        <ul>
          <li>
            Record personal data about customers and staff only when you have a reason and the right to, and tell them how you use it.
            For children, have a parent&apos;s or guardian&apos;s permission.
          </li>
          <li>
            Managio is not built for medical or other sensitive records. Do not store clinical notes, diagnoses, government ID numbers,
            passwords or card details in it.
          </li>
          <li>Keep your own copies of anything important, for example with the customer export.</li>
        </ul>
      </LegalSection>

      <LegalSection id="use" title="Acceptable use">
        <p>Do not use Managio to:</p>
        <ul>
          <li>break the law, or store data you are not allowed to have;</li>
          <li>send unwanted invitations or messages, or harass anyone;</li>
          <li>try to reach data or accounts that are not yours, or get around limits and permissions;</li>
          <li>overload, scrape or disrupt the service, or probe its security without following our <Link href="/security">security page</Link>.</li>
        </ul>
      </LegalSection>

      <LegalSection id="availability" title="Availability and changes">
        <p>
          We work to keep Managio running and your data safe, but we provide it &ldquo;as is&rdquo; and &ldquo;as available&rdquo;,
          without guarantees that it will always be available or free of errors. We may change, add or remove features. If we plan to
          shut Managio down, we will give account holders reasonable notice and a way to export their data.
        </p>
      </LegalSection>

      <LegalSection id="liability" title="Responsibility for losses">
        <p>
          To the extent the law allows, we are not responsible for indirect or consequential losses, such as lost profits or lost data
          you had not kept a copy of. Our total responsibility to you for any claim is limited to the amount you paid us for Managio in
          the twelve months before the claim, which is nothing while Managio is free. Nothing in these terms limits anything the law does
          not allow us to limit.
        </p>
      </LegalSection>

      <LegalSection id="ending" title="Ending your use">
        <p>
          You can stop using Managio at any time and ask us to delete your account by emailing <a href={`mailto:${email}`}>{email}</a>.
          We may suspend or close an account that breaks these terms or puts other people or the service at risk, and will tell you why
          unless the law or safety prevents it.
        </p>
      </LegalSection>

      <LegalSection id="changes" title="Changes to these terms">
        <p>
          We may update these terms and will change the date at the top when we do. For changes that matter, we will tell account holders
          by email or in Managio before they take effect. Using Managio after that means you accept the new terms.
        </p>
      </LegalSection>

      <LegalSection id="law" title="Law">
        <p>These terms are governed by the laws of India.</p>
      </LegalSection>

      <LegalSection id="contact" title="Contact">
        <p>
          Questions about these terms: <a href={`mailto:${email}`}>{email}</a>.
        </p>
      </LegalSection>
    </LegalPage>
  );
}
