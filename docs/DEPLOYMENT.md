# Deployment

How Managio v2 is built and shipped. Settings are in [ENVIRONMENT.md](ENVIRONMENT.md); the decisions behind this are D-009, D-021, D-030, D-031, D-032 and D-037 in [DECISIONS.md](DECISIONS.md). Nothing has been deployed to a cloud service yet; what is verified locally is listed in [STATUS.md](STATUS.md). This page is the short checklist; the step-by-step handbook, free-tier limits, measurements and the deployment log are in [DEPLOYMENT_RUNBOOK.md](DEPLOYMENT_RUNBOOK.md).

## Target `[APPROVED]` (D-037, all free plans)

```
Browser ──> Vercel: Next.js web app ──(/api/* pass-through + proxy secret)──> Render Free: API image ──> Supabase Free (session pooler)
                                                                                    └──> Resend (email)
```

* The browser only talks to `https://managio.me`, so the refresh cookies are first-party (D-009).
* The web app's `/api` pass-through adds `X-Managio-Proxy-Secret` and the visitor's address (`X-Managio-Client-Ip`); the API trusts that address only with the right secret and ignores `X-Forwarded-For` (D-032).
* Render runs the API **image** that Jenkins built and tested, pulled from Docker Hub. Vercel builds the web app from the same commit when Jenkins calls its deploy hook (D-031).
* Render Free sleeps after 15 idle minutes and starts slowly on 0.1 CPU. A working-hours keep-alive (the owner's decision, against Render's guidance) and start-up JVM flags limit cold starts (runbook section 2).
* Supabase is reached through its session pooler (port 5432, `sslmode=require`); its Data API is turned off. Neon is left untouched and unused.

## Release pipeline `[DECIDED]` (D-031)

```
git push master ──> Jenkins (polls every 5 min)
                      1. Test API: ./mvnw verify (unit, integration on PostgreSQL in Testcontainers, contract, architecture)
                      2. Check web app: npm ci, type check, lint, unit tests
                      3. Build both images, tagged <commit>-<build>
                      4. Push to Docker Hub (nitinr1103/managio-backend, nitinr1103/managio-frontend), plus :latest
                      5. Render deploy hook with imgURL=…managio-backend:<tag>  ──> wait for /actuator/health/readiness
                      6. Vercel deploy hook (builds master)                      ──> wait for https://managio.me/sign-in
                      7. Smoke test: /sign-in 200, /api/v1/me 401 through the web app, /v3/api-docs 404 on the API
                    failure ──> email to NOTIFY_EMAIL (if Jenkins mail is set up)
```

* Other branches run steps 1–3 only. `managio_frontend/vercel.json` turns off Vercel's own production deploys on push (`git.deploymentEnabled.master = false`), so the web app never goes live ahead of the API; preview deployments of other branches still build.
* Migrations run when the new API instance starts (Flyway). Keep releases backward compatible (expand, then contract) because the old web app keeps calling the new API for a few minutes.
* The pipeline is in `Jenkinsfile` and runs on Linux or Windows agents. It never contains secrets.
* Until the Jenkins job exists, the same steps are done by hand (runbook sections 5, 6 and 11).

### Jenkins setup (once) `[PLANNED]`

1. **Agent.** This PC: the Jenkins service runs as LocalSystem, which can use Docker (verified); Java 21, Node, Git, curl and Docker are on the system PATH. Docker Desktop must be running. Jenkins keeps port 8080; the API uses 8081 locally.
2. **Credentials** (Manage Jenkins → Credentials, kind *Secret text*): `dockerhub-token` (a Docker Hub access token with read/write), `render-deploy-hook`, `vercel-deploy-hook`.
3. **Job.** New *Pipeline* job, *Pipeline script from SCM*, Git `https://github.com/Nitinr0306/Managio-Business-Management-System-.git`, branch `*/master`, script path `Jenkinsfile`. The first run registers the poll trigger and the parameters; set `API_URL` (the Render URL) and optionally `NOTIFY_EMAIL` as defaults.
4. **Mail** (optional): Manage Jenkins → System → E-mail Notification.

## Artifacts `[VERIFIED]` (see STATUS.md and runbook section 13 for the evidence)

| Artifact | Built from | Notes |
|---|---|---|
| API image | `managio_backend/Dockerfile` | Multi-stage: Maven wrapper (checksum-pinned Maven) on Temurin 21 JDK → Temurin 21 JRE on Alpine. Non-root user. Health check `GET /actuator/health/readiness`. Listens on `PORT` (8080 in the image; Render sets its own). |
| Web image | `managio_frontend/Dockerfile` | Multi-stage on Node 22 Alpine with Next.js standalone output. Non-root user. Health check `GET /sign-in`. For self-hosting and the local stack; Vercel builds from source. |
| Local stack | `docker-compose.yml` | PostgreSQL 17, API, web. Only the web port is published. |
| Not used now | `deploy/oracle/` | A tested always-on VM setup (D-036, superseded), kept for a possible later move. |

## Run the production images locally

```
cp .env.example .env          # set MANAGIO_DB_PASSWORD and MANAGIO_JWT_SECRET
docker compose up --build -d
docker compose ps             # db, api and web become healthy in turn
open http://localhost:3000
docker compose down           # add -v to delete the database volume
```

The API applies its Flyway migrations on start. Emails are written to the API log (`docker compose logs api`) unless `MANAGIO_RESEND_API_KEY` is set. Nothing in front of this local stack overwrites `X-Forwarded-For`, so don't expose it to the internet as it is.

## First production deployment `[PLANNED]`

