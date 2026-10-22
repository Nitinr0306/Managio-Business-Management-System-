# Managio

Multi-business management platform (Spring Boot API in `managio_backend/`, Next.js app in `managio_frontend/`).

## Source of truth

`docs/` is the single source of truth for this project. Start every session by reading `docs/STATUS.md`, then the documents relevant to the task (index in `docs/README.md`).

* Consult the docs before changing behaviour; update them in the same piece of work after changing it.
* Every rule and decision carries a status label: `[VERIFIED]`, `[APPROVED]`, `[DECIDED]`, `[PROPOSED]`, `[OPEN]`, `[PLANNED]`. Do not treat `[PROPOSED]`, `[PLANNED]` or `[OPEN]` items as implemented.
* Never invent requirements. Unclear behaviour becomes an `[OPEN]` question in `docs/STATUS.md`.
* Record significant decisions in `docs/DECISIONS.md` with context and rationale.

## Working agreements with the owner

* Do not commit or push without explicit permission. Do not change production services, credentials or data without explicit permission.
* Never print, log or commit secrets. Local secrets live in untracked `.env` files.
* Preserve existing data: schema changes are Flyway migrations that carry data forward.
