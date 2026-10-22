# Environment

Every setting Managio reads, where it is set and what is safe. `[VERIFIED]` 2026-10-22 against `application.yml`, `next.config.ts`, `src/proxy.ts`, `docker-compose.yml`, `Jenkinsfile` and the example files.

## Rules

* Secrets live only in untracked files (`.env`, `managio_backend/.env`, `managio_frontend/.env.local`) or in the host's settings (Render, Vercel, Jenkins credentials). `.gitignore` ignores every `.env*` file except `.env.example`.
* Each runtime has an example with placeholders: `managio_backend/.env.example`, `managio_frontend/.env.example`, and `.env.example` at the root for Docker Compose. Values in `REPLACE_WITH_...` form must be replaced.
* Nothing secret goes in a `NEXT_PUBLIC_*` variable: those are compiled into JavaScript that every visitor downloads.
* Logs never contain passwords, tokens or secrets. Email bodies (which contain single-use links) are logged only when `MAIL_LOG_CONTENT=true`, which is for local development.

## API (`managio_backend`)

Read from environment variables; locally also from the untracked `managio_backend/.env`.

| Variable | Required | Default | Secret | Purpose |
|---|---|---|---|---|
| `DB_URL` | Yes | `jdbc:postgresql://localhost:5432/managio` | No | JDBC URL. Locally the PostgreSQL container on 5433 (TESTING.md). Neon: the **direct** host, `jdbc:postgresql://<host>/<db>?sslmode=require`; not the `-pooler` host, because Flyway's session lock doesn't work through Neon's pooler. |
| `DB_USERNAME` | Yes | `managio_user` | No | Database user. |
| `DB_PASSWORD` | Yes | empty | **Yes** | Database password. |
| `DB_POOL_SIZE` | No | `5` | No | Connection pool size; keep small on Neon and Render free tiers. |
| `JWT_SECRET` | Yes | none: start-up fails | **Yes** | Signs access tokens (HS256). At least 32 random characters. Changing it signs everyone out. |
| `SECURE_COOKIES` | No | `true` | No | Refresh cookies are `Secure`. Only plain-http local setups set `false`. |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:3000` | No | Comma-separated origins allowed to call the API from a browser. The web app proxies `/api`, so normally only its own URL. |
| `RATE_LIMIT_MULTIPLIER` | No | `1` | No | Multiplies every rate limit. `1` in production; local end-to-end runs use a higher value. |
| `API_PROXY_SECRET` | Yes in production | empty | **Yes** | Shared with the web app. A request carrying it in `X-Managio-Proxy-Secret` may name the client address in `X-Managio-Client-Ip`; every other request counts against the connecting address (D-032). Empty or at least 32 characters. Empty behind Vercel means every visitor shares one rate limit; the API warns at start-up. |
| `APP_URL` | Yes in production | `http://localhost:3000` | No | Base of links in emails. |
| `RESEND_API_KEY` | Yes in production | empty | **Yes** | Resend API key. Empty: emails are logged, not sent. |
| `MAIL_FROM` | No | `Managio <onboarding@resend.dev>` | No | Sender. Production needs an address on a domain verified in Resend. |
| `MAIL_LOG_CONTENT` | No | `false` | No | Log email bodies with their links. Local development only. |
| `GOOGLE_CLIENT_ID` | No | empty | No | Google sign-in; empty turns it off. Must match the web app's `NEXT_PUBLIC_GOOGLE_CLIENT_ID`. |
| `PLATFORM_ADMIN_EMAILS` | No | empty (nobody) | No | Comma-separated emails of platform administrators (D-028). Their accounts must verify their email. |
| `PORT` | No | `8081` | No | HTTP port. 8081 by default because Jenkins uses 8080 on the development machine. The Docker image sets 8080 inside the container; Render sets its own (10000 by default), so leave it unset there. |
| `API_DOCS_ENABLED` | No | `false` | No | Publish `/v3/api-docs` and Swagger UI. `true` locally (the frontend generates its types from it) and in tests. |
| `SQL_LOG_LEVEL` | No | `warn` | No | `debug` logs SQL statements. |

Fixed in `application.yml` and overridable as Spring properties if ever needed: `managio.jobs.renewal-reminders-cron` (09:00 IST daily), `managio.jobs.token-cleanup-cron` (03:30 IST daily), `managio.slow-call-threshold` (500 ms).

