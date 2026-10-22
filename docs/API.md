# API

REST over JSON, base path `/api/v1`. The OpenAPI document served at `/v3/api-docs` (Swagger UI at `/swagger-ui.html` outside production) is the authoritative contract; the frontend's TypeScript types are generated from it. This page records conventions and the endpoint catalogue.

Status: conventions are `[DECIDED]`; endpoints are listed as they are implemented and verified.

## Conventions `[DECIDED]`

* **Resources.** Plural nouns. Business-scoped resources live under `/api/v1/businesses/{businessId}/…` (D-008). Customer portal resources live under `/api/v1/portal/…` and take the business from the customer's token.
* **IDs.** Numeric IDs in URLs. They are only meaningful inside their business; a record from another business answers 404.
* **Methods.** `GET` reads, `POST` creates or performs an action (`/payments/{id}/void`), `PATCH` partially updates, `PUT` replaces settings-like documents, `DELETE` removes things that have no history (a role nobody uses, a group).
* **Validation.** Request bodies are validated with Bean Validation; violations return 400 with a field map.
* **Lists.** `?page=0&size=25` (max 100) and resource-specific filters. Response:
  ```json
  { "items": [...], "page": 0, "size": 25, "totalItems": 412, "totalPages": 17 }
  ```
* **Dates.** `YYYY-MM-DD` for business dates, ISO-8601 with offset for instants. Money as JSON numbers with two decimals, in the business currency.
* **Passwords and tokens** only travel in request bodies or cookies, never in query strings.
* **Date ranges.** `from` after `to` is 400 `validation.failed` (on `to`), never an empty list.

## Retries `[DECIDED]` (D-033, IdempotencyTest)

`POST` endpoints that create money records accept an optional `Idempotency-Key` header: invoices, sales, invoice payments, refunds, expenses, payroll payments, memberships and renewals.

* A key is 8 to 100 letters, digits, `-` or `_` (otherwise 400 `idempotency.invalid_key`). Clients generate a random one per submission and send the same key when they retry it.
* The first request with a key runs normally and its response is stored. A repeat with the same key and the same body returns that stored response with the same status, and records nothing new, even when both arrive at once.
* A request that failed (validation, a business rule) stores nothing, so the corrected request can reuse the key.
* The same key with a different body or target answers 422 `idempotency.key_reused`. Keys are per business and user and are kept for 7 days.
* Without the header, a repeated request creates a second record. The invoice row lock still prevents paying more than the balance.

## Authentication `[DECIDED]`

* `Authorization: Bearer <access token>` on every authenticated request.
* Refresh tokens travel only in the `mg_rt` / `mg_prt` HttpOnly cookies; `POST /api/v1/auth/refresh` and `POST /api/v1/portal/auth/refresh` return a new access token and rotate the cookie.
* Unauthenticated → 401 `auth.required`. Authenticated but missing permission → 403 `auth.forbidden`. Not a member of the business, or record not in the business → 404.

## Errors `[DECIDED]`

RFC 9457 problem details, content type `application/problem+json`:

```json
{
  "type": "https://managio.app/problems/validation",
  "title": "Check the highlighted fields",
  "status": 400,
  "detail": "2 fields need attention.",
  "code": "validation.failed",
  "errors": { "phone": "Enter a 10-digit mobile number" },
  "requestId": "9f2c…"
}
```

`code` values are stable and documented next to the endpoints that return them. Messages are written for the person using the app.

## Endpoint catalogue

Only implemented and tested endpoints are listed. "Member" means any active team member of the business. See [STATUS.md](STATUS.md) for progress.

### Accounts `[VERIFIED]` (AuthFlowTest)
| Method and path | Access | Notes |
|---|---|---|
| `POST /api/v1/auth/sign-up` | Public | `{fullName, email, password}` → 201 session; sends verification email. 409 `auth.email_taken`. |
| `POST /api/v1/auth/sign-in` | Public | `{email, password}` → session. 401 `auth.invalid_credentials`, 423 `auth.locked`, 403 `auth.disabled`. |
| `POST /api/v1/auth/google` | Public | `{credential}` (Google ID token) → session. 422 `google.email_unverified` / `google.disabled`, 401 `google.invalid`, 409 `google.other_account`. |
| `POST /api/v1/auth/refresh` | `mg_rt` cookie | → session with rotated cookie. 401 `auth.session_expired` clears the cookie. |
| `POST /api/v1/auth/sign-out` | `mg_rt` cookie | 204, clears the cookie. |
| `POST /api/v1/auth/verify-email` | Public | `{token}` → user. 422 `token.invalid`. |
| `POST /api/v1/auth/password-reset` | Public | `{email}` → 202 always. |
| `POST /api/v1/auth/password-reset/confirm` | Public | `{token, password}` → 204. 422 `token.invalid`. |
| `GET /api/v1/me` | User | Profile. |
| `PATCH /api/v1/me` | User | `{fullName, phone}`. |
| `POST /api/v1/me/password` | User | `{currentPassword, newPassword}` → new session (other sessions end). |
| `POST /api/v1/me/verification-email` | User | 202, resends the verification email. |

