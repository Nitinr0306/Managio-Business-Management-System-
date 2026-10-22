# v1 audit (October 2026)

Findings from reading the v1 codebase (`com.nitin.saas` backend, Next.js 15 frontend) end to end on 2026-10-20, before the v2 rebuild. Each item is `[VERIFIED]` by reading the code path named; none were exploited against a running system. They motivate D-003 and D-006 to D-012 in [DECISIONS.md](DECISIONS.md). This document is historical and does not describe v2.

## Security
1. **Member and user IDs confused.** `RBACService.getCurrentUserId()` returned the member ID for member tokens; `BusinessService.requireAccess` compared it with owner and staff user IDs. A member whose numeric ID equalled an owner's user ID passed the owner's access checks, and `GET /auth/me` returned that user's profile.
2. **Member refresh tokens minted user tokens.** `AuthService.refreshToken` ignored `subject_type` and issued a user access token for the same numeric ID.
3. **Staff privilege escalation.** `StaffInvitationService.inviteStaff` needed only business access and accepted the OWNER staff role; `StaffInvitationResponse` returned invitation tokens to every staff member.
4. **Cross-tenant subscription change.** `PaymentService.recordPayment` reactivated any `subscriptionId` without checking it belonged to the member or business.
5. **Permissions not enforced on members.** Member create, edit, deactivate and import only checked business access; owner revenue dashboards were visible to all staff.
6. **Lockout never persisted.** Failed-attempt counters were written in the login transaction and rolled back by the thrown exception (`AuthService.login`, `StaffAuthService`, `MemberAuthService`).
7. **Secrets in URLs and logs.** Passwords as query parameters (`change-password`, `reset-password`), invitation tokens logged at INFO, a default database password in `application.properties`, inline credentials in `docker-compose.yml`.
8. **Google sign-in not wired.** `OAuth2SuccessHandler` existed but `SecurityConfig` never enabled OAuth login; the handler would have put the access token in a redirect URL.

## Correctness
9. **Token refresh always failed** in the frontend (`//api/v1/auth/refresh` resolved to a host named `api`), signing everyone out after 15 minutes.
10. **Frontend API base URL hardcoded to `/`**, which only worked behind the Docker nginx proxy, not on Vercel.
11. **Member login across businesses.** Members were unique per business but login looked them up globally; the same email in two businesses caused a server error.
12. **Dead or fake UI.** Member portal enable/disable called missing endpoints; revenue trend always empty; "members added today" hardcoded to 0; stored member and staff counters drifted.
13. **Owners could not change staff roles** (required a platform ADMIN role nobody had).

## Structure
14. Three staff permission systems (role sets, per-staff overrides, boolean flags) that disagreed between UI and API.
15. Owner and staff UIs were separate, duplicated page trees.
16. Dead code: no-op tenant interceptor, unused OAuth and cookie classes, two audit interfaces, two `.env` loaders, an unused mail starter, conflicting blacklist beans when Redis was enabled.
17. No automated tests.
