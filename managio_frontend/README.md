# Managio web app

The Next.js frontend for Managio: the landing page, sign-in, the signed-in app for owners and teams, the
customer portal and the platform console for Managio's administrators (`/admin`, D-028). Product rules, architecture and decisions live in [`../docs`](../docs/README.md); read
`docs/STATUS.md` first.

## Run it

1. Start the API (see `docs/STATUS.md`, "Environment facts"); by default it listens on `http://localhost:8081`.
   For `npm run api:types` it needs `API_DOCS_ENABLED=true`.
2. Copy `.env.example` to `.env.local` and adjust if needed.
3. `npm install`, then `npm run dev` and open http://localhost:3000.

The browser only talks to this app. Requests to `/api/*` are passed through to `API_ORIGIN`, so the refresh-token
cookies are first-party (docs/DECISIONS.md D-009).

## Scripts

| Command | What it does |
|---|---|
| `npm run dev` | Development server on port 3000. |
| `npm run build` / `npm run start` | Production build and server. |
| `npm run typecheck` | TypeScript, no output. |
| `npm run lint` | ESLint, including the React Compiler rules. |
| `npm test` | Vitest unit and component tests. |
| `npm run e2e` | Playwright end-to-end tests against the running API (starts a production build if no server is running). Setup: `docs/TESTING.md`. |
| `npm run api:types` | Regenerates `src/lib/api/schema.d.ts` from the running API's OpenAPI document. Run it after any API change. |

## Deploy

* **Vercel:** set `API_ORIGIN` (the API's URL) and `NEXT_PUBLIC_GOOGLE_CLIENT_ID`. Both are read at build time.
* **Docker:** `docker build --build-arg API_ORIGIN=http://api:8080 -t managio-web .` (standalone output, non-root,
  health check). The root `docker-compose.yml` runs it with the API and PostgreSQL.

Every setting is described in `docs/ENVIRONMENT.md`, and the rollout in `docs/DEPLOYMENT.md`.

## Where things are

| Path | Contents |
|---|---|
| `src/app` | Routes only. Each page renders a feature component. |
| `src/features/<area>` | One area of the product: its API hooks (`api.ts`), pages and dialogs. |
| `src/components/ui` | Shared primitives: buttons, fields, dialogs, tables, pills. |
| `src/components/shell` | The signed-in frame: sidebar, search, notifications. |
| `src/lib` | API client and sessions, formatting, form helpers. |
| `e2e` | Playwright specs. |

Conventions worth knowing before changing code:

* Signed-in pages render in the browser; public pages are prerendered (D-024).
* Types come from the API's OpenAPI document; don't hand-write response types (D-025).
* Every query key is scoped by business: `businessKey(id, ...)`.
* Dialog forms mount when the dialog opens, so they never need to reset state.
* No CSS modules: the Turbopack rule in `next.config.ts` treats every `.css` file as global Tailwind CSS.
* Money is formatted with `formatMoney` (Indian grouping) and business dates with `formatDate` (never through `Date` parsing).
