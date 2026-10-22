# Testing

How Managio is tested and how to run each suite. Results of the latest runs are in [STATUS.md](STATUS.md).

## Databases `[DECIDED]`

Tests and local development use PostgreSQL 17 in Docker, never Neon (the owner's rule, 2026-10-22):

* **Backend tests** start a throwaway PostgreSQL in Testcontainers; nothing is configured and nothing persists.
* **Local development and end-to-end runs** use the container `managio-dev-db` on port 5433 (`docker start managio-dev-db`). Point `DB_URL` in `managio_backend/.env` at `jdbc:postgresql://localhost:5433/managio`.

H2 is not used: the schema and queries rely on PostgreSQL features H2 lacks (`INSERT … ON CONFLICT`, `text[]` columns, `generate_series`), and tests on another database engine wouldn't show that the production database works.

## Backend (`managio_backend`)

| Suite | What it covers |
|---|---|
| Unit tests | Rules that need no database: invoice maths, the slow-call aspect. |
| Integration tests (`IntegrationTest` subclasses) | Every API workflow through HTTP (MockMvc) against PostgreSQL 17 in Testcontainers with the real Flyway migrations. Each test starts from empty tables and a test clock fixed at 10:00 IST on 20 October 2026. Emails go to a recording mailer. |
| `TenancyIsolationTest`, `AdminTest` | Cross-tenant isolation for every role, and who may use the platform console. |
| `AccessSweepTest` | Walks every registered `/api/v1/businesses/{businessId}/**` route: an outsider gets 404 on all of them and changes nothing; a member whose role has no permissions gets 403 on every route that requires one; a manager holding every permission still can't archive the business, manage roles, or act on the owner. |
| `ClientAddressTest` | Forged `X-Forwarded-For` doesn't escape rate limits; only the web app's secret can name the client address; IPv6 /64 grouping; sign-in records keep the resolved address (D-032). |
| `GoogleIdentityVerifierTest` | Real RS256 Google-style tokens signed by a local key: wrong audience, issuer, signature, expiry, unsigned and HMAC tokens are refused (D-010). `AuthFlowTest` covers the account rules after a verified token (lockout, disabled, pre-hijacking, conflicts; D-034). |
| `IdempotencyTest` | Retried, concurrent (six at once) and mismatched `Idempotency-Key` requests for payments, sales, refunds and expenses (D-033). |
| `ProductionDefaultsTest` | An unset variable falls back to the safe production value (secure cookies, no email-body logging, no API docs, normal rate limits, no forwarded headers, port 8081), and the refresh cookie is `Secure; HttpOnly; SameSite=Lax` when secure cookies are on. |
| `MigrationTest` | Migrations apply cleanly to an empty database. |
| `OpenApiContractTest` | The OpenAPI document matches what the frontend's generated types assume (D-025). |
| `ArchitectureTest` | Package and layer rules (D-029). |

Run: `./mvnw verify` (Docker must be running). One class: `./mvnw test -Dtest=AdminTest`.

## Web app (`managio_frontend`)

| Command | What it checks |
|---|---|
| `npm run typecheck` | `next typegen` then TypeScript, so it also works on a clean checkout without a build (Jenkins runs it before building). Includes the types generated from the API (`npm run api:types` regenerates them from a running API with `API_DOCS_ENABLED=true`; `scripts/api-types.mjs` works on Windows, macOS and Linux). |
| `npm run lint` | ESLint, including the React Compiler rules and a ban on non-null assertions. |
| `npm test` | Vitest and Testing Library: session client, formatting, errors, forms, navigation by permission, components, the `/api` proxy's address hand-off (`src/proxy.test.ts`) and idempotency keys. |
| `npm run build` | Production build. |
| `npm run e2e` | Playwright end-to-end tests (below). |

## End-to-end tests (`managio_frontend/e2e`)

They drive a production build of the web app (`npm run build && npm run start`, started by Playwright unless a server is already on port 3000) against a real running API and database.

Prerequisites:

1. The API running on `http://localhost:8081` against the local database (above) with `MAIL_LOG_CONTENT=true`, `RESEND_API_KEY` empty (so no real email is sent to test addresses), its output written to a log file, a raised `RATE_LIMIT_MULTIPLIER` (the suite signs up many accounts) and `platform.admin@example.com` in `PLATFORM_ADMIN_EMAILS`.
2. `E2E_API_LOG` set to that log file, so tests can follow emailed links (verification, reset, invitations). Without it those tests are skipped.

```
E2E_API_LOG=/path/to/api.log npx playwright test            # desktop and phone projects
E2E_API_LOG=/path/to/api.log npx playwright test e2e/admin.spec.ts
```

| Spec | Workflows |
|---|---|
| `owner-day.spec.ts` | Owner onboarding, customers, plans, payments, receipts. |
| `money-and-operations.spec.ts` | Renewals and dates, refunds, voiding, attendance, payroll, profit and loss, settings, activity log, double-submit protection. |
| `team-and-portal.spec.ts` | Invitations and role restrictions, tasks, requests approved and rejected, customer portal. |
| `isolation-and-sessions.spec.ts` | Two tenants can't see each other, portfolio, sign-out leaves nothing behind, expired sessions, blocked direct links. |
| `screens.spec.ts` | Password reset, customer list tools, renewals, catalogue, second business, batches, custom roles, portal self sign-up. |
| `public-and-mobile.spec.ts` | Public pages and the phone menu. |
| `admin.spec.ts` | Platform console: refusal for non-administrators (screen and API), sign-in redirect, real figures, directories, health. |
| `seo-and-legal.spec.ts` | Public pages: unique titles, canonical URLs, sharing cards, footer with help and legal links only; structured data matches the page; `robots.txt`, sitemap and manifest; `noindex` on sign-in pages and private areas (including the API); `www` and trailing-slash redirects; legal pages name the operator and contact; sign-up links to the Terms and Privacy Policy. |
| `sweep.spec.ts` | Every screen on desktop (1440 px), tablet (768 px) and phone (390 px), including the legal pages: heading renders, no console errors, no failed API calls, no sideways scrolling, no serious or critical axe (WCAG 2.1 AA) violations. Writes `e2e-results/screen-sweep.json`. Console errors from Google's sign-in library itself (`accounts.google.com/gsi/`) are ignored: Google answers one of its own requests without a CORS header. |

With a Google client ID configured, the sign-in pages also show the Google button, so tests name the password button exactly (`{ name: "Sign in", exact: true }`). A real Google sign-in needs a person's Google account and is a manual check (DEPLOYMENT.md, smoke test).

Every run creates its own people and businesses (unique suffix), so the suite can run repeatedly against the same development database.
