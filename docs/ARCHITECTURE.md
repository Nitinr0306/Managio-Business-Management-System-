# Architecture

How Managio v2 is built. Product behaviour lives in [PRODUCT.md](PRODUCT.md), the schema in [DATA_MODEL.md](DATA_MODEL.md), API rules in [API.md](API.md). Decisions referenced as D-xxx are in [DECISIONS.md](DECISIONS.md). Implementation progress and test evidence are in [STATUS.md](STATUS.md). Deployment is described in [DEPLOYMENT.md](DEPLOYMENT.md), settings in [ENVIRONMENT.md](ENVIRONMENT.md) and how to run the tests in [TESTING.md](TESTING.md).

## System shape `[DECIDED]`

```
Browser ──> Vercel: Next.js app ──(/api/* rewrite)──> Render: Spring Boot API ──> Supabase PostgreSQL (session pooler)
                                                              └──> Resend (email)
```

* The browser only talks to the Vercel origin (`https://managio.me`). Vercel rewrites `/api/*` to the API, so auth cookies are first-party (D-009). On the way, `src/proxy.ts` vouches for the visitor's address with a shared secret (D-032).
* Releases go through one Jenkins pipeline: tests, immutable images on Docker Hub, the API image deployed to Render through its deploy hook, then the Vercel build (D-031, [DEPLOYMENT.md](DEPLOYMENT.md)).
* Production costs ₹0 a month on free plans: Vercel, Render and Supabase (D-037). Render Free sleeps when idle, so a working-hours keep-alive and start-up JVM flags limit cold starts.
* One API deployable, a modular monolith packaged by feature and split into layers (D-004, D-029).
* Both apps ship as Docker images; `docker-compose.yml` runs the whole stack locally (D-030).

## Backend `[DECIDED]`

Spring Boot 4.1, Java 21, Spring Security with the OAuth2 resource server for JWT validation, Spring Data JPA (Hibernate 7) on PostgreSQL, Flyway, springdoc OpenAPI (D-003).

```
com.managio
├── platform             shared infrastructure; never depends on a feature
│   ├── config           settings (ManagioProperties), clock, password encoder, OpenAPI contract
│   ├── error            ApiException and the RFC 9457 problem handler
│   ├── mail             Mailer, Resend, templates
│   ├── observability    slow-call warning aspect
│   ├── persistence      persistence helpers, SQL text helpers
│   ├── security         JWT access tokens, refresh cookies, principals, rate limiter
│   ├── tenancy          business access interceptor, BusinessContext, permissions, modules
│   ├── util             money, CSV
│   └── web              paging, request IDs, input normalisation, @Nullable
└── modules              one package per feature
    ├── auth             accounts, sign-in, Google sign-in, sessions, one-time tokens
    ├── business         businesses, business types (templates), settings, onboarding, number sequences
    ├── team             team members, invitations, roles
    ├── payroll          monthly salaries
    ├── customers        customers, CSV import/export
    ├── attendance       check-ins, groups and batches
    ├── catalog          plans, services, products, stock
    ├── memberships      memberships, renewals, installments, reminders
    ├── billing          invoices, payments, refunds, sales
    ├── expenses         expense categories and expenses
    ├── daybook          daily cash count and the day lock
    ├── tasks            tasks and repeats
    ├── requests         requests and approvals
    ├── notifications    in-app notifications
    ├── activity         activity log
    ├── reports          Today view, reports, portfolio
    ├── portal           customer self-service API
    └── admin            platform console for Managio's administrators (D-028)
```

Each module has only the layer packages it needs (D-029):

* `controller`: HTTP mapping, input validation, permission annotations. No business logic. Used by nothing else.
* `dto`: request and response records for the module's API. Mapping is a static `from` method on the response record.
* `service`: transactions and business rules; takes the business ID explicitly. Used by controllers and other services.
* `repository`: Spring Data repositories (every tenant query takes the business ID) and `JdbcClient` read models (`XQueries`) that join across tables for lists and dashboards. Read models never write. Used only by services.
* `domain`: JPA entities with domain methods for state changes, enums and value objects. Depends on no other layer.
* `web`: request interceptors owned by the module (portal access, platform console access).
* Writes to another feature's data go through that feature's service.
* `ArchitectureTest` (ArchUnit) enforces these rules on every build.

