# Deployment

How Managio v2 is built and shipped. Settings are in [ENVIRONMENT.md](ENVIRONMENT.md); the decisions behind this are D-009, D-021, D-030, D-031 and D-032 in [DECISIONS.md](DECISIONS.md). Nothing has been deployed to a cloud service yet; what is verified locally is listed in [STATUS.md](STATUS.md).

## Target `[DECIDED]`

```
Browser ──> Vercel: Next.js web app ──(/api/* pass-through + proxy secret)──> Render: API image ──> Neon PostgreSQL
                                                                                    └──> Resend (email)
```

* The browser only talks to `https://managio.me`, so the refresh cookies are first-party (D-009).
* The web app's `/api` pass-through adds `X-Managio-Proxy-Secret` and the visitor's address (`X-Managio-Client-Ip`); the API trusts that address only with the right secret and ignores `X-Forwarded-For` (D-032).
* Render runs the API **image** that Jenkins built and tested, pulled from Docker Hub. Vercel builds the web app from the same commit when Jenkins calls its deploy hook (D-031).

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

### Jenkins setup (once) `[PLANNED]`

1. **Agent.** Docker, Node 22 and curl. On Windows, Docker Desktop runs per user: run the Jenkins service under the account that runs Docker Desktop (Services → jenkins → Log On) and add that account to `docker-users`, or the API tests can't start Testcontainers. Jenkins keeps port 8080; the API uses 8081 locally.
2. **Credentials** (Manage Jenkins → Credentials, kind *Secret text*): `dockerhub-token` (a new Docker Hub access token with read/write), `render-deploy-hook`, `vercel-deploy-hook`.
3. **Job.** New *Pipeline* job, *Pipeline script from SCM*, Git `https://github.com/Nitinr0306/Managio-Business-Management-System-.git`, branch `*/master`, script path `Jenkinsfile`. The first run registers the poll trigger and the parameters; set `API_URL` (the Render URL) and optionally `NOTIFY_EMAIL` as defaults.
4. **Mail** (optional): Manage Jenkins → System → E-mail Notification.

## Artifacts `[VERIFIED]` (see STATUS.md for the build and smoke-test evidence)

| Artifact | Built from | Notes |
|---|---|---|
| API image | `managio_backend/Dockerfile` | Multi-stage: Maven wrapper (checksum-pinned Maven) on Temurin 21 JDK → Temurin 21 JRE on Alpine. Runs as a non-root user. Health check `GET /actuator/health/readiness`. Listens on `PORT` (8080 in the image; Render sets its own). |
| Web image | `managio_frontend/Dockerfile` | Multi-stage on Node 22 Alpine with Next.js standalone output. Non-root user. Health check `GET /sign-in`. `API_ORIGIN` and `NEXT_PUBLIC_GOOGLE_CLIENT_ID` are build arguments; `API_PROXY_SECRET` and `CLIENT_IP_HEADER` are read when it runs. For self-hosting and Compose; Vercel builds from source. |
| Local stack | `docker-compose.yml` | PostgreSQL 17, API, web. Only the web port is published. |

## Run the production images locally

```
cp .env.example .env          # set MANAGIO_DB_PASSWORD and MANAGIO_JWT_SECRET
docker compose up --build -d
docker compose ps             # db, api and web become healthy in turn
open http://localhost:3000
docker compose down           # add -v to delete the database volume
```

The API applies its Flyway migrations on start. Emails are written to the API log (`docker compose logs api`) unless `MANAGIO_RESEND_API_KEY` is set. Nothing in front of the Compose stack overwrites `X-Forwarded-For`, so all visitors share the web container's rate limits; don't expose it to the internet without a reverse proxy that does (then set `MANAGIO_CLIENT_IP_HEADER`).

## First production deployment `[PLANNED]`

Manual steps happen in provider dashboards; paste secrets there, never into chat, code or tickets.

