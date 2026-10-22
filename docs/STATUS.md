# Status

Last updated: 2026-10-22. Read this first when resuming work.

## Where the project is

| Phase | State |
|---|---|
| 1. Audit and design directions | Done. Owner chose Variant A with borrowings (D-001) and widened the scope (D-002). |
| 2. Rebuild (backend, frontend, tests) | Done. |
| 2b. Deployment readiness | Done locally: production images build and pass a smoke test together (D-030). |
| 2c. Security and production-readiness remediation | Done on 2026-10-22 (below). Full regression green. |
| 2d. Release polish: legal pages, SEO, brand | Done on 2026-10-22 (below). Final runs after the footer change wait on free memory on the development machine. |
| 3. Deployment (Render, Neon, Vercel, Google sign-in, Resend) | Not started. The pipeline and runbook are ready (D-031, DEPLOYMENT.md); needs the owner's dashboards and approval to commit and push. |

Both v1 apps have been removed (still in git history). `managio_frontend/` is the v2 app (D-027).

## Regression on 2026-10-22 `[VERIFIED]`

| Check | Result |
|---|---|
| Backend `./mvnw clean verify` (unit, integration on PostgreSQL 17 in Testcontainers, contract, architecture, access sweep, package) | 169 tests, 0 failures, build success (`clean verify`, after the session fix below); then 170 with the invitation-with-Google test added (`./mvnw test`, 0 failures) |
| Frontend `npm run typecheck`, `npm run lint` | Clean (run in PowerShell; npm under Git Bash misreports exit codes) |
| Frontend `npm test` (Vitest) | 52 passed (8 files) |
| End to end `npx playwright test` against a production build and the real API on the local database, desktop and phone projects, Google button live, proxy secret on | 51 passed, 1 skipped (the phone-only layout test on the desktop project); rerun after the session fix with the same result |
| Screen sweep (in the e2e run): every screen at 1440 px and 390 px | No console errors (Google's own `gsi/style` CORS message excluded), no failed API calls, no sideways scrolling, no serious or critical axe violations |
| Production images together (`docker compose -p managio-smoke`, fresh database, random secrets) | db, api, web healthy; both apps uid 100; only the web port published; schema version 3, migrations healthy; CSP, HSTS, COOP, nosniff, DENY on the web app; API `/v3/api-docs` 404, readiness UP, API responses carry `default-src 'none'`; forged `X-Forwarded-For` to the API → 429 after 10; forged `X-Managio-*` headers through the web app dropped; trusted visitor addresses get separate limits; a retried payment with one `Idempotency-Key` recorded once; another owner gets 404 on every probed route including a payment POST; `/admin` 403 for an owner; API log has no secrets, tokens, passwords or errors. Torn down with `down -v`. |
| Google sign-in, real, end to end (web on 3000, API on 8081, local database) | Google's popup accepts `http://localhost:3000` (an unregistered port gets `origin_mismatch`, so the origin check is real). The owner signed in with their Google account (Google's own 2-step verification): first sign-in 06:22 created a verified, Google-only account (`POST /api/v1/auth/google` → 200); the second, 06:37, reused it, no duplicate. Sign-in events recorded as `google` SUCCESS. `mg_rt` cookie `HttpOnly; SameSite=Lax; Path=/`, 30 days (`Secure` is off only because local runs set `SECURE_COOKIES=false`). Redirect to `/app` → `/app/new` (no business yet). A reload restores the session (`/auth/refresh` 200). The account page shows "Google is linked" and offers to set a password without asking for a current one. Sign-out: 204, cookie removed, server session revoked, `/app` redirects to `/sign-in?next=/app`. |
| Client address hand-off through Next.js's real rewrite | Trusted header → API recorded `198.51.100.21`; forged `x-managio-*` and `X-Forwarded-For` → API recorded the connecting address. |
| Dependencies (OSV database, 122 Maven runtime and 138 shipped npm packages) | 0 known vulnerabilities after pinning Tomcat 11.0.27 and Jackson 3.1.8 / 2.21.8 (were: 3 critical Tomcat, 7 Jackson advisories). `npm audit --omit=dev`: 0. Dev-only: 5 high in the lint chain (`braces` via `eslint-config-next`), not shipped. |
| Secrets | No `.env` file is tracked or would be committed; no secret patterns in the files a commit would include. None of the 7 secrets found in git history is used by today's local `.env` files (compared by SHA-256 fingerprint, values never printed). |

