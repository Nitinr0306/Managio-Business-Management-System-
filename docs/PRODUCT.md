# Product

What Managio does and the rules it follows. Every rule has an ID so tests and code reviews can refer to it. A rule is `[PLANNED]` until it is implemented and covered by a test, then `[VERIFIED]` with the test that proves it.

Scope comes from the owner's brief (D-002); the product decisions below were made under that delegation unless marked `[APPROVED]`.

## Users

| Person | Main jobs |
|---|---|
| Owner | See every business at a glance, approve requests, control who can do what, know profit per business. |
| Manager, front desk, cashier | Find customers fast, collect payments, renew plans, record expenses, close the day with a cash count. |
| Trainer, teacher, stylist | Work their tasks, see their customers, mark attendance, raise requests (leave, petty cash). |
| Accountant | Invoices, dues, payments, expenses, payroll, reports. |
| Customer (member, student, client, patient) | See their plan, what they owe, their receipts. |
| Platform administrator (Managio's own team) | See how Managio is used: growth, activity, adoption, which businesses went quiet, who is stuck signing up, and whether the service is healthy (D-028). |

## Pain points the product targets `[DECIDED]`

1. Cash leakage at the counter → every payment has a receipt number and the person who took it; the day book reconciles expected and counted cash.
2. Customers lapse unnoticed → renewal lists and the renewal runway show who ends soon and who lapsed.
3. Untracked dues → invoices with due dates, partial payments and installments give every customer a balance.
4. Approvals over WhatsApp → requests with an approver, decision and audit trail; approved expense claims become expenses.
5. No idea of profit → expenses and payroll are recorded, so each business has a cash-basis profit and loss.
6. Staff overstepping → per-business roles enforced by the API.

## Business types

`[DECIDED]` (D-005). A type sets vocabulary, default modules, default roles and default expense categories. Every type uses the same engine.

| Type | Customer called | Groups called | Plans called | Default optional modules |
|---|---|---|---|---|
| `FITNESS` gym, yoga, dance studio | Member | Batch | Membership plan | Memberships, Attendance, Payroll |
| `SALON` salon, spa | Client | Group | Package | Catalog, Inventory, Memberships, Payroll |
| `COACHING` coaching centre, tuition, academy | Student | Batch | Course | Memberships, Groups, Attendance, Payroll |
| `SCHOOL` school | Student | Class | Fee plan | Memberships, Groups, Attendance, Payroll |
| `CLINIC` clinic, physiotherapy | Patient | Group | Package | Catalog, Memberships, Payroll |
| `RETAIL` shop | Customer | Group | Plan | Catalog, Inventory, Payroll |
| `OTHER` | Customer | Group | Plan | Memberships, Catalog, Payroll |

**Core modules (always on):** Customers, Billing, Expenses, Team, Tasks, Requests, Day book, Reports, Activity.

**Optional modules:** `MEMBERSHIPS` (plans with a duration), `CATALOG` (services and products), `INVENTORY` (stock for products; needs Catalog), `GROUPS`, `ATTENDANCE`, `PAYROLL`.

## Permissions and default roles

`[DECIDED]` (D-007). Permission catalogue:

| Permission | Allows |
|---|---|
| `customers.view` | See customers and their history |
| `customers.manage` | Add and edit customers, manage groups |
| `customers.archive` | Archive and restore customers |
| `customers.import` | Import and export customers as CSV |
| `attendance.mark` | Check customers in, mark group attendance |
| `offerings.manage` | Create and edit plans, services, products and prices; adjust stock |
| `memberships.manage` | Start, renew and cancel memberships |
| `billing.view` | See invoices, payments and dues |
| `billing.collect` | Create invoices, record payments, make sales |
| `billing.discount` | Give discounts without approval |
| `billing.void` | Void invoices and payments, record refunds |
| `expenses.view` | See expenses |
| `expenses.record` | Record and void expenses |
| `daybook.close` | Close the day with a cash count |
| `team.view` | See the team |
| `team.manage` | Invite people, change roles, suspend or remove people, edit roles |
| `payroll.manage` | Prepare payroll and pay salaries |
| `tasks.manage` | Create tasks for anyone and edit any task |
| `requests.approve` | Approve or reject requests |
| `reports.view` | See financial reports and the full dashboard |
| `activity.view` | See the activity log |
| `settings.manage` | Edit business profile, modules and settings |

Default roles for `FITNESS` (other types rename and adjust them in `BusinessTemplates`):

| Role | Permissions |
|---|---|
| Owner | All, not editable |
| Manager | Everything except `billing.void`, `team.manage`, `payroll.manage` and `settings.manage` |
| Front desk | `customers.view`, `customers.manage`, `attendance.mark`, `memberships.manage`, `billing.view`, `billing.collect`, `daybook.close` |
| Trainer | `customers.view`, `attendance.mark` |
| Accountant | `customers.view`, `billing.view`, `billing.collect`, `billing.void`, `expenses.view`, `expenses.record`, `daybook.close`, `payroll.manage`, `reports.view` |

Everyone who is an active team member can, without any permission: see the Today view (content filtered by their permissions), see and update tasks assigned to them, create tasks for themselves, raise requests and see their own requests, and see their own profile.

## Business rules

### Businesses and team
* **BUS-1** `[VERIFIED]` The user who creates a business becomes its owner (a team member with the Owner role). A business code of four digits and four letters is generated and never changes. _Test: BusinessAndTeamTest.creatingABusinessSeedsItsTypeAndMakesTheCreatorOwner._
* **BUS-2** `[VERIFIED]` Creating a business seeds the type's default roles, expense categories and modules. _Test: BusinessAndTeamTest.creatingABusinessSeedsItsTypeAndMakesTheCreatorOwner, schoolsAndShopsGetTheirOwnVocabularyAndRoles._
* **BUS-3** `[VERIFIED]` Archiving a business hides it; its data is kept. Only the owner can archive. _Test: BusinessAndTeamTest.onlyTheOwnerArchivesAndArchivedBusinessesDisappear._
* **TEAM-1** `[VERIFIED]` People join by invitation: an email and a role. Invitations expire after 7 days and are single use. Resending creates a new link and invalidates the old one. _Test: BusinessAndTeamTest.invitationsExpireAfterSevenDaysAndResendReplacesTheLink._
* **TEAM-2** `[VERIFIED]` Accepting an invitation as a new person creates their account (password or Google). An existing account accepts by signing in with the invited email. _Test: BusinessAndTeamTest.anInviteeJoinsWithTheInvitedRole, anExistingAccountMustSignInToAccept._
* **TEAM-3** `[VERIFIED]` The Owner role cannot be given through an invitation or a role change, and the owner cannot be suspended, removed or demoted. Nobody can change their own role. _Test: BusinessAndTeamTest.ownerAndSelfAreProtected, nobodyChangesTheirOwnRoleAndRolesInUseStay._
* **TEAM-4** `[VERIFIED]` Suspended or removed people lose access to that business on their next request. _Test: BusinessAndTeamTest.suspendedPeopleLoseAccessImmediately._
* **TEAM-5** `[VERIFIED]` Roles in use cannot be deleted. The Owner role cannot be edited. _Test: BusinessAndTeamTest.ownerAndSelfAreProtected, nobodyChangesTheirOwnRoleAndRolesInUseStay._
* **TEAM-6** `[DECIDED]` `[VERIFIED]` Only the owner creates, edits and deletes roles. _Test: BusinessAndTeamTest.peopleCanOnlyHandOutAccessTheyHave._
* **TEAM-7** `[DECIDED]` `[VERIFIED]` Someone with `team.manage` can invite people or move someone to a role only if they themselves hold every permission in that role. This stops people granting access they don't have. _Test: BusinessAndTeamTest.peopleCanOnlyHandOutAccessTheyHave._
* **TEAM-8** `[DECIDED]` `[VERIFIED]` Inviting people needs a verified email address, so invitations can't be sent from unconfirmed accounts. Nobody can suspend or remove themselves or the owner. _Test: BusinessAndTeamTest.invitationsNeedAVerifiedSender, ownerAndSelfAreProtected._
* **TEAM-9** `[DECIDED]` `[VERIFIED]` Salaries are visible and editable only to people with `payroll.manage`. _Test: BusinessAndTeamTest.salariesAreVisibleOnlyWithPayrollPermission._

### Accounts
* **IDN-1** `[DECIDED]` `[VERIFIED]` Signing up creates the account, signs the person in and sends a verification email. Unverified accounts can use the app but cannot send invitations (TEAM-8). _Test: AuthFlowTest.signUpSignsInAndSendsVerificationEmail, BusinessAndTeamTest.invitationsNeedAVerifiedSender._
* **IDN-2** `[DECIDED]` `[VERIFIED]` Sign-in shows the same message for an unknown email and a wrong password. Five failed attempts lock the account for 15 minutes. Sign-in attempts are recorded. _Test: AuthFlowTest.wrongPasswordAndUnknownEmailLookTheSame, fiveFailuresLockTheAccountAndTheLockSurvivesTheFailedRequests._
* **IDN-3** `[DECIDED]` `[VERIFIED]` Google sign-in accepts only Google-verified emails. It signs in the account with that Google identity, or links Google to the account with the same email, or creates a new verified account. The same account rules apply as for a password: a disabled account is refused and a locked account stays locked. Linking Google to an account whose email was never verified removes that account's password and ends its sessions, because whoever chose the password never proved they own the address; a verified account keeps its password. A second Google identity with the same email is refused (D-034). _Tests: AuthFlowTest.googleSignInCreatesOrLinksAccounts, googleSignInObeysLockoutAndDisabledAccounts, googleTakesOverAnAccountWhoseEmailWasNeverProven, googleLinksToAVerifiedAccountAndKeepsItsPassword, anInvitationCanBeAcceptedWithGoogleOnlyByTheInvitedAddress; GoogleIdentityVerifierTest. Verified with a real Google account on 2026-10-22 (STATUS.md)._
* **IDN-4** `[DECIDED]` `[VERIFIED]` Asking for a password reset always answers the same way. Reset links work once, for 1 hour. Resetting confirms the email and signs the account out everywhere. _Test: AuthFlowTest.passwordResetRevealsNothingAndEndsOtherSessions._
* **IDN-5** `[DECIDED]` `[VERIFIED]` Sessions last 30 days of inactivity at most; access tokens 15 minutes. Each refresh rotates the refresh token. Reusing an old refresh token more than 30 seconds after it was rotated signs out that whole session family. _Test: AuthFlowTest.refreshRotatesTheCookieAndReuseEndsTheSession, signOutEndsTheSession._
* **IDN-6** `[DECIDED]` `[VERIFIED]` Changing a password needs the current one (unless the account only uses Google) and signs out every other session. _Test: AuthFlowTest.changingPasswordNeedsTheCurrentOne._
* **IDN-7** `[DECIDED]` `[VERIFIED]` Email verification links work for 48 hours. A newer link replaces older ones. _Test: AuthFlowTest.verificationLinkWorksOnce, verificationLinkExpiresAfter48Hours._
* **IDN-8** `[DECIDED]` `[VERIFIED]` Accepting an invitation as a new person creates an account with a verified email (the link proves it). An existing account must be signed in with the invited email to accept. _Test: BusinessAndTeamTest.anInviteeJoinsWithTheInvitedRole, anExistingAccountMustSignInToAccept._

### Customers
* **CUS-1** `[VERIFIED]` Customer numbers are sequential per business and never reused. _Test: CustomerTest.customersAreNumberedPerBusinessAndNormalised._
* **CUS-2** `[VERIFIED]` Customers are archived, never deleted. Archived customers are hidden from lists and search by default and cannot be given new memberships or invoices. _Test: CustomerTest.archivedCustomersAreHiddenAndCanBeRestored._
* **CUS-3** `[VERIFIED]` A customer with portal access must have an email or phone that no other portal-enabled customer in the same business uses. _Test: PortalTest.aPortalLoginBelongsToOneCustomerPerBusiness._
* **CUS-4** `[VERIFIED]` Duplicate warning: adding or editing a customer so that their phone (digits only) matches another active customer in the business returns a conflict naming that customer, unless the request sets `confirmDuplicate`. _Test: CustomerTest.aSharedPhoneNeedsConfirmation._
* **CUS-5** `[DECIDED]` `[VERIFIED]` Customer search matches name, email and customer number; phone numbers by digits once at least three are typed; `#12` means number 12 only. `%` and `_` search for themselves. _Test: CustomerTest.searchFindsByNameNumberPhoneAndEmail._
* **CUS-6** `[DECIDED]` `[VERIFIED]` CSV import adds valid rows and reports skipped rows with their row numbers (blank name, bad email, bad date, phone already in use). Dates are strict (`2026-10-20`, `20/10/2026`). Exports neutralise spreadsheet formulas. _Test: CustomerTest.csvImportAddsGoodRowsAndReportsTheRest, csvExportNeutralisesFormulas._

### Memberships
* **MEM-1** `[VERIFIED]` A membership runs from its start date to its end date inclusive. A 30-day plan starting 20 Oct ends 18 Nov; a 1-month plan starting 20 Oct ends 19 Nov. When the target month is shorter, the end clamps to its last day (3 months from 31 Jan ends 29 Apr). _Test: MembershipTest.dayPlansEndOnTheirLastDayAndMonthPlansOnTheDayBeforeTheAnniversary._
* **MEM-2** `[VERIFIED]` Membership state is computed in the business time zone: Upcoming (starts later), Active (running), Ending soon (active and ends today or in the next 6 days), Expired (ended), Cancelled. _Test: MembershipTest (states in responses), renewalWorklistShowsWhoToChase._
* **MEM-3** `[VERIFIED]` Renewing continues from the day after the current end date if the membership has not expired; otherwise it starts today. The renewal links to the membership it renews. _Test: MembershipTest.renewingContinuesAfterTheEndDateOrStartsTodayWhenExpired._
* **MEM-4** `[VERIFIED]` Starting or renewing a membership creates its invoices. A plan with N installments creates N invoices that split the price evenly (the last absorbs rounding), due at evenly spaced dates from the start date. _Test: MembershipTest.installmentsSplitThePriceAndSpreadTheDueDates, InvoiceMathTest._
* **MEM-5** `[VERIFIED]` A customer can hold several memberships at once, but not two overlapping memberships of the same plan. _Test: MembershipTest.overlappingMembershipsOfTheSamePlanAreRefused._
* **MEM-6** `[VERIFIED]` Cancelling a membership voids its unpaid future invoices. Money already paid is unaffected; refunds are recorded separately. _Test: MembershipTest.cancellingVoidsUnpaidFutureInvoicesButKeepsWhatIsOwed._
* **MEM-7** `[VERIFIED]` Plans that are in use cannot be deleted, only deactivated. Changing a plan's price does not change existing memberships. _Test: MembershipTest.startingCreatesTheInvoiceAndAPriceChangeLaterDoesNotTouchIt, BillingTest.soldItemsCanOnlyBeDeactivatedAndInactiveItemsAreHiddenFromFrontDesk._
* **MEM-8** `[DECIDED]` `[VERIFIED]` A customer's membership status is worked out from all their non-cancelled memberships, so an early renewal counts: Active (covered today and beyond the next 7 days), Ending soon (covered today, coverage ends today or in the next 6 days), Upcoming (only a future membership), Lapsed (had a membership, none covers today or later), No plan (never had one). _Test: CustomerTest.countsMatchTheFiltersAndStatusFollowsTheMembership._
* **MEM-10** `[DECIDED]` `[VERIFIED]` A daily job (09:00 India time) emails each customer whose membership ends within the next 3 days, once per membership. It skips customers without an email, archived customers, cancelled memberships, and customers who already have a later membership. A business can switch reminders off (default on). A failed send is retried on the next run. _Test: ScheduledJobsTest.eachEndingMembershipIsRemindedOnceAndOnlyWhenNobodyHasRenewedIt, remindersCanBeSwitchedOffPerBusiness, remindersFollowEachBusinessesOwnDate._
* **MEM-9** `[DECIDED]` `[VERIFIED]` A discount given when starting or renewing a membership applies to its first invoice. _Test: MembershipTest.aDiscountAppliesToTheFirstInvoiceOnly._

### Billing
* **BIL-1** `[VERIFIED]` Invoice total = lines subtotal − discount + tax. Prices are before tax; each line's tax rate is added on top. Discount applies to the whole invoice and reduces the taxable value proportionally. Amounts round half-up to 2 decimals. _Test: InvoiceMathTest, BillingTest.anInvoiceTotalsItsLinesDiscountAndTax._
* **BIL-9** `[DECIDED]` `[VERIFIED]` Plans are sold only by starting or renewing a membership, never as a plain invoice line, so every plan sale has dates. Custom lines (a description and a price) are allowed for anything not in the catalogue. Changing a catalogue item's price on an invoice needs `billing.discount`. _Test: BillingTest.discountsAndPriceChangesNeedThePermissionAndPlansNeedMemberships._
* **BIL-11** `[DECIDED]` `[VERIFIED]` An invoice without a customer is only possible as a sale paid in full, because nobody could owe the rest. _Test: BillingTest.salesRecordTheInvoiceAndPaymentTogetherEvenForWalkIns._
* **BIL-10** `[DECIDED]` `[VERIFIED]` A customer's balance due is the unpaid amount of invoices due today or earlier; later installments are shown separately as upcoming. _Test: MembershipTest.installmentsSplitThePriceAndSpreadTheDueDates, BillingTest.anInvoiceMovesFromDueToPartlyPaidToOverdue._
* **BIL-2** `[VERIFIED]` Invoice state: Void, or computed from payments and dates: Paid (paid ≥ total), Overdue (unpaid part and due date before today, even if partly paid), Partly paid (some paid, not yet due), Due. _Test: BillingTest.anInvoiceMovesFromDueToPartlyPaidToOverdue, InvoiceMathTest._
* **BIL-3** `[VERIFIED]` A payment is always recorded against an invoice and cannot exceed the invoice's balance. A sale (invoice and payment together) is one action. _Test: BillingTest.paymentsCannotExceedTheBalanceAndReceiptsAreNumbered, salesRecordTheInvoiceAndPaymentTogetherEvenForWalkIns._
* **BIL-4** `[VERIFIED]` Giving a discount needs `billing.discount`; others raise a discount request. _Test: BillingTest.discountsAndPriceChangesNeedThePermissionAndPlansNeedMemberships._
* **BIL-5** `[VERIFIED]` Voiding needs `billing.void` and a reason. An invoice with valid payments cannot be voided until those are voided or refunded. _Test: BillingTest.voidingNeedsThePermissionAReasonAndNoMoneyLeft._
* **BIL-6** `[VERIFIED]` A refund is recorded against a payment, cannot exceed what is left of it, and reduces the invoice's paid amount. _Test: BillingTest.refundsReduceWhatWasPaidAndCannotExceedThePayment._
* **BIL-7** `[VERIFIED]` Invoice and receipt numbers are sequential per business and shown with the business's prefixes (INV-0042, RCT-0108). _Test: BillingTest.paymentsCannotExceedTheBalanceAndReceiptsAreNumbered, MembershipTest (INV-0001..)._
* **BIL-8** `[VERIFIED]` Selling a product with stock tracking reduces its stock; voiding the invoice restores it. Stock may not go below zero. _Test: BillingTest.stockFollowsSalesAndIsRestoredWhenAnInvoiceIsVoided._

### Expenses and payroll
* **EXP-1** `[VERIFIED]` People with `expenses.record` record expenses directly. Others raise an expense request; approving it records the expense. _Test: OperationsTest.expenseValidationPermissionsAndCategories, anApprovedExpenseClaimBecomesAnExpenseAndNotifiesBothSides._
* **EXP-2** `[VERIFIED]` Expenses are voided with a reason, never deleted. _Test: OperationsTest.expensesAreRecordedVoidedAndSummarised._
* **EXP-3** `[VERIFIED]` Expense categories are per business, start from the business type's list, can be read by everyone on the team (an expense claim needs one), and are changed only by people with `settings.manage`. Archived categories can't take new expenses; the Salaries category can't be archived. _Test: OperationsTest.expenseValidationPermissionsAndCategories._
* **EXP-4** `[VERIFIED]` An expense date can't be in the future. _Test: OperationsTest.expenseValidationPermissionsAndCategories._
* **PAY-1** `[VERIFIED]` Preparing payroll for a month creates one entry per active team member with a monthly salary, once per person and month. _Test: OperationsTest.salariesArePreparedPaidInPartsAndTrackedThroughExpenses, peopleWhoLeftOrHaveNoSalaryAreNotInPayroll._
* **PAY-2** `[VERIFIED]` Paying a payroll entry records a Salaries expense linked to the entry. An entry can be paid in parts; its state (Unpaid, Partly paid, Paid) is computed. _Test: OperationsTest.salariesArePreparedPaidInPartsAndTrackedThroughExpenses._

### Requests and approvals
* **REQ-1** `[VERIFIED]` Any active team member can raise a request: expense claim, purchase, leave, discount or other. Only the person who raised it can cancel it, and only while pending. _Test: OperationsTest.requestsAreCheckedPerTypeAndCancelledOnlyByTheRequester._
* **REQ-2** `[VERIFIED]` People with `requests.approve` decide requests. Nobody decides their own request. _Test: OperationsTest.nobodyDecidesTheirOwnRequestAndRejectionsNeedAReason._
* **REQ-3** `[VERIFIED]` Approving an expense claim records the expense dated today (payee: the requester; the approver chooses how it was paid, which is required). Approving a discount request adds the discount to the linked invoice if it has no payments and isn't void; otherwise approval fails and the request stays pending. Purchase, leave and other requests are approval-only. If recording fails (for example cash on a closed day) nothing changes and the request stays pending. _Test: OperationsTest.anApprovedExpenseClaimBecomesAnExpenseAndNotifiesBothSides, anApprovedDiscountReducesAnUnpaidInvoice, approvingACashExpenseOnAClosedDayFailsAndLeavesTheRequestPending._
* **REQ-5** `[VERIFIED]` Requesters see only their own requests; people with `requests.approve` see everyone's. A rejection needs a written reason. A decided or cancelled request can't be decided again. _Test: OperationsTest.requestsAreOnlyVisibleToTheRequesterAndApprovers, nobodyDecidesTheirOwnRequestAndRejectionsNeedAReason._
* **REQ-4** `[VERIFIED]` The requester is notified of the decision; approvers are notified of new requests. _Test: OperationsTest.anApprovedExpenseClaimBecomesAnExpenseAndNotifiesBothSides._

### Day book
* **DAY-1** `[VERIFIED]` For a business date: cash in = cash payments − cash refunds; cash out = cash expenses; opening cash = counted cash of the most recent earlier close (0 if none); expected = opening + in − out. _Test: OperationsTest.theDayBookCountsCashAndClosingLocksCashForThatDay._
* **DAY-2** `[VERIFIED]` Closing the day records the counted cash and the difference. One close per date. _Test: OperationsTest.theDayBookCountsCashAndClosingLocksCashForThatDay._
* **DAY-3** `[VERIFIED]` Cash payments, refunds and expenses cannot be recorded or voided on a date that is already closed. _Test: OperationsTest.theDayBookCountsCashAndClosingLocksCashForThatDay, approvingACashExpenseOnAClosedDayFailsAndLeavesTheRequestPending._

### Tasks
* **TSK-1** `[VERIFIED]` People with `tasks.manage` create tasks for anyone and edit any task. Everyone else creates tasks for themselves and updates the status of tasks assigned to them. _Test: OperationsTest.tasksFollowWhoOwnsThem._
* **TSK-2** `[VERIFIED]` Finishing a repeating task (daily, weekly, monthly) creates the next occurrence, due one day, week or month after the finished one's due date (today if it had none). Setting an already-finished task to done again creates nothing. _Test: OperationsTest.finishingARepeatingTaskCreatesTheNextOne._
* **TSK-3** `[VERIFIED]` With `tasks.manage` you see every task. Without it you see tasks assigned to you or created by you. A task's assignee must be an active team member of the same business. _Test: OperationsTest.tasksFollowWhoOwnsThem, tasksCanOnlyBeAssignedToActiveTeamMembersOfTheSameBusiness._

### Attendance
* **ATT-1** `[VERIFIED]` One check-in per customer per business date. Checking in a customer without a running membership is allowed and flagged in the response. _Test: CustomerTest.oneCheckInPerDayAndRollCallReplacesThePresentList._

### Customer portal
* **POR-1** `[VERIFIED]` Customers sign in with their business code and their email or phone plus password. _Test: PortalTest.anInvitedCustomerSetsAPasswordOnceAndSignsInWithEmailOrPhone._
* **POR-2** `[VERIFIED]` Staff turn on portal access for a customer by sending an email invitation to set a password. Businesses that allow it let customers sign themselves up with the business code. _Test: PortalTest.anInvitedCustomerSetsAPasswordOnceAndSignsInWithEmailOrPhone, invitingNeedsAnEmailAndTheRightPermission, selfSignUpIsOffUntilTheBusinessAllowsItAndNeverAttachesToAnExistingCustomer._
* **POR-3** `[VERIFIED]` Customers see only their own memberships, invoices, payments and profile. Void invoices and void payments are not shown. No portal endpoint takes a customer or business ID from the request; both come from the signed-in token. _Test: PortalTest.aCustomerSeesOnlyTheirOwnRecords, portalAndStaffTokensDoNotOpenEachOthersDoors._
* **POR-4** `[VERIFIED]` Portal sign-in uses the same protections as staff sign-in: one message for an unknown business code, an unknown login and a wrong password; 5 failed attempts lock the customer for 15 minutes; per-address rate limits. Email and phone matches ignore case and formatting. _Test: PortalTest.failedSignInsLockTheAccountAndNothingRevealsWhichPartWasWrong._
* **POR-5** `[VERIFIED]` Self sign-up always creates a new customer. It never attaches to an existing customer who has the same email or phone, because nobody has proven the email or phone is theirs; staff can combine records by hand. _Test: PortalTest.selfSignUpIsOffUntilTheBusinessAllowsItAndNeverAttachesToAnExistingCustomer._
* **POR-6** `[VERIFIED]` Turning portal access off, or archiving the customer, takes effect on their next request and ends their sessions. The invitation link works for 7 days and once; a password-reset link works for 1 hour and once. Setting a password ends all earlier sessions. _Test: PortalTest.turningAccessOffOrArchivingEndsItAtOnce, passwordResetEmailsOnlyRealAccountsAndEndsOldSessions, aCustomerChangesTheirPasswordWithTheCurrentOne._

### Reports
* **REP-1** `[VERIFIED]` Revenue = payments − refunds by paid date; expenses by spent date; profit = revenue − expenses (cash basis). Void records are excluded. _Test: ReportsTest.profitAndLossIsCashBasedAndIgnoresVoidedRecords._
* **REP-2** `[VERIFIED]` Anyone who works in two or more businesses sees a portfolio view with one line per business: this month's revenue, expenses and profit (if their role there has `reports.view`), memberships ending in the next 7 days (`customers.view`), overdue dues (`billing.view`), requests waiting (`requests.approve`) and their own open tasks. Figures their role there doesn't allow are left out, and the totals add up only the businesses where they may see reports. _Test: ReportsTest.thePortfolioComparesBusinessesAndHidesWhatARoleCannotSee._
* **REP-3** `[VERIFIED]` The Today view is open to every team member. Its sections follow permissions: renewals (ending today, ending in the next 7 days, lapsed in the last 30 days and not renewed) and new customers this month need `customers.view`; dues (open and overdue invoices, with amounts) need `billing.view`; takings need `billing.view` and `expenses.view`; whether the day is closed needs `daybook.close`; requests waiting need `requests.approve`. Everyone sees their own open and overdue tasks. _Test: ReportsTest.todayShowsOnlyWhatTheRoleMaySee, overdueDuesAndMyTasksAppearOnceTheirDatesPass._
* **REP-4** `[VERIFIED]` The renewal runway counts, for each of the next 1 to 90 days, the memberships of active customers that end that day and have not been renewed, with their combined price. Renewing, or cancelling, takes a membership off the runway. It needs `customers.view` and the Memberships module. _Test: ReportsTest.theRunwayCountsMembershipsEndingEachDayUntilTheyAreRenewed._

### Platform console
Managio is free in v2 (D-028): there are no plans, prices or subscriptions for businesses, and the console shows none.
* **ADM-1** `[VERIFIED]` Only an active account whose email is on the server's administrator list and verified can use the console; anyone else is refused (403), with no session 401, and a customer's portal session is refused too. Disabling the account ends access at once. _Test: AdminTest.onlyAListedVerifiedActiveAccountGetsIn, aPortalCustomerCannotGetIn; e2e admin.spec.ts._
* **ADM-2** `[VERIFIED]` The overview counts real records: open businesses, accounts, customers and team seats; new ones in the last 30 days; businesses that recorded something and people who signed in or used the app in the last 1, 7 and 30 days; sign-ups per week for 12 weeks; activity per day for 30 days; business types; modules switched on; use of each product area over 30 days; and how many businesses have added a customer, recorded a payment, added a teammate and been active in the last 7 days. _Test: AdminTest.theOverviewCountsWhatHappened._
* **ADM-3** `[VERIFIED]` The business directory lists each business with its owner's name and email, team size, customer count, modules and last activity, filtered by active, dormant (nothing recorded in 30 days) or archived, by type and by name, code or owner email. The people directory lists owner and staff accounts with verification, sign-in methods, lock state, businesses and last activity, filtered by unverified, without a business or locked. Neither ever shows a business's customers, invoices, payments or amounts. _Test: AdminTest.theDirectoriesShowAccountsAndBusinessesButNeverTheirCustomers._
* **ADM-4** `[VERIFIED]` System health shows the application version and uptime, the database version, size and migration state, whether email is really sent and Google sign-in configured, sign-ins and failures in 24 hours, locked accounts, open sessions, accounts unverified for over two days and invitations waiting or expired. _Test: AdminTest.healthReportsTheRealState._
