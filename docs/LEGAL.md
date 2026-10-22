# Legal and trust pages

The public pages `/privacy`, `/terms`, `/contact` and `/security` (`managio_frontend/src/app/<page>/page.tsx`). They describe what the code actually does; this file records the source for each claim and what still needs the owner's confirmation or a qualified lawyer's review. Status labels as in the rest of `docs/`.

## Facts confirmed by the owner `[DECIDED]` 2026-10-22

| Fact | Where it appears |
|---|---|
| Operator: Nitin, an individual based in India (no company, no address published) | Privacy, Terms, Contact (`SITE.operator` in `src/lib/site.ts`) |
| Contact, privacy and security address: `nitinr0306@gmail.com` (the owner's own email; chosen instead of `support@managio.me` later on 2026-10-22) | All four pages (`SITE.supportEmail`) |
| Domain: `https://managio.me` | Canonical URLs, sitemap, sharing cards (`SITE.url`) |
| Managio v1 is free; no payments, subscriptions or payment gateway | Terms ("The service"), landing page, structured data (price 0) |

## Claims and their source in the code `[VERIFIED]` 2026-10-22

| Claim | Source |
|---|---|
| Account data: name, email, optional phone, email verification, last sign-in; passwords as BCrypt hashes | `users` table (V1), `AuthService.hashPassword` |
| Google sign-in receives only the account ID, email, verified flag and name | `GoogleIdentityVerifier` (ID token claims `sub`, `email`, `email_verified`, `name`); scopes `openid email profile` |
| Sign-in records: IP, user agent, time, outcome; lockouts | `sign_in_events`, `refresh_tokens`, `SignInAttempts`, `ClientAddressFilter` |
| Activity log with IP | `activity_events`, `ActivityLog` |
| Customer fields: name, phone, email, date of birth, gender, address, guardian name and phone, notes | `customers` table |
| Team fields: title, phone, employee code, joining date, salary and payroll | `team_members`, `payroll_entries` |
| No card data, no payment processing | No gateway in v2 (D-033 context); payments are records of money received |
| Cookies `mg_rt`, `mg_prt`: HttpOnly, Secure in production, SameSite=Lax, up to 30 days; access token in memory | `RefreshCookies`, `ProductionDefaultsTest`, `src/lib/api/client.ts` |
| No analytics or advertising trackers; only Google Identity Services loads from a third party | CSP in `next.config.ts` allows only `self` and `accounts.google.com/gsi/` |
| Emails: verification, reset, team and portal invitations, renewal reminders (switchable per business) | `MailTemplates`, `RenewalReminders`, `businesses.renewal_reminders` |
| Expired sessions, one-time links and retry keys deleted about a week after expiry | `TokenCleanup`, `IdempotentRequests.deleteOlderThanAWeek` |
| Records are voided, not deleted; archived businesses keep data | `PaymentService.voidPayment`, `BusinessService.archive` |
| Customer CSV export; profile name and phone editable | `CustomerController` export, `PATCH /api/v1/me` |
| No self-service account deletion; deletion by email | No delete-account endpoint exists |
| Security page controls (lockout 5 / 15 min, rate limits, token checks, rotation, tenant isolation, idempotency) | D-032, D-033, D-034; `AuthFlowTest`, `ClientAddressTest`, `AccessSweepTest`, `IdempotencyTest` |
| Service providers: Vercel, Render (application servers), Supabase (database), Resend, Google | D-037 (hosting settled on 2026-10-22, before any production deployment) |

## Needs the owner's confirmation `[OPEN]`

1. **Contact address.** The pages publish `nitinr0306@gmail.com`. If a `managio.me` mailbox is set up later, change `SITE.supportEmail` in one place.
2. **Hosting regions.** The privacy policy says providers may process data outside the user's country. Render and Supabase both run in Singapore (D-037); once deployed, consider naming the region.
3. **Deletion timeline.** The policy promises deletion "after confirming the request" without a number of days. Decide on a period you can keep, then state it.
4. **Backups.** Supabase Free keeps no backups; the owner keeps weekly `pg_dump` copies on their own PC, where deleted data can persist until the copies are deleted. Consider saying so once the plan is known.

## For a qualified lawyer's review `[OPEN]`

* Whether the privacy policy meets India's Digital Personal Data Protection Act, 2023 and its rules (notice, consent, grievance redressal, a named contact for data principals), and any law that applies to users outside India (for example the GDPR).
* Managio's role for customer data entered by businesses (data processor or equivalent) and whether a data-processing agreement is needed for business accounts.
* The liability limitation, "as is" wording and governing-law clause in the Terms, and whether a jurisdiction or courts clause should be added.
* Records of children entered by schools and coaching centres (the Terms require guardian permission; the DPDP Act has specific rules for children's data).
* Age requirement (18+) for accounts.

Nothing in these pages claims legal compliance, a certification or an audit. Do not add such claims without evidence.