## Security and tenancy `[DECIDED]`

* **Principals.** `UserPrincipal` and `CustomerPrincipal` are distinct types created from the token's `typ` claim (D-006).
* **Access tokens.** HS256 JWT, 15 minutes, issuer `managio`, claims `sub`, `typ`, and for customers `bid` (business ID). Validated by Spring Security's resource server.
* **Refresh tokens.** Random 256-bit, SHA-256 hashed at rest, rotated on use, reuse of a rotated token revokes the family. A successful sign-in (password, Google, sign-up, invitation, portal) ends the session whose cookie the browser sent with it, so a replaced session doesn't stay valid unused. Cookie `mg_rt` (users) and `mg_prt` (customers), `HttpOnly; Secure; SameSite=Lax; Path=/` (D-009).
* **Google sign-in.** Google Identity Services ID token (popup and JavaScript callback, so no redirect URI or client secret) verified against Google's JWKS: RS256 signature, issuer, audience = configured client ID, expiry (D-010). The account rules are the same as for a password: disabled and locked accounts are refused, and linking Google to an account whose email was never verified removes that account's password and sessions (D-034).
* **Tenancy.** `BusinessAccessInterceptor` handles every `/api/v1/businesses/{businessId}/**` request: loads the caller's active team membership, 404 if none, checks `@RequirePermission`, and exposes a request-scoped `BusinessContext` (D-008).
* **Passwords.** BCrypt. Lockout after 5 failed attempts for 15 minutes, recorded in its own transaction so it survives the failed request.
* **Client address** (D-032). `X-Forwarded-*` headers are ignored (`server.forward-headers-strategy: none`). `ClientAddressFilter` decides the address once per request: the `X-Managio-Client-Ip` value when the request also carries the shared `API_PROXY_SECRET` in `X-Managio-Proxy-Secret` (constant-time comparison, IP literals only), otherwise the connecting address. The web app's proxy drops both headers from what clients send and adds them itself, using the address Vercel puts in `x-forwarded-for` (or the header named in `CLIENT_IP_HEADER` when self-hosted). Rate limits, sign-in records, sessions and the activity log all see the resolved address.
* **Rate limiting.** In-memory fixed windows (`RateLimiter`; one API instance, at most 100,000 counters per policy, reset on restart): per client address, IPv6 grouped by /64, sign-in 10/min (password and Google), sign-up 5/10 min, email-sending requests 5/15 min, refresh 60/min; per recipient, 3 password-reset emails an hour; per account, 5 current-password checks per 15 min. Lockout is per account and stored in the database.
* **Idempotency** (D-033). Money-creating `POST`s accept `Idempotency-Key`; `IdempotentRequests` claims the key in `idempotency_keys` within the action's transaction and replays the stored response for repeats, including concurrent ones. Payments, refunds and voids also lock their invoice row, so the balance can't be overdrawn.
* **Email.** Resend is called on a small background pool, so a request never waits on the provider and a password-reset request takes the same time whether or not the account exists.
* **Platform console.** `/api/v1/admin/**` is guarded by `AdminAccessInterceptor`: the caller must be an active account whose email is in `PLATFORM_ADMIN_EMAILS` and verified, checked on every request (D-028).
* **API documentation.** `/v3/api-docs` and Swagger UI are off unless `API_DOCS_ENABLED=true` (D-030).
* **CORS and CSRF.** The browser calls its own origin, but the pass-through forwards its `Origin` header, so `CORS_ALLOWED_ORIGINS` lists the web app's origin (`https://managio.me`). A request from any other origin is refused with 403, which also stops a foreign site from using the refresh cookie. The other CSRF defences: access tokens travel in a header, the cookie is `SameSite=Lax`, and endpoints accept only JSON (a form post gets 415).
* **API response headers.** Spring Security's defaults (`nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store`) plus `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'` on `/api/**` and `Referrer-Policy: no-referrer`.

## Time and money `[DECIDED]`

* Instants are `Instant`/`timestamptz`. Business dates are `LocalDate` in the business time zone (default `Asia/Kolkata`). A `Clock` bean is injected everywhere "now" or "today" is used, so tests control time.
* Money is `BigDecimal` with scale 2, `RoundingMode.HALF_UP`, in the business currency (INR by default).

## Cross-cutting behaviour `[DECIDED]`

* **Errors.** RFC 9457 problem details with a stable `code` (see [API.md](API.md#errors-decided)). Unexpected errors are logged once with the request ID and returned without internal detail.
* **Logging.** Structured console logs with request ID (from `X-Request-Id` or generated) in the MDC. No tokens, passwords or full card/bank details in logs. Request duration is logged per request. The one blanket interception is `SlowCallAspect`, which warns when a service method takes longer than 500 ms and never logs arguments (D-026).
* **Activity log.** Services record activity events explicitly for money, permission and lifecycle changes. Explicit calls are used instead of AOP so each event has a meaningful summary.
* **Email.** `Mailer` interface; `ResendMailer` in production, `LoggingMailer` when no API key is set. Emails are sent after the transaction commits (D-017).
* **Scheduling.** Daily jobs for renewal reminders and token cleanup. The API runs as a single instance; jobs are idempotent so a duplicate run is harmless.

## Frontend `[DECIDED]`

Next.js 16.4 App Router with Cache Components and the React Compiler, React 19, TypeScript, Tailwind CSS 4, TanStack Query, React Hook Form with Zod, Radix primitives (`radix-ui`), lucide icons, Sonner toasts (D-015, D-027).

* **Layout of the code.** `src/app` holds routes only; each page renders a component from `src/features/<area>` (admin, auth, business, customers, memberships, billing, operations, team, reports, portal, today, marketing). Each feature has an `api.ts` of typed query and mutation hooks. Shared primitives are in `src/components/ui`, the signed-in frame in `src/components/shell`, and the API client, session store, formatting and form helpers in `src/lib`.
* **Routes.** Public: `/` (landing), `/sign-in` (Variant B role tiles), `/sign-up`, `/forgot-password`, `/reset-password`, `/verify-email`, `/invite`. Staff app: `/app` (portfolio for 2+ businesses, otherwise straight in), `/app/new`, `/app/account`, and `/app/[businessId]/…`: Today, customers, renewals, billing (and `billing/invoices/[invoiceId]`), expenses, day-book, tasks, requests (and `requests/[requestId]`), attendance, catalog, team, payroll, reports, activity, settings. Portal: `/portal`, `/portal/[code]`, `/portal/[code]/set-password`. Platform console: `/admin` (overview), `/admin/businesses`, `/admin/people`, `/admin/health` (D-028).
* **Rendering.** Public pages are prerendered; `/app`, `/admin` and `/portal` render in the browser behind a session gate (D-024). `src/proxy.ts` redirects `/app` and `/admin` to sign-in when the `mg_rt` cookie is missing (a shortcut, not a security control: the API decides), and signs `/api` requests with the visitor's address (D-032). The console shows nothing of itself until `GET /api/v1/admin/access` confirms the person is an administrator.
* **API access.** The browser calls `/api/*` on its own origin; `next.config.ts` rewrites it to `API_ORIGIN` (default `http://localhost:8081`). Types come from the OpenAPI document (D-025). `src/lib/api/client.ts` holds two session clients, staff and portal: each attaches its access token, refreshes it shortly before expiry or once after a 401 (one refresh shared by concurrent requests), retries, and signs out when the refresh cookie is refused. Every failure becomes an `ApiError` with the problem's `code` and field errors.
* **State.** Server state lives in TanStack Query, with keys scoped by business (`["business", id, …]`) so switching business never shows another business's data. The only client state is the in-memory session (`useSyncExternalStore`) and the current business from the URL. Signing out, in any tab (BroadcastChannel), clears the query cache.
* **Permissions.** `useBusiness()` exposes `can(permission)` and `has(module)` from `/me/businesses`. The sidebar, actions and form fields follow them, worded with the business type's labels (Members, Clients, Students...). The API remains the authority.
* **Design.** Variant A "Runway" tokens in `globals.css` (`@theme`): canvas, ink, cobalt, and green/amber/red status colours that mean the same thing everywhere. Public Sans via `next/font`. Tables first; one customer panel (Variant B) opened from search, tables and Today; the portfolio (Variant C) at `/app`.
* **Security headers.** On every response: a Content Security Policy that allows only this origin and Google Identity Services (`accounts.google.com/gsi/` for the script, its styles, the button frame and its calls), `frame-ancestors 'none'`, `object-src 'none'`, `base-uri 'self'`, `form-action 'self'`; `Strict-Transport-Security: max-age=63072000; includeSubDomains`; `Cross-Origin-Opener-Policy: same-origin-allow-popups` (Google's popup); `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy`, `Permissions-Policy`; no `X-Powered-By`. Scripts keep `'unsafe-inline'` because Next.js inlines its page data and a per-request nonce would end the prerendering of public pages; the only raw HTML is the landing page's JSON-LD block, built from constants with `<` escaped, so React's escaping remains the defence against injected script. Zod runs `jitless` (`src/lib/zod-config.ts`) so it never probes `new Function`, which the policy forbids.
* **Public site and search engines** (D-035). Public pages: `/`, `/privacy`, `/terms`, `/security`, `/contact`, each with its own title, description, canonical URL on `https://managio.me` and sharing card (`publicPageMetadata` in `src/lib/site.ts`; image `src/app/opengraph-image.png`). `robots.ts` disallows `/app`, `/admin`, `/portal`, `/api/`; `sitemap.ts` lists only the public pages; sign-in style pages are `noindex, follow` (the `(auth)` layout and an `X-Robots-Tag` header); `/app`, `/admin`, `/portal` are `noindex, nofollow` (layout metadata and header), and the API sends `X-Robots-Tag: noindex, nofollow` itself. `www.managio.me` redirects permanently to `managio.me`. The landing page carries `WebSite` and `SoftwareApplication` (free) structured data only.
* **Brand.** The mark is a white "m" on a cobalt rounded square (reversed on dark backgrounds): `src/app/icon.svg`, the `Logo`/`LogoMark` components in `src/components/brand/logo.tsx`, next to the lowercase Public Sans wordmark. `scripts/brand-assets.mjs` renders the favicon, Apple and manifest icons and the sharing image from the same geometry. Public pages share `SiteHeader` and `SiteFooter` (`src/components/site/site-chrome.tsx`); the footer holds help and legal links only.
* **Money forms** send an `Idempotency-Key` (`useSubmissionKey`): the same key until the action succeeds, a new one after.

## Testing `[DECIDED]`

* Backend unit tests for rules (dates, money, installments, permissions, day book).
* Backend integration tests on PostgreSQL in Testcontainers with real Flyway migrations, including the v1 → v2 data migration and cross-tenant isolation for every role (D-016).
* Frontend unit and component tests with Vitest and Testing Library (`npm test`): session client refresh and sign-out, formatting, errors, form error mapping, permission-based navigation, key components.
* Playwright end-to-end tests of each workflow against the real API and a production build of the web app (`npm run e2e`, specs in `managio_frontend/e2e`). Flows that follow an emailed link read the token from the API's log (`E2E_API_LOG`). A sweep visits every screen on desktop and phone sizes and fails on console errors, failed API calls, sideways scrolling or serious axe accessibility violations.
* `ArchitectureTest` keeps the package rules (D-029); `OpenApiContractTest` keeps the API contract (D-025).
* Details and commands: [TESTING.md](TESTING.md).