1. **Rotate exposed credentials first** (STATUS.md, open question 5): Docker Hub token, Resend key, any v1 database password, Google client secret, SMTP and Razorpay keys.
2. **Neon.** Create the project in the region nearest Render. The owner confirms resetting any v1 data at that moment (D-021). Use the **direct** (non-pooled) host in `DB_URL`: Flyway takes a session-level advisory lock, which Neon's pooler doesn't support. Format: `jdbc:postgresql://<host>/<database>?sslmode=require`, user and password in their own settings. Before every release with a new migration, create a Neon branch (or note the restore point) as the database rollback.
3. **Docker Hub.** Run the pipeline once (or push `managio-backend:<tag>` by hand) so an image exists. If the repository is private, give Render a registry credential.
4. **Render (API).** New web service → *Existing image* `docker.io/nitinr1103/managio-backend:<tag>`. Health check path `/actuator/health/readiness`. Auto-deploy off (Jenkins deploys). Environment:

   | Variable | Value |
   |---|---|
   | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Neon direct connection |
   | `JWT_SECRET` | new, `openssl rand -base64 48` |
   | `API_PROXY_SECRET` | new, `openssl rand -hex 32`; the same value goes to Vercel |
   | `APP_URL` | `https://managio.me` |
   | `CORS_ALLOWED_ORIGINS` | `https://managio.me` |
   | `GOOGLE_CLIENT_ID` | the OAuth client ID |
   | `RESEND_API_KEY` | a send-only key |
   | `MAIL_FROM` | `Managio <noreply@managio.me>` (domain verified in Resend) |
   | `PLATFORM_ADMIN_EMAILS` | the administrators' emails |

   Leave `PORT`, `SECURE_COOKIES`, `API_DOCS_ENABLED`, `MAIL_LOG_CONTENT` and `RATE_LIMIT_MULTIPLIER` unset. Copy the service's deploy hook URL into the Jenkins credential `render-deploy-hook`.
5. **Vercel (web).** New project from the GitHub repository, root directory `managio_frontend`, framework Next.js, production branch `master`. Production environment variables: `API_ORIGIN` (the Render URL, no trailing slash), `NEXT_PUBLIC_GOOGLE_CLIENT_ID`, `API_PROXY_SECRET` (same as Render). Add the domain `managio.me` (Vercel issues the HTTPS certificate; choose whether `www` redirects to the apex). Create a deploy hook for `master` (Settings → Git) and store it as the Jenkins credential `vercel-deploy-hook`.
6. **Google Cloud Console** (OAuth client, type Web application). Authorised JavaScript origins: `https://managio.me`, `http://localhost`, `http://localhost:3000` (`www.managio.me` redirects to the apex, so it doesn't need to be listed). No redirect URIs and no client secret are used. Branding, **after** the pages below answer 200 in production: application home page `https://managio.me`, privacy policy `https://managio.me/privacy`, terms of service `https://managio.me/terms`, authorised domain `managio.me`, user support email `nitinr0306@gmail.com`; then publish the app ("In production").
7. **Resend.** `managio.me` is verified for sending: on 2026-10-22 Resend accepted a test email from `noreply@managio.me` with the send-only key (Resend refuses unverified domains). Use a send-only key on Render too, set `MAIL_FROM=Managio <noreply@managio.me>` and leave `MAIL_LOG_CONTENT` unset (false).
8. **Release.** Run the Jenkins job on master; it deploys the API, then the web app, then smoke-tests.
9. **Smoke test by hand** (below), including a real Google sign-in and a real verification email.

## Smoke test after any deployment

1. `GET <api>/actuator/health/readiness` answers `{"status":"UP"}`; `<api>/v3/api-docs` answers 404.
2. `https://managio.me/sign-in` loads with `Content-Security-Policy`, `Strict-Transport-Security`, `X-Frame-Options: DENY` and `X-Content-Type-Options: nosniff`; the Google button shows with no console errors.
3. `https://managio.me/privacy`, `/terms`, `/security` and `/contact` answer 200; `https://www.managio.me/` redirects to `https://managio.me/`; `/robots.txt` and `/sitemap.xml` load; submit the sitemap in Google Search Console (domain property `managio.me`).
4. Sign up, receive the verification email from `noreply@managio.me`, follow its link.
5. Create a business, add a customer and a plan, record a payment, open the receipt.
6. Sign out, sign back in; an unknown email and a wrong password show the same message. Sign in with Google.
7. A platform administrator opens `/admin` and sees the new business; a non-administrator sees the refusal.
8. The refresh cookie `mg_rt` is `Secure; HttpOnly; SameSite=Lax`.
9. Rate limits count visitors: from one machine, the 11th wrong sign-in within a minute answers 429, while another network still gets 401. A direct call to the Render URL with any `X-Forwarded-For` doesn't escape the limit.

## Rolling back

* **API:** run the Jenkins job with `ROLLBACK_TO_TAG` set to the last good image tag (Docker Hub lists them); it redeploys that image through the Render hook and waits for readiness. Render's dashboard can also redeploy an earlier deploy.
* **Web app:** Vercel → Deployments → the last good production deployment → *Instant Rollback*.
* **Database:** migrations only move forward. Flyway ignores migrations newer than the image it runs in, so an earlier API image starts on a schema that only gained tables or columns (expand, then contract in a later release). To undo a bad migration, restore the Neon branch or point in time taken before the release, then roll the API back.