## Web app (`managio_frontend`)

Locally in the untracked `managio_frontend/.env.local`; on Vercel in the project's environment variables.

| Variable | Required | Default | Secret | When it is read | Purpose |
|---|---|---|---|---|---|
| `API_ORIGIN` | Yes in production | `http://localhost:8081` | No | **Build time** | Where `/api/*` is passed through to. On Vercel the Render URL; in Docker `http://api:8080`. Changing it needs a rebuild. |
| `NEXT_PUBLIC_GOOGLE_CLIENT_ID` | No | empty | No (public by design) | Build time | Google Identity Services client ID; empty hides the Google button. Changing it needs a new build or a restart of `npm run dev`: a build made before it was set has no button. |
| `API_PROXY_SECRET` | Yes in production | empty | **Yes** (server only) | Run time | The API's `API_PROXY_SECRET`. The `/api` pass-through sends it with the visitor's address. Never use a `NEXT_PUBLIC_` name for it. |
| `CLIENT_IP_HEADER` | No | `x-forwarded-for` on Vercel, otherwise none | No | Run time | Which request header holds the visitor's address. Vercel overwrites `x-forwarded-for`, so it is trusted there. Self-hosted: name only a header your front proxy overwrites; with none, no address is vouched for. |
| `API_DOCS_URL` | Type generation only | `http://localhost:8081/v3/api-docs` | No | `npm run api:types` | Where `scripts/api-types.mjs` reads the OpenAPI document. |
| `E2E_API_LOG` | Tests only | none | No | Test run | Path of the API's log, so end-to-end tests can follow emailed links. |
| `E2E_ADMIN_EMAIL` | Tests only | `platform.admin@example.com` | No | Test run | The administrator the console tests sign in as; must be in the API's `PLATFORM_ADMIN_EMAILS`. |

## Docker Compose (repository root)

`docker compose up` reads the root `.env`. The `MANAGIO_` prefix keeps these apart from other values in that file.

| Variable | Required | Maps to |
|---|---|---|
| `MANAGIO_DB_PASSWORD` | Yes | PostgreSQL password and the API's `DB_PASSWORD`. |
| `MANAGIO_JWT_SECRET` | Yes | API `JWT_SECRET`. |
| `MANAGIO_WEB_PORT` | No (3000) | Host port of the web app; also used for `APP_URL` and CORS. |
| `MANAGIO_RESEND_API_KEY`, `MANAGIO_MAIL_FROM`, `MANAGIO_MAIL_LOG_CONTENT` | No | API email settings. |
| `MANAGIO_GOOGLE_CLIENT_ID` | No | API `GOOGLE_CLIENT_ID` and the web build's `NEXT_PUBLIC_GOOGLE_CLIENT_ID`. |
| `MANAGIO_PLATFORM_ADMIN_EMAILS` | No | API `PLATFORM_ADMIN_EMAILS`. |
| `MANAGIO_API_PROXY_SECRET`, `MANAGIO_CLIENT_IP_HEADER` | No | `API_PROXY_SECRET` for both apps and the web app's `CLIENT_IP_HEADER`. Only useful with a reverse proxy in front that sets that header. |

Compose sets `SECURE_COOKIES=false` because it serves plain http on localhost.

## CI (Jenkinsfile)

Plain values in the file: `DOCKER_USERNAME`, image names, `WEB_IMAGE_API_ORIGIN` (`http://api:8080`, the Compose address baked into the self-hosting web image). Jenkins credentials (Secret text): `dockerhub-token` (passed to `docker login --password-stdin`), `render-deploy-hook`, `vercel-deploy-hook`. Job parameters: `API_URL` (Render URL), `WEB_URL` (`https://managio.me`), `NOTIFY_EMAIL`, `ROLLBACK_TO_TAG`. Jenkins masks credentials in its logs.

## Local files

* `managio_backend/.env`: the API's local settings. Keep it pointed at the local PostgreSQL container, never at Neon (the owner's rule, 2026-10-22). The API imports it from the directory it starts in, so start the API from `managio_backend`.
* `managio_frontend/.env.local`: `API_ORIGIN` and `NEXT_PUBLIC_GOOGLE_CLIENT_ID` for `npm run dev` and local builds.
* Root `.env`: only the `MANAGIO_` Compose variables (the v1 values it used to hold were removed on 2026-10-22; rotate any that were ever real, STATUS.md open question 5).