Every step is done by the owner in the providers' dashboards, in this order. The details, including where each value comes from, are in the runbook (sections 3, 5 and 6). Secrets go into Render, Vercel and Jenkins, never into chat, code or tickets.

1. **Rotate exposed credentials** (STATUS.md, risk 1): the old Docker Hub token and Resend key.
2. **Commit and push** the current changes (runbook section 13 lists them).
3. **Docker Hub:** a read/write token for pushing and a read-only token for Render; build and push `nitinr1103/managio-backend:<commit>-manual1`.
4. **Supabase:** a Free project in Singapore with a generated database password; **turn off the Data API**; copy the session pooler host and user from **Connect → Session pooler**.
5. **Render:** *New → Web Service → Existing Image* with the image and the read-only Docker Hub credential; Singapore; Free; health check `/actuator/health/readiness`; no pre-deploy command. Environment:

   | Variable | Value |
   |---|---|
   | `DB_URL` | `jdbc:postgresql://<session pooler host>:5432/postgres?sslmode=require` |
   | `DB_USERNAME`, `DB_PASSWORD` | `postgres.<project-ref>` and the Supabase database password |
   | `JWT_SECRET` | new, `openssl rand -base64 48` |
   | `API_PROXY_SECRET` | new, `openssl rand -hex 32`; the same value goes to Vercel |
   | `APP_URL`, `CORS_ALLOWED_ORIGINS` | `https://managio.me` |
   | `GOOGLE_CLIENT_ID` | the OAuth client ID |
   | `RESEND_API_KEY` | a send-only key |
   | `MAIL_FROM` | `Managio <noreply@managio.me>` |
   | `PLATFORM_ADMIN_EMAILS` | the administrators' emails |
   | `JAVA_TOOL_OPTIONS` | `-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xss512k` |

   Leave `PORT`, `SECURE_COOKIES`, `API_DOCS_ENABLED`, `MAIL_LOG_CONTENT` and `RATE_LIMIT_MULTIPLIER` unset. Copy the deploy hook URL (Settings → Deploy Hook) for the Jenkins credential `render-deploy-hook`.
6. **Keep-alive** (the owner's decision): a free scheduler calls `https://<service>.onrender.com/actuator/health/liveness` every 10 minutes, 08:00–22:00 India time.
7. **Vercel (web):** the existing project; root directory `managio_frontend`, framework Next.js, Node 22.x. Production environment variables: `API_ORIGIN` (the Render URL, no trailing slash), `NEXT_PUBLIC_GOOGLE_CLIENT_ID`, `API_PROXY_SECRET` (the same as Render). Add `www.managio.me` as a redirect to the apex. Create a deploy hook for `master` (Settings → Git), store it as the Jenkins credential `vercel-deploy-hook`, and call it once to deploy v2 (this replaces the live v1 site).
8. **Google Cloud Console** (OAuth client, type Web application). Authorised JavaScript origins: `https://managio.me`, `http://localhost`, `http://localhost:3000`. No redirect URIs and no client secret are used. Branding, **after** the pages below answer 200 in production: application home page `https://managio.me`, privacy policy `https://managio.me/privacy`, terms of service `https://managio.me/terms`, authorised domain `managio.me`, user support email `nitinr0306@gmail.com`; then publish the app ("In production").
9. **Resend.** `managio.me` is verified for sending (Resend accepted a test email from `noreply@managio.me` on 2026-10-22). Use a send-only key on Render, `MAIL_FROM=Managio <noreply@managio.me>`, and leave `MAIL_LOG_CONTENT` unset (false).
10. **Smoke test by hand** (below), including a real Google sign-in and a real verification email; then the first `pg_dump` backup (runbook section 4).

## Smoke test after any deployment

1. `GET https://<service>.onrender.com/actuator/health/readiness` answers `{"status":"UP"}`; `/actuator/health` answers `UP` (database reachable); `/v3/api-docs` answers 404.
2. `https://managio.me/sign-in` loads with `Content-Security-Policy`, `Strict-Transport-Security`, `X-Frame-Options: DENY` and `X-Content-Type-Options: nosniff`; the Google button shows with no console errors.
3. `https://managio.me/privacy`, `/terms`, `/security` and `/contact` answer 200; `https://www.managio.me/` redirects to `https://managio.me/`; `/robots.txt` and `/sitemap.xml` load; submit the sitemap in Google Search Console (domain property `managio.me`).
4. Sign up, receive the verification email from `noreply@managio.me`, follow its link.
5. Create a business, add a customer and a plan, record a payment, open the receipt.
6. Sign out, sign back in; an unknown email and a wrong password show the same message. Sign in with Google.
7. A platform administrator opens `/admin` and sees the new business; a non-administrator sees the refusal.
8. The refresh cookie `mg_rt` is `Secure; HttpOnly; SameSite=Lax`.
9. Rate limits count visitors: from one machine, the 11th wrong sign-in within a minute answers 429, while another network still gets 401. A direct call to the Render URL with any `X-Forwarded-For` doesn't escape the limit.

## Rolling back

* **API:** run the Jenkins job with `ROLLBACK_TO_TAG` set to the last good image tag (Docker Hub lists them); it redeploys that image through the Render hook and waits for readiness. By hand: Render → the service → Settings → Image URL → the earlier tag.
* **Web app:** Vercel → Deployments → the last good production deployment → *Instant Rollback*.
* **Database:** migrations only move forward. Flyway ignores migrations newer than the image it runs in, so an earlier API image starts on a schema that only gained tables or columns (expand, then contract in a later release). To undo a bad migration, restore the `pg_dump` taken before the release (runbook section 4), then roll the API back.