## Changes in the remediation pass (2026-10-22)

**Security fixes (confirmed defects)**
* **Rate-limit bypass fixed** (D-032): forged `X-Forwarded-For` gave a fresh limit per address. The API now ignores forwarded headers; `ClientAddressFilter` trusts only the web app's `X-Managio-Client-Ip` proven by `API_PROXY_SECRET`; `src/proxy.ts` drops client-sent copies and adds them on Vercel. IPv6 grouped by /64. Tests: `ClientAddressTest` (7), `proxy.test.ts` (6).
* **Google sign-in bypassed lockout** and the account rules (D-034): locked and disabled accounts are now refused for Google too. **Account pre-hijacking closed**: linking Google to an account whose email was never verified removes its password and sessions. Tests: 4 new in `AuthFlowTest`, `GoogleIdentityVerifierTest` (12, real RS256 tokens: audience, issuer, expiry, signature, unsigned and HMAC tokens).
* **Duplicate money records on retry** (D-033): `Idempotency-Key` on invoices, sales, payments, refunds, expenses, payroll payments, memberships and renewals, stored in `idempotency_keys` (migration V3) in the action's transaction. Web forms send one key per submission. Tests: `IdempotencyTest` (6, including six concurrent duplicates → one payment).
* **Email bombing** via password reset from many addresses: per-recipient limit (3 an hour). Current-password guesses limited per account.
* **Account enumeration by timing**: Resend is now called on a background pool, so a reset request takes the same time whether or not the account exists.
* **Vulnerable shipped dependencies**: Tomcat and Jackson patched (above).
* **Security headers**: web app CSP, HSTS, COOP; API CSP and `Referrer-Policy: no-referrer`. CSRF and cross-origin refusal tested (`AuthFlowTest`).

**Correctness and UX fixes**
* Reversed date ranges on payments and expenses answer 400 instead of an empty list.
* Google button: a busy state and no double submission; invitation "Accept" has a loading state (a double click used to show a false error); undoing a check-in shows an error when it fails.
* `npm run api:types` failed on Windows (POSIX `${VAR:-default}` in package.json); now `scripts/api-types.mjs`.
* E2E selectors named the password "Sign in" button ambiguously once the Google button appears; now exact.

* **Replaced sessions stayed valid**: found during the real Google test (the 06:22 session was still active after the 06:37 sign-in in the same browser). A successful sign-in now ends the session of the cookie it replaces (staff, Google, invitation and portal); a failed sign-in leaves it alone. Tests: `AuthFlowTest.aNewSignInEndsTheSessionTheBrowserHadAndAFailedOneDoesNot`, `PortalTest.aNewPortalSignInEndsTheSessionTheBrowserHad`.

