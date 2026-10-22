# Decision log

Each entry records a significant decision: its status, the context, what was decided, alternatives considered and why. Newest entries go at the bottom. Superseded decisions stay in the log with a pointer to the replacement.

Status labels are defined in [README.md](README.md).

---

## D-001 Frontend design direction
**Status:** `[APPROVED]` 2026-10-20

**Context:** Phase 1 produced three design directions (A Runway, B Front Desk, C Portfolio), shown on Dashboard, Members and Sign in in Stitch and as HTML prototypes (`design/prototypes/`, comparison page https://claude.ai/artifact/LaqFCoNVVmgaM2ZLiT9TqQ).

**Decision:** Build Variant A (Runway) with three borrowings, and add a marketing landing page:
* B's always-visible member search and the member panel with one main action (renew or record payment).
* C's portfolio overview for owners with two or more businesses.
* B's sign-in page (role tiles: "I run a business", "I work here", "I'm a member").
* A landing page before sign-in that makes the product appealing.

**Owner's words:** "My recommendation is A, with two ideas borrowed … i like the B's login page and there should be amazing hero page before login and all. the website should be so apealing."

---

## D-002 Product scope: a multi-business operating system
**Status:** `[APPROVED]` 2026-10-20

**Decision:** Managio is rethought as a platform where one owner manages multiple businesses, each with staff, customers or members, payments, expenses, tasks, requests, approvals, daily operations and reports. Different business types (gym, salon, coaching centre, school, retail shop and others) must not be forced into one operational model. The engineer makes the product and architecture decisions and may rebuild the backend.

**Owner's words:** "Imagine a platform where a business owner can manage multiple businesses from one place, with everything each business needs … Use your own judgment to determine the right product design, business logic, workflows, architecture, database structure, APIs, and frontend experience … you can rebuild the backend if you want."

---

## D-003 Rebuild the backend on Spring Boot 4.1 in package `com.managio`
**Status:** `[DECIDED]` 2026-10-20

**Context:** The v1 backend (Spring Boot 3.3.5, package `com.nitin.saas`) has structural defects in identity and tenancy (see [LEGACY_AUDIT.md](LEGACY_AUDIT.md)): member and user IDs are compared with each other, three permission systems disagree, and tenant checks are ad hoc. Spring Boot 3.3 is out of open-source support. The new scope (D-002) adds invoices, expenses, requests, payroll, day book and business types.

**Decision:** Rewrite the backend in the same Maven project, on Spring Boot 4.1.x and Java 21, under `com.managio`. v1 code is removed. The database starts fresh (D-021).

**Alternatives:** Incremental refactor of v1 (rejected: the identity and tenancy model is wrong at the root and every module depends on it). Staying on Spring Boot 3.x (rejected: unsupported line; a rewrite is the cheapest time to move).

---

## D-004 Modular monolith, packaged by feature
**Status:** `[DECIDED]` 2026-10-20

**Decision:** One deployable API. Packages by feature plus `platform` for cross-cutting concerns. No interface/implementation pairs for single implementations, no mapper framework. The package layout was refined by D-029.

**Rationale:** The data is tightly connected (a payment touches an invoice, a membership, the day book and reports). Services would add network hops and distributed transactions with no scaling need. Feature packages keep boundaries visible.

---

## D-005 Business types are templates over one engine
**Status:** `[DECIDED]` 2026-10-20

**Decision:** A business type (Fitness, Salon, Coaching, School, Clinic, Retail, Other) sets vocabulary, default modules, default roles and default expense categories. All types share the same engine. Owners can switch optional modules on or off. Details in [PRODUCT.md](PRODUCT.md#business-types).

**Alternatives:** Separate workflows per type in code (rejected: multiplies code and tests; most differences are vocabulary and which features are visible).

---

## D-006 One account type for owners and staff; customers are separate
**Status:** `[DECIDED]` 2026-10-20

**Decision:** Owners and staff are both `users`; a user's role in a business comes from their team membership. Staff sign in with email and password or Google, without a business ID, and pick a business after signing in. Customers have a separate portal identity scoped to one business. Tokens carry the subject type, and the code uses separate principal classes for users and customers.

**Rationale:** Fixes the v1 defect where member IDs were compared with user IDs, and lets one person own one business while working in another.

---

## D-007 Per-business roles with a fixed permission catalogue
**Status:** `[DECIDED]` 2026-10-20

**Decision:** Each business has roles; a role is a named set of permissions from a catalogue defined in code. Business types seed default roles. The Owner role has every permission and cannot be edited, removed or assigned through an invitation. There are no per-person permission overrides.

**Alternatives:** v1's mix of role sets, per-staff grants/revokes and boolean flags (rejected: three sources of truth that disagreed).

---

## D-008 Tenancy enforced at the edge and in every query
**Status:** `[DECIDED]` 2026-10-20

**Decision:** Business-scoped endpoints live under `/api/v1/businesses/{businessId}/…`. A single interceptor resolves the caller's active team membership in that business and returns 404 if there is none. Controllers declare the required permission with `@RequirePermission`. Every repository method for tenant data takes the business ID.

---

## D-009 Token handling
**Status:** `[DECIDED]` 2026-10-20

**Decision:** Short-lived JWT access tokens (15 minutes) held in memory by the frontend and sent as a Bearer header. Refresh tokens are random 256-bit values stored only as SHA-256 hashes, rotated on each use with reuse detection per token family, and delivered as `HttpOnly; Secure; SameSite=Lax` cookies. The frontend reaches the API through a same-origin `/api` rewrite on Vercel so cookies are first-party.

**Alternatives:** Tokens in localStorage (v1; rejected: readable by any injected script). Cross-site cookies to the Render domain (rejected: blocked or unreliable in browsers that restrict third-party cookies).

---

## D-010 Google sign-in through Google Identity Services
**Status:** `[DECIDED]` 2026-10-20

**Decision:** The browser obtains a Google ID token with Google Identity Services; the API verifies signature, issuer, audience (the existing Google client ID) and expiry, then signs the user in or creates their account. No redirect flow and no client secret are needed.

**Dependency:** The Google Cloud OAuth client must list the production and local origins as authorised JavaScript origins. Tracked as an open item in [STATUS.md](STATUS.md).

---

## D-011 Money model: invoices, payments, expenses
**Status:** `[DECIDED]` 2026-10-20

**Decision:** Money in is recorded as invoices (what is owed) and payments (what was received, each with a receipt number and the person who took it). Money out is recorded as expenses against categories. Balances, dues and profit are computed from these records, never stored as running totals. Every payment belongs to an invoice.

---

## D-012 Membership expiry is derived from dates
**Status:** `[DECIDED]` 2026-10-20

**Decision:** A membership stores start and end dates and is either active or cancelled. Whether it is upcoming, running, ending soon or expired is computed from the dates in the business time zone. No nightly job changes membership status.

**Rationale:** v1's expiry job let stored status drift from reality whenever the job did not run.

---

## D-013 Migrate v1 data forward in one Flyway migration
**Status:** Superseded by D-021 on 2026-10-20. Was `[DECIDED]`; the migration was written and passed its data-preservation test before the owner chose a fresh start.

**Decision:** `V17__managio_v2_schema.sql` moves the v1 tables into a temporary `legacy` schema, creates the v2 tables, copies all meaningful rows keeping their primary keys, and drops the legacy schema, all in one transaction. Refresh, verification and reset tokens are not carried over. Details and mapping in [DATA_MODEL.md](DATA_MODEL.md#migrations-verified-migrationtest-admintest).

---

## D-014 Readable per-business numbers
**Status:** `[DECIDED]` 2026-10-20

**Decision:** Customers, invoices and receipts get sequential numbers per business from a locked sequence table, so staff can read them over the phone and receipts have no gaps.

---

## D-015 Rebuild the frontend on current Next.js
**Status:** `[DECIDED]` 2026-10-20

**Decision:** Replace the v1 frontend with a new app on Next.js 16, React 19, Tailwind CSS 4, TanStack Query, React Hook Form with Zod and Radix primitives, with API types generated from the backend's OpenAPI document.

**Rationale:** The new design (D-001), the unified owner and staff workspace and the new modules touch every screen; v1's dark glass styling and duplicated page trees would not survive the redesign.

---

## D-016 Test against real PostgreSQL
**Status:** `[DECIDED]` 2026-10-20

**Decision:** Integration tests run the full application against PostgreSQL in Testcontainers with the real migrations, including a test that loads v1-shaped data and migrates it. Browser end-to-end tests use Playwright against the real frontend and API.

---

## D-017 Email through Resend; no SMTP
**Status:** `[DECIDED]` 2026-10-20

**Decision:** Keep the existing Resend integration (HTTP API) behind a small `Mailer` interface. In development without an API key, emails are logged instead of sent. SMTP is not used.

---

## D-018 Legacy business types map to Fitness by default
**Status:** Superseded by D-021 on 2026-10-20 (no v1 data is migrated).

**Decision:** v1 stored business type as free text. The migration maps recognisable words to the new types and treats empty values as Fitness, because v1 was built for gyms and studios. Owners can change the type in settings.

---

## D-019 One full-name field
**Status:** `[DECIDED]` 2026-10-20

**Decision:** People have one `full_name` field instead of first and last names. Indian names do not split reliably into first and last, and nothing in the product needs the split.

---

## D-020 Out of scope for v2
**Status:** `[DECIDED]` 2026-10-20

**Decision:** Not built in v2: appointments and calendars, staff commissions, online payment collection, WhatsApp reminders, GST reports, file attachments, multi-currency. The design leaves room for them.

---

## D-021 Start the database fresh
**Status:** `[APPROVED]` 2026-10-20

**Context:** D-013 planned to carry v1 data forward. The owner confirmed there is no data worth keeping.

**Decision:** Delete the v1 migrations (V1–V16) and the carry-over migration, and start with a single `V1__initial_schema.sql` containing only the v2 schema. Existing databases with v1 history must be reset before the v2 API can start against them: the local development database is reset by the engineer; the production (Neon) database is reset in Phase 3 after the owner confirms at that moment.

**Owner's words:** "you can delete previous migration and start new i dont mind. start fresh as there is no data."

---

## D-022 Customer portal accounts
**Status:** `[DECIDED]` 2026-10-20

**Context:** Customers need a way to see their plan, dues and receipts without staff access. The v1 portal let a member's refresh token mint a staff token and confused member and user IDs.

**Decision:** Portal accounts live on the customer record. Customers sign in with the business code plus email or phone and a password; their tokens carry a customer type and one business, and open only `/api/v1/portal/**`. Staff send an email invitation (7-day link). Self sign-up exists only when the business turns it on, and always creates a new customer, never linking to an existing one, because an unverified email or phone proves nothing. A per-request check makes disabling or archiving take effect at once. Sign-in has the same lockout, rate limits and single error message as staff sign-in.

**Alternatives:** Letting self sign-up attach to a matching customer (rejected: anyone who knows a phone number could read that person's invoices). Verifying email before use (kept for later; the new record is empty, so nothing is exposed). Using staff user accounts for customers (rejected: v1's root cause).

**Consequences:** `[OPEN]` A self-signed-up person can use an email or phone someone else owns, which blocks that person from being invited until staff disable the account. Email verification at sign-up would close this.

---

## D-023 Renewal reminders
**Status:** `[DECIDED]` 2026-10-20

**Decision:** One daily job emails customers three days before a membership ends, once per membership, only if they have an email and no later membership. Each business can switch it off (default on). The job marks a membership reminded only after the mail service accepted the email, so failures retry. Reminders go out through the existing Resend integration (no SMTP). WhatsApp and SMS remain out of scope (D-020).

---

## D-024 Signed-in screens render in the browser
**Status:** `[DECIDED]` 2026-10-21

**Context:** Next.js 16 with Cache Components prerenders a static shell for every route and, by default, checks that every navigation can show its UI instantly. The access token lives only in browser memory (D-009), so the server cannot render signed-in content.

**Decision:** Public pages (landing, sign-in and account pages) are prerendered, and the landing page declares `instant = true`. Everything under `/app` and `/portal` renders in the browser behind a session gate: the server ships the loading shell, the client restores the session from the refresh cookie and fetches data with TanStack Query. Those subtrees set `instant = false`, and instant-navigation validation runs only where a segment opts in (`experimental.instantInsights.validationLevel: 'manual-warning'`). A `proxy.ts` check sends visitors without the `mg_rt` cookie straight to sign-in.

**Alternatives:** A backend-for-frontend in Next.js that reads the cookie and renders signed-in pages on the server (rejected for now: it would put a second session layer between the browser and the API and double the attack surface; revisit if first-load speed of signed-in screens becomes a problem).

**Consequences:** The scaffold's Turbopack rule runs every `.css` file through Tailwind as global CSS, so the app does not use CSS modules; page-specific CSS lives in `globals.css` under names that can't collide with utilities.

---

## D-025 The OpenAPI document is the frontend's contract
**Status:** `[DECIDED]` 2026-10-21

**Decision:** The frontend's request and response types are generated from `/v3/api-docs` (`npm run api:types`). The API publishes a precise document: `BusinessContext` is hidden; every `/businesses/{businessId}` path declares `businessId`; responses are `application/json`; properties of response-only schemas are required (Jackson always writes them) and only fields marked `@Nullable` (`com.managio.platform.web.Nullable`) may be null; record names exposed by the API are unique, because springdoc merges schemas with the same simple name. `OpenApiContractTest` guards all of this.

**Consequences:** A response field that can be null must carry `@Nullable`, or the TypeScript types will claim it is always present. Roles are read with permission keys (`customers.view`) and saved with permission names (`CUSTOMERS_VIEW`); the frontend converts with `toPermissionName`.

---

## D-026 A slow-call warning is the one blanket interception
**Status:** `[DECIDED]` 2026-10-21

**Context:** The owner asked for AOP where it fits cross-cutting concerns. The activity log stays explicit (each event needs a meaningful summary), and transactions and security already use Spring's own proxies.

**Decision:** `SlowCallAspect` times public methods of `@Service` classes and logs a warning naming the method when one takes longer than `managio.slow-call-threshold` (default 500 ms). It never logs arguments or results, because they can hold customers' personal data.

---

## D-027 Frontend structure
**Status:** `[DECIDED]` 2026-10-21

**Context:** The owner asked for the v1 frontend to be removed and a fresh app created with the official Next.js CLI: "remove the current frontend entirely and build new" (2026-10-21).

**Decision:** `managio_frontend/` was recreated with `create-next-app@16.4.2` (TypeScript, Tailwind CSS 4, ESLint, App Router, `src/`, React Compiler). Code is organised by feature: `src/features/<area>/` holds that area's API hooks, pages and dialogs; `src/components/ui` the shared primitives; `src/components/shell` the signed-in frame; `src/lib` the API client, session store, formatting and form helpers. Dialog forms mount fresh each time they open, instead of resetting state in effects. Charts are drawn with plain SVG and CSS to one scale rather than with Recharts (this replaces the Recharts part of D-015).

---

## D-028 Platform console for Managio's administrators
**Status:** `[DECIDED]` 2026-10-21, scope `[APPROVED]`

**Context:** The owner asked whether Managio needs its own administrator dashboard to understand the platform as a business, separate from tenant administration. Managio does not charge businesses, so it has no plans, prices, subscriptions or SaaS revenue. The owner decided: "For V1, Managio will be completely free. Do not implement paid subscriptions, payment gateways, pricing management, or monetization workflows yet. Instead, focus the platform-admin dashboard on meaningful analytics ... Keep the architecture flexible so monetization can be added later" (2026-10-21).

**Decision:**
* A read-only console at `/admin` (API under `/api/v1/admin`), in its own backend module `modules/admin` and frontend feature `features/admin`, with its own frame. It shows: totals and growth (accounts, businesses, customers), activity (businesses that recorded something, people who used the app), activation (customer added, payment recorded, teammate added, active this week), business types, modules switched on, feature use, a business directory, a people directory and system health.
* **Who gets in:** an active account whose email is in `PLATFORM_ADMIN_EMAILS` *and* is verified, checked on every request by an interceptor on `/api/v1/admin/**`. There is no in-app way to grant it. Verification is required because anyone could sign up with a listed address they don't own. Refusals are 403; each use is logged with the administrator's user ID only.
* **Tenant isolation:** the console shows business metadata (name, type, owner contact for support) and counts. It never returns a business's customers, invoices, payments or amounts.
* Figures come from existing tables (activity log, sessions, sign-in events, Flyway history); "active" is defined in the response types and on screen. Days and weeks are counted in Asia/Kolkata, like the scheduled jobs.
* Monetization later: a future billing module can add plan and subscription data keyed by business and extend the tenant row and overview without changing the access model.

**Alternatives:** A separate admin login (rejected for now: more surface to secure for a read-only view; the allowlist plus verification is enough while the console cannot change anything). A role in the database granted from the app (rejected: an in-app grant is a privilege-escalation path). Requiring an authenticator-app code (deferred: worth adding before the console gains any action that changes data). Showing tenants' revenue (rejected: it is the businesses' private data).

---

## D-029 Backend packages: feature modules split into layers
**Status:** `[DECIDED]` 2026-10-21

**Context:** The owner asked for feature-oriented packages with controller, DTO, service, repository and domain separation, without pointless folders.

**Decision:** Features live in `com.managio.modules.<feature>` (`activity`, `admin`, `attendance`, `auth`, `billing`, `business`, `catalog`, `customers`, `daybook`, `expenses`, `memberships`, `notifications`, `payroll`, `portal`, `reports`, `requests`, `tasks`, `team`), each with only the layer packages it needs: `controller`, `dto`, `service`, `repository` (Spring Data repositories and JdbcClient read models), `domain` (entities, enums, value objects) and `web` (interceptors). Shared infrastructure stays in `com.managio.platform` (`config`, `error`, `mail`, `observability`, `persistence`, `security`, `tenancy`, `util`, `web`). The old `operations` package was split into tasks, requests, day book and notifications; payroll left `team`; attendance and groups left `customers`. `ArchitectureTest` (ArchUnit) enforces: controllers are used by nothing, services only by controllers and services, repositories only by services; domain classes depend on no other layer; platform code never depends on a feature; controllers, entities, repositories and services live in their layer packages. A controller may use the filter enums and row records declared inside a read-model query class, but reaches the query only through a service.

**Rationale:** Layers make each class's job obvious and the rules keep them honest without code review. Feature modules keep related code together.

---

## D-030 Deployment artifacts and secure defaults
**Status:** `[DECIDED]` 2026-10-21

**Decision:**
* The API and the web app each have a production Docker image (multi-stage, non-root user, health check). The web image uses Next.js standalone output; its `/api` target (`API_ORIGIN`) is fixed when it is built.
* `docker-compose.yml` runs the v2 stack (PostgreSQL 17, API, web) for a production-like local check. Only the web port is published. Settings come from a git-ignored `.env` with a `MANAGIO_` prefix; nothing secret is in the file.
* The production target stays Vercel (web) → Render (API, Docker) → Neon (PostgreSQL) per the architecture; the Jenkinsfile builds, tests and pushes both images. (Its Kubernetes rollout step was replaced by D-031.)
* API documentation (`/v3/api-docs`, Swagger UI) is off unless `API_DOCS_ENABLED=true`, which local development and tests set.
* The Maven wrapper pins the SHA-256 of the Maven distribution it downloads.

**Alternatives:** Keeping the v1 Compose file with nginx (rejected: it used v1 settings, Redis and inline credentials; the web app already proxies `/api`).

---

## D-031 One release pipeline: Jenkins tests and releases, Render runs the tested image, Vercel builds on Jenkins' signal
**Status:** `[DECIDED]` 2026-10-22 (engineer-decided under the owner's instruction to pick one coherent route)

**Context:** The Jenkinsfile built and pushed images and then restarted a Kubernetes deployment that isn't in this repository, while the documented target was Render and Vercel, each able to deploy straight from GitHub. Two half-routes meant nothing guaranteed that what reached production had passed the tests, or that the web app never ran ahead of the API.

**Decision:**
* Jenkins (the owner's, on Windows; the pipeline also runs on Linux) is the only path to production. It polls GitHub, runs the API and web checks, builds both images tagged `<commit>-<build>`, pushes them to Docker Hub, deploys that exact API image to Render through its deploy hook (`imgURL`), waits for readiness, triggers the Vercel deploy hook for master, waits for the site and smoke-tests it.
* Render runs an image-backed service with auto-deploy off. Vercel keeps building previews of other branches; `managio_frontend/vercel.json` turns off production deploys on push.
* Rollback: re-run the job with `ROLLBACK_TO_TAG` for the API; Vercel's Instant Rollback for the web app; a Neon branch or restore point for the database.
* The Kubernetes step is removed.

**Alternatives:** Render and Vercel each auto-deploying from GitHub with Jenkins as a side check (rejected: production could run untested code and the two apps deploy in any order). Deploying the web app with the Vercel CLI from Jenkins (rejected for now: needs a Vercel token on the build machine; the deploy hook needs nothing but a URL). Keeping Kubernetes (rejected: no cluster or manifests exist for v2).

**Consequence:** Production deploys stop when the owner's Jenkins is off. A Jenkins on a laptop can't receive GitHub webhooks, so it polls every five minutes.

---

## D-032 Client addresses: never trust X-Forwarded-For; the web app vouches with a shared secret
**Status:** `[DECIDED]` 2026-10-22

**Context:** Rate limits keyed on the first `X-Forwarded-For` address (Spring's `framework` strategy). Anyone could send that header, directly to the API or through the web app, and get a fresh limit for every forged address (verified 2026-10-21). Behind Vercel the connecting address is Vercel's, so ignoring the header alone would put every visitor in one bucket.

**Decision:**
* The API ignores `X-Forwarded-*` (`server.forward-headers-strategy: none`). `ClientAddressFilter` resolves the address once per request: the value of `X-Managio-Client-Ip` if the request carries the shared `API_PROXY_SECRET` in `X-Managio-Proxy-Secret` (compared in constant time) and the value is an IP literal; otherwise the connecting address. Rate limits, sign-in records, sessions and the activity log all use it.
* The web app's `src/proxy.ts` removes those two headers from every incoming `/api` request and, when it knows a trustworthy visitor address, adds them back with the secret. On Vercel that is `x-forwarded-for`, which Vercel overwrites; self-hosted, only the header named in `CLIENT_IP_HEADER`.
* Rate limits group IPv6 addresses by /64. Password-reset emails are also limited per recipient (3 an hour), and current-password checks per account.

**Alternatives:** Trusting known proxy IP ranges (rejected: Vercel's egress addresses aren't a fixed list). Rejecting requests without the secret (rejected: health checks and the owner's direct API checks need the API reachable; unverified requests are simply rate-limited by their connection). A shared rate-limit store such as Redis (rejected for v1: one API instance runs; counters reset on restart, while lockout is stored in the database).

---

## D-033 Idempotency keys for actions that create money records
**Status:** `[DECIDED]` 2026-10-22

**Context:** The UI blocks double clicks, but a retry after a lost response, or two tabs, could record a partial payment, refund, sale or expense twice. v1 is free and has no payment gateway; this is about the business's own bookkeeping.

**Decision:** `POST` invoices, sales, payments, refunds, expenses, payroll payments, memberships and renewals accept an optional `Idempotency-Key` header (8–100 of `A-Z a-z 0-9 _ -`). `IdempotentRequests` claims the key in `idempotency_keys` (unique per business, user and key) in the same transaction as the action and stores the response. A repeat waits for the first request to finish and gets its response; a failed request leaves no claim; reusing a key for a different request answers 422 `idempotency.key_reused`. Keys are deleted after a week by the nightly cleanup. The web app sends one key per form until it succeeds (`useSubmissionKey`). Requests without a key behave as before.

**Alternatives:** A unique client reference column on each money table (rejected: one mechanism per table and sales span two tables). Relying on the invoice row lock (rejected: it stops overpaying, not a second partial payment).

---

## D-035 Public site: legal pages, search engines and brand
**Status:** `[DECIDED]` 2026-10-22 (operator, contact address and free status `[APPROVED]` by the owner the same day)

**Decision:**
* Four plain-language public pages, `/privacy`, `/terms`, `/security` and `/contact`, written from what the code does (LEGAL.md maps each claim to its source). The operator is "Nitin, an individual based in India"; the contact is `nitinr0306@gmail.com` (the owner's choice). No cookie banner: Managio sets only strictly necessary cookies and runs no analytics or advertising.
* Search engines see only the public pages: canonical URLs on `https://managio.me`, a sitemap of those five pages, `noindex` on sign-in style pages, `noindex, nofollow` plus `robots.txt` exclusions for `/app`, `/admin`, `/portal` and the API, and a permanent `www` to apex redirect. Structured data limited to what the page shows (`WebSite`, `SoftwareApplication`, free). No ratings, reviews, customer counts or testimonials anywhere.
* A logo mark (white "m" on cobalt) with favicon, Apple and manifest icons and a sharing image, generated from one SVG by `scripts/brand-assets.mjs`. The landing illustration is labelled "Example".
* The public footer carries help (Contact, Security) and legal (Privacy Policy, Terms of Service) links only; sign-in and account creation stay in the header and on the page (owner's instruction).

**Alternatives:** Designing the mark in Figma or Stitch (rejected: Figma has a view-only seat here and Stitch produces screen mockups, not vector marks; a hand-written SVG is exact and tiny). Generating the sharing image at request time with `next/og` (rejected: a static PNG is simpler, cacheable and identical).

---

## D-034 Google sign-in follows the same account rules as a password
**Status:** `[DECIDED]` 2026-10-22

**Decision:** A disabled account is refused and a locked account (5 failed password attempts, 15 minutes) stays locked for Google sign-in too; both outcomes are recorded as sign-in events. When Google is linked to an existing account whose email was never verified, that account's password is removed and its sessions end: whoever set it never proved they own the address (account pre-hijacking). A verified account keeps its password. A second Google account with the same email is refused (409 `google.other_account`). The ID token's RS256 signature, issuer, audience, expiry and `email_verified` are checked and covered by tests with locally signed tokens.

**Consequence:** Someone who can guess a person's email can keep them out for 15 minutes at a time by failing password sign-ins, now including Google sign-in; per-address rate limits slow that down, and the owner asked for lockout to apply to every sign-in method.

---

## D-036 ₹0 hosting: the API and PostgreSQL on one Oracle Cloud Always Free VM
**Status:** superseded by D-037 on 2026-10-22 (the owner chose Render, Supabase and Vercel). Originally `[DECIDED]` 2026-10-22, on the owner's instructions: a ₹0 monthly budget, continuous availability and few cold starts first, and "change neon db also". Replaces the Render part of D-031 and Neon as the production database. Nothing has been created yet: executing it needs the owner's approval for Oracle's sign-up card check, for the `api.managio.me` DNS record (or the no-DNS fallback), and for replacing the live v1 site on Vercel.

**Context:** Measured on 2026-10-22 with the production API image on this PC, under a 512 MB limit like Render's: ready in 18 s with 2 CPUs, 60 s with 0.5 CPU (Render's cheapest paid plan, about $7 a month), 338 s with 0.1 CPU (Render Free) and 218 s with 0.1 CPU and start-up JVM flags; peak memory 406–455 MiB. Render Free sleeps after 15 minutes without traffic, Vercel stops waiting for the API after 120 s, and Render asks that free services not be kept awake artificially, so on Render Free the first visits after every quiet spell would fail for minutes and the 09:00 renewal reminders would run only when the service happened to be awake. Neon Free gives 100 CU-hours a month (400 hours at 0.25 CU) and suspends the database for the rest of the month when they run out; the API's pool keeps one connection open (`minimum-idle: 1`), which would keep Neon awake around the clock (about 186 CU-hours) behind an always-on API.

**Decision:**
* One VM, `VM.Standard.A1.Flex` (Arm), 2 OCPUs and 3 GB, Ubuntu 24.04, in the owner's existing tenancy's home region, **Mumbai** (`ap-mumbai-1`), on the Always Free allowance (2 OCPUs and 12 GB in total). It never sleeps. Oracle allows Always Free compute only in the home region, and the home region can't be changed (checked 2026-10-22); Singapore, first planned to sit next to Neon, no longer matters now that the database is on the VM, and Mumbai is closer to Indian users. Mumbai has one availability domain and its Arm capacity is often exhausted: the owner's attempt failed, and the fallbacks are in runbook section 5.
* Docker Compose runs PostgreSQL 17, the API image and Caddy (`deploy/oracle/`). Caddy is the only published service and gets the HTTPS certificate for `api.managio.me` (one new A record) or, without a DNS change, `<ip-with-dashes>.nip.io`. The database is never published.
* Backups: `deploy/oracle/backup.sh` nightly (`pg_dump`, checked by reading it back, 14 days kept on the VM), Oracle's block volume backups (5 included in Always Free) for a copy off the VM, and a periodic copy to the owner's PC for a copy outside Oracle.
* The web app stays on Vercel (Hobby); `API_ORIGIN` becomes `https://api.managio.me`. Resend, Google sign-in, Docker Hub and Jenkins stay.
* The Dockerfile builds the jar on the builder's own platform, so one `docker buildx build --platform linux/amd64,linux/arm64` tag runs on the PC and on the Arm VM. Jenkins keeps testing and building; its Render stage is to be replaced by a call to `deploy.sh` over SSH once the VM exists. Until then, `deploy.sh <tag>` is run by hand.
* Neon is left untouched (nothing is deleted). If it is ever used again, the API needs `SPRING_DATASOURCE_HIKARI_MINIMUMIDLE=0` so Neon can suspend (verified: zero connections about 3 minutes after the last request, and health checks open none).

**Alternatives:** Render Free (no card, but sleeps, 0.1 CPU, start-up measured at 218–338 s). Render Starter (no sleep, 60 s start-up, but about $7 a month). Google Cloud Run (needs a billing account; its free allowance applies in North American regions, far from India; scales to zero, so the 09:00 job wouldn't run). Fly.io (no free tier for new organisations; a card after a short trial). Koyeb (no free web services). Hugging Face Spaces (new Docker Spaces need a paid plan). The owner's PC behind a tunnel (only up while the PC is on, which has little free memory). Keeping Neon behind the VM (managed storage and restore, but the 100 CU-hour cap and a cold database after 5 idle minutes). Supabase Free (500 MB, paused after a week of inactivity, no backups). Aiven's free PostgreSQL (no card, but may be powered off after inactivity). Oracle's `VM.Standard.E2.1.Micro` (AMD, 1/8 OCPU, 1 GB) is the fallback if Arm capacity isn't available.

**Consequence:** One VM is a single point of failure with no SLA: an Oracle outage, maintenance reboot or hardware fault takes Managio down until the VM is back, and data written after the last backup could be lost if the VM's disk were lost. The owner maintains the VM (security updates, Docker, disk space). Oracle needs a card at sign-up to verify identity and says it isn't charged unless the account is upgraded; the account must stay on Free Tier. Oracle may reclaim an Always Free VM that is idle by its definition (95th-percentile CPU, network and, on Arm, memory all under 20% for 7 days); the 3 GB size keeps memory use above 20%. A deploy restarts the API, so the API is unavailable for roughly its start-up time. Vercel's Hobby plan is for personal, non-commercial use. The privacy policy's provider list now names Oracle Cloud Infrastructure instead of Render and Neon.

---

## D-037 Final stack: Vercel Free, Render Free and Supabase Free
**Status:** `[APPROVED]` 2026-10-22. The owner: "The final deployment stack for Managio is: Backend: Render Free; Database: Supabase; Frontend: Vercel." Supersedes D-036, and Neon as the database. Restores D-031's Render deploy, which the Jenkinsfile already implements.

**Context:** The Oracle Always Free VM (D-036) could not be created: the owner's tenancy is in Mumbai, which had no Arm capacity. Always Free compute is home-region only, and a second free account isn't allowed. Neon Free's 100 CU-hours a month could run out for an app used all day. Supabase Free has no compute-hour cap and pauses only after a week of inactivity.

**Decision:**
* **Web app:** Vercel Hobby (the existing `managio.me` project, root directory `managio_frontend`). `API_ORIGIN` is the Render URL.
* **API:** a Render Free web service from the existing image `docker.io/nitinr1103/managio-backend:<tag>`, in Singapore, with health check `/actuator/health/readiness`. It uses a read-only Docker Hub credential, no pre-deploy command, and `JAVA_TOOL_OPTIONS=-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xss512k`, which cut the measured start-up at 0.1 CPU from 338 s to 218 s.
* **Database:** a Supabase Free project in Singapore, reached through the **session pooler**: port 5432, user `postgres.<project-ref>`, `sslmode=require`. Session mode supports Flyway and prepared statements; the direct connection is IPv6-only on Free. **The Data API is turned off**, because Supabase exposes tables in `public` to it by default. Flyway creates the schema on the first start; production starts empty (D-021).
* **Keep-alive:** a free external scheduler calls `/actuator/health/liveness` every 10 minutes from 08:00 to 22:00 India time (about 434 of the 750 free hours a month). That endpoint doesn't touch the database. This was the owner's explicit choice, after being told that Render's guidance calls keeping a free service awake artificially abuse of the free tier.
* **Backups:** weekly `pg_dump` from the owner's PC (Supabase Free has no backups).
* **Unused:** Neon is left untouched. `deploy/oracle/` and the Dockerfile's multi-platform build stay for a possible later move.

**Alternatives:** the Oracle VM (D-036: blocked by capacity in the home region); Neon (compute-hour cap); Render Starter or other paid plans (not ₹0); no keep-alive (the first visit after every 15 quiet minutes could fail).

**Consequence:**
* Render Free may restart the service at any time.
* Outside the pinged hours, the first request after a sleep can fail behind Vercel's 120 s limit, and the 03:30 clean-up job may not run.
* Render may act against the keep-alive pings; if the free hours run out, every free Render service is suspended until the next month.
* Supabase Free: 500 MB of database, 5 GB egress, no backups; a paused project must be restored by hand.
* Vercel Hobby is for personal, non-commercial use.
* The privacy policy now names Render and Supabase.
