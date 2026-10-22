# Managio documentation

This folder is the single source of truth for Managio: what the product does, why it is built the way it is, and what is actually implemented. Code and documents must agree. When they disagree, treat it as a bug in one of them, find out which, and fix it.

## Documents

| Document | What it holds |
|---|---|
| [STATUS.md](STATUS.md) | What is implemented and verified, what is in progress, known issues, open questions, next steps. Read this first when resuming work. |
| [DECISIONS.md](DECISIONS.md) | Decision log: every significant product and technical decision with its context, rationale and status. |
| [PRODUCT.md](PRODUCT.md) | Users, business types, modules, business rules and workflows. |
| [ARCHITECTURE.md](ARCHITECTURE.md) | System shape, backend and frontend organisation, security, tenancy, conventions, testing approach. |
| [DATA_MODEL.md](DATA_MODEL.md) | Database tables, invariants, migrations and how v1 data was carried over. |
| [API.md](API.md) | API conventions, error format, authentication and the endpoint catalogue. |
| [ENVIRONMENT.md](ENVIRONMENT.md) | Every setting the API, web app, Docker Compose and CI read, with defaults and which are secret. |
| [DEPLOYMENT.md](DEPLOYMENT.md) | Docker images, the local production-like stack, the production plan, smoke test and rollback. |
| [TESTING.md](TESTING.md) | The test suites and how to run them. |
| [LEGAL.md](LEGAL.md) | The privacy, terms, contact and security pages: the code behind each claim, and what needs the owner's confirmation or a lawyer's review. |
| [LEGACY_AUDIT.md](LEGACY_AUDIT.md) | Findings from the audit of the v1 codebase (October 2026) that motivated the rebuild. |

## Status labels

Every rule, decision and claim carries one of these labels so readers know how much to trust it.

| Label | Meaning |
|---|---|
| `[VERIFIED]` | Confirmed against the code, a test run or the running system. Say how it was verified. |
| `[APPROVED]` | Decided by the project owner (Nitin). Quote or reference their words. |
| `[DECIDED]` | Decided by the engineer (Claude) under the owner's explicit delegation to make product and architecture decisions. Can be revisited by the owner. |
| `[PROPOSED]` | Suggested but not approved or implemented yet. |
| `[OPEN]` | Unresolved question that needs the owner's input or more information. Never implement around an open question by guessing. |
| `[PLANNED]` | Decided and scheduled, not implemented yet. Becomes `[VERIFIED]` once built and tested. |

## Rules for keeping the docs true

1. **Read before changing.** Before changing behaviour, read STATUS.md and the relevant sections of PRODUCT.md, DECISIONS.md, DATA_MODEL.md and API.md.
2. **Update after changing.** After a change, update every document it affects in the same piece of work: implementation status, rules, schema, endpoints.
3. **Never invent requirements.** If a behaviour is not documented and not obvious from the code, record it as `[OPEN]` rather than assuming the current behaviour is correct.
4. **Record decisions, not just outcomes.** Significant choices go into DECISIONS.md with the alternatives considered and the reason.
5. **Label honestly.** Something is `[VERIFIED]` only after it has been checked. Planned work stays `[PLANNED]` until it is built and tested.
6. **Keep it short.** Prefer one accurate sentence over a page of intentions. Delete statements that are no longer true instead of piling corrections on top.