A session response is `{accessToken, expiresIn, user}`; the refresh token is set as the `mg_rt` cookie. Sign-up, sign-in, Google sign-in and accepting an invitation as a new account end the session of any `mg_rt` cookie sent with the request, once they succeed (the portal does the same with `mg_prt`).

Rate limits (429 `rate_limited`), per client address (IPv6 per /64; D-032): sign-in and Google 10/min, sign-up 5/10 min, password-reset and verification emails 5/15 min, refresh 60/min. Per recipient: 3 password-reset emails an hour. Per account: 5 current-password checks (`POST /me/password`) per 15 min. Google sign-in also answers 423 `auth.locked` and 403 `auth.disabled`, like a password (D-034).

### Businesses, roles and team `[VERIFIED]` (BusinessAndTeamTest, TenancyIsolationTest)
| Method and path | Access | Notes |
|---|---|---|
| `GET /api/v1/reference/business-types` | User | Types with labels and default modules. |
| `GET /api/v1/reference/permissions` | User | Permission catalogue for the role editor. |
| `POST /api/v1/businesses` | User | `{name, type, phone?, city?}` → 201; caller becomes owner. |
| `GET /api/v1/me/businesses` | User | Businesses the caller is active in, with role, permissions, modules and labels. |
| `GET /api/v1/businesses/{businessId}` | Member | |
| `PATCH /api/v1/businesses/{businessId}` | `settings.manage` | Profile: name, type, contact, address, GSTIN. |
| `PUT /api/v1/businesses/{businessId}/settings` | `settings.manage` | `{modules, timezone, invoicePrefix, receiptPrefix, allowCustomerSignup}`. |
| `POST /api/v1/businesses/{businessId}/archive` | Owner | 204. |
| `GET /api/v1/businesses/{businessId}/roles` | `team.view` | With member counts. |
| `POST /api/v1/businesses/{businessId}/roles` | Owner | `{name, description, permissions[]}` (permission enum names). 409 `role.name_taken`. |
| `PATCH /api/v1/businesses/{businessId}/roles/{roleId}` | Owner | 422 `role.owner_locked`. |
| `DELETE /api/v1/businesses/{businessId}/roles/{roleId}` | Owner | 422 `role.in_use`, `role.owner_locked`. |
| `GET /api/v1/businesses/{businessId}/team` | `team.view` | `?includeLeft=true` includes people who left. Salary only with `payroll.manage`. |
| `GET /api/v1/businesses/{businessId}/team/{memberId}` | `team.view` | |
| `PATCH /api/v1/businesses/{businessId}/team/{memberId}` | `team.manage` | Saves the edit form; `monthlySalary`/`clearSalary` need `payroll.manage`. 422 `team.self`, `team.owner`. |
| `POST /api/v1/businesses/{businessId}/team/{memberId}/suspend` · `/reactivate` · `/remove` | `team.manage` | 422 `team.self`, `team.owner`, `team.left`. |
| `GET /api/v1/businesses/{businessId}/invitations` | `team.manage` | Pending invitations. |
| `POST /api/v1/businesses/{businessId}/invitations` | `team.manage` | `{email, roleId, title?}`. 422 `invitation.unverified_sender`, `role.owner_not_assignable`; 403 when the role holds permissions the caller lacks; 409 `invitation.already_member`. |
| `POST /api/v1/businesses/{businessId}/invitations/{id}/resend` | `team.manage` | 204; old link stops working. |
| `DELETE /api/v1/businesses/{businessId}/invitations/{id}` | `team.manage` | 204. |
| `POST /api/v1/invitations/preview` | Public | `{token}` → business, role, email, whether an account exists. 422 `invitation.invalid`. |
| `POST /api/v1/invitations/accept` | Public or user | `{token}` when signed in with the invited email; otherwise `{token, fullName, password}` or `{token, googleCredential}`. Returns `{businessId, session?}`. 409 `invitation.sign_in_required`, 422 `invitation.wrong_account`. |
| `GET /api/v1/businesses/{businessId}/activity` | `activity.view` | Paged; `?entityType=&entityId=`. |