**Tests added:** `AccessSweepTest` walks every business route (outsider → 404 everywhere, no permission → 403 on every permissioned route, a manager with every permission can't do owner-only actions).

**Delivery:** Jenkinsfile rewritten for one route (D-031): Linux or Windows agent, poll trigger, immutable tags, Render deploy by image, Vercel deploy hook, readiness waits, smoke test, failure email, `ROLLBACK_TO_TAG`. `managio_frontend/vercel.json` stops push-triggered production deploys. Env examples, Compose, README and docs updated.

## Release polish pass (2026-10-22, later) `[VERIFIED]` unless noted

**Added**
* Legal and trust pages `/privacy`, `/terms`, `/security`, `/contact` (D-035, LEGAL.md): operator "Nitin, an individual based in India" and contact `nitinr0306@gmail.com` (both chosen by the owner); each claim traced to the code.
* SEO: per-page titles, descriptions, canonical URLs on `https://managio.me`, Open Graph and Twitter cards with a 1200 × 630 image, `robots.txt`, a five-URL sitemap, `noindex` on sign-in pages, `noindex, nofollow` on `/app`, `/admin`, `/portal` and the API (now also sent by the API itself), permanent `www` → apex redirect, `WebSite` + `SoftwareApplication` (free) structured data.
* Brand: logo mark (white "m" on cobalt) used in every header; favicon (16/32/48), SVG icon, Apple icon, manifest icons (incl. maskable) and the sharing image, generated by `scripts/brand-assets.mjs`; web manifest.
* Landing: "Free to use" stated, the illustration labelled "Example", "a few minutes" instead of an unsupported "two minutes"; standard footer with Help (Contact, Security) and Legal (Privacy Policy, Terms of Service) only, per the owner; account-creation screens (sign-up, invitation, portal sign-up) link to the Terms and the Privacy Policy.

**Fixed**
* Default Next.js favicon ("N") shown in browser tabs.
* Legal pages had no sharing image (Next.js drops the root image when a page sets its own Open Graph); now explicit.
* API responses had no `X-Robots-Tag` (proxied responses skip the web app's headers); the API sends it. Test: `AuthFlowTest`.
* Zod 4's `new Function` probe filed a CSP violation on every form page (`jitless` now).
* Landing page had no `<main>` landmark (Lighthouse accessibility 98 → fixed); phone header wrapped "Sign in" and "Create an account" onto two lines (header keeps "Sign in" on phones).
* Local `.env` files (owner-approved): API now on the local PostgreSQL (5433), Neon lines commented out; `MAIL_FROM` fixed to `noreply@managio.me` in both files.

**Evidence**
| Check | Result |
|---|---|
| Resend (send-only key, owner-approved) | Domain listing refused by design (send-only key). One test email from `noreply@managio.me` (to `support@managio.me`, the address planned at the time): accepted (HTTP 200, message id), so `managio.me` is verified for sending. |
| Lighthouse (production build, localhost) | Home: accessibility 98 (missing `<main>`, fixed since), best practices 100, SEO 100. `/privacy`: 100 / 100 / 100. `/sign-in`: SEO 63 by design (noindex), best practices 96 from the Zod probe (fixed since). Performance not measured by this tool. |
| `npx playwright test` (before the footer change) | 57 passed, 1 skipped, including `seo-and-legal.spec.ts` (6) and the sweep at 1440, 768 and 390 px with the legal pages |
| `seo-and-legal.spec.ts` alone | 6 passed |
| Web type check, lint, Vitest after the footer change | clean, clean, 52 passed |
| `ProductionDefaultsTest` (new), `AuthFlowTest` | 2 passed; 21 passed |
| Final run after the footer and contact changes | Backend `./mvnw clean verify`: 172 tests, 0 failures. Web: type check, lint clean; Vitest 52 passed; production build OK. `npx playwright test`: 56 passed, 1 skipped, 1 failed (an ambiguous test selector on /sign-up; test fixed) and `seo-and-legal.spec.ts` rerun: 6 passed. |
| Clean-checkout type check | Failed before (`Cannot find name 'LayoutProps'`: route types live in `.next/`); `typecheck` now runs `next typegen` first and passes with no `.next`. |
| Not rerun | Docker images and the Compose smoke test (last passed earlier on 2026-10-22, before the public-site changes; Dockerfiles unchanged). Lighthouse after the landmark fix (landmarks checked directly instead). |

## Why the Google button didn't show `[VERIFIED]`

The client ID was set correctly and identically in all three files, but the last web build (2026-10-21 19:13) predated `managio_frontend/.env.local` (2026-10-22 10:20). `NEXT_PUBLIC_*` values are compiled in when the app is built or `npm run dev` starts, so that build had none and hid the button. Rebuilding fixed it. Restart `npm run dev` after changing `.env.local`; rebuild Docker images after changing `MANAGIO_GOOGLE_CLIENT_ID`.

## Known risks and follow-ups `[PLANNED]` unless noted

1. **Exposed credentials, provider side** `[OPEN]`: the old Docker Hub token and Resend key in public git history must be revoked in those dashboards; local files no longer use them. Rewriting history is not planned (values are already public; rotation is the fix).
2. ~~Local API `.env` pointed at Neon~~ Fixed 2026-10-22: local PostgreSQL on 5433. For Render, use Neon's direct host (Flyway's lock doesn't work through the pooler).
3. ~~`MAIL_FROM` typo~~ Fixed 2026-10-22 in both local `.env` files.
4. **CSP keeps `'unsafe-inline'` for scripts** (Next.js inline data); a nonce-based policy would end prerendering of public pages.
5. **Rate-limit counters are in memory**: correct for one Render instance; a second instance needs a shared store. Restarts reset counters (lockout is persistent).
6. **Platform console sign-in strength.** Read-only; add an authenticator-app step before it gains actions that change data (D-028).
7. **Node 20 locally is past end of life.** The images use Node 22.
8. **Dev-only npm advisories** in the lint chain; the offered fix downgrades `eslint-config-next`, so not applied.
9. **Local Docker leftovers**: a stopped `jenkins` container publishes host port 8081 and a stopped `agrinex-dev-agrinex-db-1` publishes 5433; starting either would collide with the API or the dev database.
10. **Lockout as a denial of service**: anyone who knows an email can lock that account for 15 minutes (now for Google too, by the owner's rule); rate limits slow it down.

## Environment facts `[VERIFIED]` 2026-10-22

* Local toolchain: Java 21.0.8, Node 20.20.2, npm 10.8.2, Docker Desktop 29.2.0 (must be running for Testcontainers, the development database and image builds).
* Jenkins 2.555.1 runs as the Windows service `jenkins` on port 8080 (home `C:\ProgramData\Jenkins\.jenkins`). It has no job for this repository yet (only a demo pipeline and a job for another repository).
* The API listens on **8081** by default (`server.port: ${PORT:8081}`, confirmed at start-up). Run it from `managio_backend` so it reads `managio_backend/.env`.
* Development database: Docker container `managio-dev-db` (PostgreSQL 17, port 5433, at schema version 3). Tests use Testcontainers; H2 is not used (TESTING.md).
* The web app runs with `npm run dev` on port 3000 and passes `/api/*` to `API_ORIGIN` (`http://localhost:8081`).
* The production domain is `https://managio.me`.
* The JVM default time zone is pinned to UTC in `ManagioApplication.main`.
* Stitch MCP works with direct calls; Figma has a View seat only; Context7 rejects its API key.

## Open questions `[OPEN]`

1. **Google production origin.** Add `https://managio.me` to the OAuth client's JavaScript origins (localhost is confirmed working); publish the consent screen.
2. **Production access.** Render, Neon, Vercel and Docker Hub dashboards (DEPLOYMENT.md, "First production deployment").
3. **Production reset.** Resetting the Neon database gets a final confirmation from the owner at the moment it happens (D-021).
4. **Email domain.** `managio.me` is verified for sending (Resend accepted a test email on 2026-10-22). The public contact is `nitinr0306@gmail.com`.
5. **Credential revocation.** Confirm the old Docker Hub token and Resend key are revoked (risk 1).
6. **Legal pages.** Written (D-035); items for the owner and for a lawyer are in LEGAL.md. Enter the URLs in Google's consent screen only after they answer 200 in production.
7. **Portal sign-up squatting.** Self sign-up does not verify the email or phone (D-022). Decide whether to require email verification at sign-up.
8. **Commit and push.** Local `master` is 1 ahead and 5 behind GitHub (owner's README edits and screenshots from June); nothing from v2 is committed. Merge plan in the final report of 2026-10-22.
9. **Context7 key.** Replace it if library docs through MCP are wanted.

## Next steps

1. Owner: approve the commit and merge; revoke the old credentials; fix the local `.env` items (risks 2–3).
2. Owner: dashboards per DEPLOYMENT.md (Neon direct host, Docker Hub, Render image service, Vercel project and deploy hook, Google origins, Resend domain), then the Jenkins job.
3. Run the pipeline on master and the manual smoke test, including a real Google sign-in and a real email.