### Customers, groups and attendance `[VERIFIED]` (CustomerTest)
All under `/api/v1/businesses/{businessId}`.

| Method and path | Access | Notes |
|---|---|---|
| `GET /customers` | `customers.view` | `?view=ACTIVE\|ENDING_SOON\|LAPSED\|NO_PLAN\|WITH_DUES\|ARCHIVED\|ALL&q=&groupId=&sort=NAME\|NUMBER\|NEWEST\|ENDING\|DUES&page=&size=`. Rows carry membership state, plan, coverage end and balance due. |
| `GET /customers/counts` | `customers.view` | Counts for the filter tabs. |
| `GET /customers/{id}` | `customers.view` | With groups, membership state, `balanceDue` (BIL-10) and `upcoming`. |
| `POST /customers` · `PATCH /customers/{id}` | `customers.manage` | Profile fields plus `joinedOn`, `confirmDuplicate`. 409 `customer.duplicate_phone`. |
| `POST /customers/{id}/archive` · `/restore` | `customers.archive` | |
| `GET /customers/export` | `customers.import` | CSV download. |
| `POST /customers/import` | `customers.import` | Multipart `file`; returns `{added, skipped, errors[{row, message}]}`. |
| `GET/POST /groups`, `PATCH/DELETE /groups/{id}`, `POST /groups/{id}/members`, `DELETE /groups/{id}/members/{customerId}` | Groups module; view `customers.view`, change `customers.manage` | 409 `group.name_taken`. |
| `GET /attendance?date=&groupId=` | Attendance module, `customers.view` | |
| `POST /attendance` | Attendance module, `attendance.mark` | `{customerId}` → check-in; `alreadyCheckedIn`, `covered` flags. |
| `POST /groups/{id}/attendance` | Attendance module, `attendance.mark` | `{date, present[]}` replaces the present list for that group and date. |
| `DELETE /attendance/{id}` | Attendance module, `attendance.mark` | Same business day only. |

### Plans, services and products `[VERIFIED]` (BillingTest, MembershipTest)
| Method and path | Access | Notes |
|---|---|---|
| `GET /offerings` | Member | `?kind=&includeInactive=` (inactive only with `offerings.manage`). |
| `GET /offerings/{id}` | Member | |
| `POST /offerings` · `PATCH /offerings/{id}` | `offerings.manage` | `{kind, name, price, taxRate?, durationValue?, durationUnit?, installments?, sku?, trackStock?, initialStock?, active?}`. Kind can't change. 422 `module.disabled`. |
| `DELETE /offerings/{id}` | `offerings.manage` | 422 `offering.in_use` once sold. |
| `POST /offerings/{id}/stock` | Inventory module, `offerings.manage` | `{change, reason?}`. 422 `stock.insufficient`. |

### Memberships `[VERIFIED]` (MembershipTest)
Memberships module required (404 `module.disabled` otherwise).

| Method and path | Access | Notes |
|---|---|---|
| `GET /memberships` | `customers.view` | `?view=ALL\|ACTIVE\|ENDING_SOON\|UPCOMING\|EXPIRED\|CANCELLED&needsRenewal=true` (only unrenewed memberships of active customers). |
| `GET /memberships/{id}` · `GET /customers/{id}/memberships` | `customers.view` | With invoices. |
| `POST /memberships` | `memberships.manage` | `{customerId, offeringId, startsOn?, discount?, payNow?{method, amount?, reference?}}` → 201. 409 `membership.overlap`. `discount` needs `billing.discount`, `payNow` needs `billing.collect`. |
| `POST /memberships/{id}/renew` | `memberships.manage` | `{offeringId?, discount?, payNow?}`. 422 `membership.already_renewed`, `membership.cancelled`. |
| `POST /memberships/{id}/cancel` | `memberships.manage` | `{reason?}`; voids unpaid future invoices. |

### Invoices, payments and sales `[VERIFIED]` (BillingTest)
| Method and path | Access | Notes |
|---|---|---|
| `GET /invoices` | `billing.view` | `?view=ALL\|OPEN\|OVERDUE\|PAID\|VOID&customerId=&q=`. |
| `GET /invoices/{id}` | `billing.view` | With lines, payments, `paid`, `balance`, `state`. |
| `POST /invoices` | `billing.collect` | `{customerId, lines[{offeringId? \| description+unitPrice, quantity?, unitPrice?, taxRate?}], discount?, dueOn?, notes?}`. 422 `invoice.plan_line`, `offering.inactive`, `stock.insufficient`. |
| `POST /sales` | `billing.collect` | `{invoice, payment}`: invoice and payment in one step; `customerId` optional only when paid in full. |
| `POST /invoices/{id}/void` | `billing.void` | `{reason}`. 422 `invoice.has_payments`. |
| `POST /invoices/{id}/payments` | `billing.collect` | `{amount?, method, paidOn?, reference?, notes?}`; amount defaults to the balance. 400 when above the balance. |
| `GET /payments` | `billing.view` | `?from=&to=&method=&customerId=`. |
| `GET /payments/{id}` | `billing.view` | |
| `POST /payments/{id}/void` | `billing.void` | `{reason}`. 422 `payment.has_refunds`, `payment.already_void`. |
| `POST /payments/{id}/refunds` | `billing.void` | `{amount?, method, reason}`. |

A payment's `receiptNumber` and an invoice's `invoiceNumber` use the business's prefixes (RCT-0001, INV-0042).

### Expenses, payroll and the day book `[VERIFIED]` (OperationsTest)
| Method and path | Access | Notes |
|---|---|---|
| `GET /expenses` | `expenses.view` | `?from=&to=&categoryId=&q=&includeVoid=`. |
| `GET /expenses/summary` | `expenses.view` | `?from=&to=` gives the total and totals by category (void excluded). |
| `GET /expenses/{id}` | `expenses.view` | |
| `POST /expenses` | `expenses.record` | `{categoryId, amount, method, spentOn?, payee?, description?, reference?}`. 422 `category.archived`, `day.closed`. |
| `POST /expenses/{id}/void` | `expenses.record` | `{reason}`. 422 `expense.already_void`, `day.closed`. |
| `GET /expense-categories` | Member | Everyone on the team can read them; expense claims need one. |
| `POST /expense-categories`, `PATCH /expense-categories/{id}` | `settings.manage` | `{name, archived?}`. 422 `category.required` for Salaries. |
| `GET /payroll?month=` | Payroll module, `payroll.manage` | Entries with `paid`, `remaining`, `state`. |
| `POST /payroll/prepare` | Payroll module, `payroll.manage` | `{month?}` creates missing entries for active people with a salary. |
| `POST /payroll/{id}/payments` | Payroll module, `payroll.manage` | `{amount?, method, paidOn?, notes?}`; records a Salaries expense. |
| `PATCH /payroll/{id}` | Payroll module, `payroll.manage` | `{amount, notes?}`: a bonus or deduction, not below what is already paid. |
| `GET /day-book?date=` | `daybook.close` | Opening cash, cash in and out, expected cash, per-method totals, close details. |
| `POST /day-book/close` | `daybook.close` | `{date?, countedCash, notes?}`. 409 `day.already_closed`. |
| `GET /day-book/history?days=` | `daybook.close` | |

### Tasks, requests and notifications `[VERIFIED]` (OperationsTest)
| Method and path | Access | Notes |
|---|---|---|
| `GET /tasks` | Member | `?view=OPEN\|DONE\|ALL&mine=&assigneeId=`; visibility per TSK-3. |
| `GET /tasks/{id}`, `POST /tasks` | Member | Creating for someone else needs `tasks.manage`. |
| `PUT /tasks/{id}`, `DELETE /tasks/{id}` | `tasks.manage` | |
| `PATCH /tasks/{id}/status` | Assignee or `tasks.manage` | `{status}`; finishing a repeating task creates the next one. |
| `GET /requests` | Member | `?status=&mine=`; requesters see their own only. |
| `GET /requests/{id}`, `POST /requests` | Member | `{type: EXPENSE\|PURCHASE\|LEAVE\|DISCOUNT\|OTHER, title, details?, amount?, expenseCategoryId?, startsOn?, endsOn?, invoiceId?}`. |
| `POST /requests/{id}/cancel` | Requester | While pending. |
| `POST /requests/{id}/approve` | `requests.approve` | `{note?, method?}`; `method` is required for expense claims. 422 `request.own`, `request.closed`, `invoice.not_discountable`. |
| `POST /requests/{id}/reject` | `requests.approve` | `{note}` is required. |
| `GET /requests/pending-count` | `requests.approve` | |
| `GET /api/v1/me/notifications?unread=&limit=` | User | Across all the user's businesses. |
| `GET /api/v1/me/notifications/count`, `POST /api/v1/me/notifications/read` | User | Read takes `{ids?}`; empty marks all read. |

### Reports `[VERIFIED]` (ReportsTest)
| Method and path | Access | Notes |
|---|---|---|
| `GET /businesses/{id}/today` | Member | Sections the role may not see are `null` (REP-3). |
| `GET /businesses/{id}/reports/runway?days=30` | `customers.view`, Memberships module | 1 to 90 days; per-day `ending` and `value`, plus `lapsedLast30Days` and `lapsedValue` (REP-4). |
| `GET /businesses/{id}/reports/profit-loss?from=&to=` | `reports.view` | Defaults to the month so far. Series is daily for ranges under 62 days, otherwise monthly. Ranges over a year are refused (REP-1). |
| `GET /me/portfolio` | User | One line per business the user works in (REP-2). |

### Customer portal `[VERIFIED]` (PortalTest)
Public, under `/api/v1/portal/auth` (refresh token in the `mg_prt` cookie):

| Method and path | Notes |
|---|---|
| `GET /business/{code}` | Name, type and whether sign-up is open, for the sign-in page. 404 for an unknown code. |
| `POST /sign-in` | `{businessCode, identifier, password}`. 401 `auth.invalid_credentials` for any mismatch; 423 `auth.locked`. |
| `POST /sign-up` | `{businessCode, fullName, email?, phone?, password}` (email or phone needed). 403 `portal.signup_closed`; 409 `customer.portal_login_taken`. |
| `POST /set-password` | `{token, password}` from an invitation or reset link; signs the customer in. 422 `token.invalid`. |
| `POST /password-reset` | `{businessCode, identifier}`. Always 202. |
| `POST /refresh`, `POST /sign-out` | Cookie-based, like staff. |

Customer token only, under `/api/v1/portal`:

| Method and path | Notes |
|---|---|
| `GET /me` | Profile, business, membership state, covered until, balance due. |
| `GET /memberships` | The customer's own memberships with state. |
| `GET /invoices?page=&size=` | Own invoices, void ones left out. |
| `GET /payments?page=&size=` | Own payments and refunds, void ones left out. |
| `POST /me/password` | `{currentPassword, newPassword}`; ends other sessions. |

Staff, under `/api/v1/businesses/{id}/customers/{customerId}/portal` (`customers.manage`):

| Method and path | Notes |
|---|---|
| `POST /invite` | Turns access on and emails a 7-day link. 422 `portal.email_required`; 409 `customer.portal_login_taken`. |
| `POST /disable` | Turns access off and ends the customer's sessions. |

Business settings (`PUT /businesses/{id}/settings`) also take `allowCustomerSignup` and `renewalReminders` (omit to keep the current value).

### Platform console `[VERIFIED]` (AdminTest)
Under `/api/v1/admin`, for platform administrators only (D-028): an active account whose email is in `PLATFORM_ADMIN_EMAILS` and verified. Anyone else gets 403 `auth.forbidden`; no session gets 401. Read-only. Tenants appear as metadata and counts, never their customers, invoices, payments or amounts.

| Method and path | Notes |
|---|---|
| `GET /access` | 204 for an administrator. The console calls it before showing anything. |
| `GET /overview` | Totals (businesses, accounts, customers, team seats, active businesses and people over 1, 7 and 30 days), sign-ups per week (12 weeks), activity per day (30 days), activation counts, business types, modules switched on, feature use over 30 days. Days and weeks in Asia/Kolkata. |
| `GET /businesses?view=&type=&q=&sort=&page=&size=` | Business directory. `view`: `ALL`, `ACTIVE` (recorded something in 30 days), `DORMANT`, `ARCHIVED`. `q` matches name, code or owner email. `sort`: `NEWEST`, `LAST_ACTIVE`, `CUSTOMERS`, `NAME`. Rows: owner name and email, team size, customer count, modules, last activity. |
| `GET /people?view=&q=&page=&size=` | Owner and staff accounts. `view`: `ALL`, `UNVERIFIED`, `WITHOUT_BUSINESS`, `LOCKED`. Rows: verification, sign-in methods, lock state, businesses, last sign-in, last active. Never a password hash. |
| `GET /health` | Application (version, build time, uptime, memory), database (PostgreSQL version, size, schema version, migrations healthy), integrations (email delivery, Google sign-in), sign-ins in 24 hours, locked accounts, open sessions, unverified accounts, invitations. |

## The contract for clients `[VERIFIED]` (OpenApiContractTest)

* The OpenAPI document at `/v3/api-docs` is the source of the frontend's types (D-025). It is published only when `API_DOCS_ENABLED=true` (local development and tests; off in production, D-030). Regenerate them with `npm run api:types` in `managio_frontend` after any API change.
* Response objects always contain every property. A property is `null` only when the field is marked nullable in the document.
* Requests list only the properties they require; leave optional ones out rather than sending empty strings.
* Roles are returned with permission keys (`customers.view`) and saved with permission names (`CUSTOMERS_VIEW`).
