import { expect, test } from "@playwright/test";
import { apiAs } from "./api";
import { addMember, addPlan, createBusiness, email, nav, openCustomer, runId, signIn, signUp } from "./support";

type Membership = { id: number; startsOn: string; endsOn: string; state: string; renewedFromId: number | null };
type Invoice = { id: number; total: number; paid: number; balance: number; state: string; invoiceNumber: string };
type Page_<T> = { items: T[]; totalItems: number };

/**
 * Money and day-to-day operations end to end, checking what each UI action stored through the API:
 * renewals, refunds, voids, sales, attendance, payroll, reports and settings.
 */
test.describe.serial("money and operations", () => {
  const owner = email("money");
  let businessId = 0;
  let customerId = 0;

  test("set-up: a gym with a monthly plan and a fully paid member", async ({ page }) => {
    await signUp(page, "Farah Khan", owner);
    businessId = await createBusiness(page, `Peak ${runId}`, "Gym or fitness studio");
    await addPlan(page, "Monthly", "1200");
    await addMember(page, "Isha Patel", { phone: "9811122233" });
    const panel = page.getByRole("dialog", { name: "Isha Patel" });
    await panel.getByRole("button", { name: "Start a membership plan" }).click();
    await page.getByRole("dialog", { name: /Start a membership plan/ }).getByRole("button", { name: /Start and take ₹1,200/ }).click();
    await expect(panel.getByRole("button", { name: "Renew early" })).toBeVisible();

    const api = await apiAs(page);
    const customers = await api.get<Page_<{ id: number; fullName: string; balanceDue: number }>>(`/businesses/${businessId}/customers?q=Isha`);
    customerId = customers.body.items[0].id;
    expect(customers.body.items[0].balanceDue).toBe(0);
  });

  test("renewing early continues from the day after the current plan ends", async ({ page }) => {
    await signIn(page, owner);
    const panel = await openCustomer(page, "Isha Patel");
    await panel.getByRole("button", { name: "Renew early" }).click();
    const renew = page.getByRole("dialog", { name: "Renew Isha Patel" });
    await expect(renew.getByText(/no days are lost/)).toBeVisible();
    await renew.getByLabel("Method").selectOption("UPI");
    await renew.getByRole("button", { name: /Renew and take ₹1,200/ }).click();
    await expect(page.getByText("Renewed Isha Patel's Monthly")).toBeVisible();

    const api = await apiAs(page);
    const memberships = (await api.get<Membership[]>(`/businesses/${businessId}/customers/${customerId}/memberships`)).body;
    const first = memberships.find((m) => m.renewedFromId == null);
    const second = memberships.find((m) => m.renewedFromId != null);
    if (!first || !second) throw new Error("Expected the plan and its renewal");
    const dayAfter = new Date(`${first.endsOn}T00:00:00Z`);
    dayAfter.setUTCDate(dayAfter.getUTCDate() + 1);
    expect(second.startsOn).toBe(dayAfter.toISOString().slice(0, 10));
    expect(second.state).toBe("UPCOMING");
  });

  test("a refund gives part of a payment back and the invoice owes it again", async ({ page }) => {
    await signIn(page, owner);
    await nav(page, "Billing");
    await page.getByRole("tab", { name: "Payments" }).click();
    const row = page.getByRole("row", { name: /Isha Patel.*UPI.*₹1,200/ });
    await row.getByRole("button", { name: /Actions for/ }).click();
    await page.getByRole("menuitem", { name: "Refund" }).click();
    const dialog = page.getByRole("dialog", { name: /Refund from/ });
    await dialog.getByLabel("Amount").fill("200");
    await dialog.getByLabel("Reason").fill("Two weeks of the plan returned");
    await dialog.getByRole("button", { name: "Refund" }).click();
    await expect(page.getByText("Refunded ₹200")).toBeVisible();
    await expect(page.getByRole("row", { name: /Refund.*Isha Patel.*−₹200/ })).toBeVisible();

    const api = await apiAs(page);
    const open = (await api.get<Page_<Invoice>>(`/businesses/${businessId}/invoices?view=OPEN&customerId=${customerId}`)).body.items;
    expect(open).toHaveLength(1);
    expect(open[0]).toMatchObject({ paid: 1000, balance: 200, state: "PARTLY_PAID" });
  });

  test("a sale entered by mistake is voided: payment first, then the invoice", async ({ page }) => {
    await signIn(page, owner);
    await nav(page, "Billing");
    await page.getByRole("button", { name: "New sale or invoice" }).click();
    const sale = page.getByRole("dialog", { name: "New sale or invoice" });
    await sale.getByLabel("Line 1 description").fill("Locker for October");
    await sale.getByLabel("Line 1 price").fill("300");
    await sale.getByRole("button", { name: "Record sale" }).click();
    await expect(page.getByText("Sold ₹300")).toBeVisible();

    const api = await apiAs(page);
    const lockerInvoice = (await api.get<Page_<Invoice & { customer: unknown }>>(`/businesses/${businessId}/invoices?view=PAID&size=50`)).body.items.find(
      (i) => i.total === 300,
    );
    if (!lockerInvoice) throw new Error("The sale's invoice should exist");
    await page.goto(`/app/${businessId}/billing/invoices/${lockerInvoice.id}`);
    await expect(page.getByRole("heading", { name: new RegExp(lockerInvoice.invoiceNumber) })).toBeVisible();

    // The invoice can't be voided while money sits on it.
    await page.getByRole("button", { name: "More actions" }).click();
    await expect(page.getByRole("menuitem", { name: /Void \(refund or void its payments first\)/ })).toHaveAttribute("data-disabled", "");
    await page.keyboard.press("Escape");

    await page.getByRole("button", { name: /Actions for RCT/ }).click();
    await page.getByRole("menuitem", { name: /Void \(entered by mistake\)/ }).click();
    await page.getByRole("dialog", { name: /Void RCT/ }).getByLabel("Reason").fill("Wrong customer");
    await page.getByRole("button", { name: "Void payment" }).click();
    await expect(page.getByText(/RCT-\d+ voided/)).toBeVisible();

    await page.getByRole("button", { name: "More actions" }).click();
    await page.getByRole("menuitem", { name: "Void invoice" }).click();
    await page.getByRole("dialog", { name: /Void INV/ }).getByLabel("Reason").fill("Entered by mistake");
    await page.getByRole("button", { name: "Void invoice" }).click();
    await expect(page.getByText("Void", { exact: true }).first()).toBeVisible();
    const after = (await api.get<Invoice>(`/businesses/${businessId}/invoices/${lockerInvoice.id}`)).body;
    expect(after.state).toBe("VOID");
    expect(after.paid).toBe(0);
  });

  test("checking a member in shows on today's attendance", async ({ page }) => {
    await signIn(page, owner);
    await nav(page, "Attendance");
    await page.getByPlaceholder("Type a member's name, phone or #number").fill("Isha");
    await page.getByRole("option", { name: /Isha Patel/ }).getByRole("button").click();
    await expect(page.getByText("Isha Patel checked in")).toBeVisible();
    await expect(page.getByRole("button", { name: /Isha Patel #\d+/ })).toBeVisible();
  });

  test("payroll is prepared from salaries and paid as a Salaries expense", async ({ page }) => {
    await signIn(page, owner);
    await nav(page, "Team");
    await page.getByRole("row", { name: /Farah Khan/ }).getByRole("button", { name: "Edit" }).click();
    const dialog = page.getByRole("dialog", { name: "Edit Farah Khan" });
    await dialog.getByLabel("Monthly salary").fill("20000");
    await dialog.getByRole("button", { name: "Save" }).click();
    await expect(page.getByRole("row", { name: /Farah Khan.*₹20,000/ })).toBeVisible();

    await nav(page, "Payroll");
    await page.getByRole("button", { name: /^Prepare / }).first().click();
    await expect(page.getByText(/payroll is ready: 1 person/)).toBeVisible();
    await page.getByRole("row", { name: /Farah Khan/ }).getByRole("button", { name: "Pay" }).click();
    const pay = page.getByRole("dialog", { name: "Pay Farah Khan" });
    await pay.getByLabel("Amount").fill("5000");
    await pay.getByRole("button", { name: "Record payment" }).click();
    await expect(page.getByRole("row", { name: /Farah Khan.*₹20,000.*₹5,000.*₹15,000.*Part paid/ })).toBeVisible();

    await nav(page, "Expenses");
    await expect(page.getByRole("row", { name: /Salaries.*Salary.*Farah Khan.*₹5,000/ })).toBeVisible();
  });

  test("this month's report adds up what came in and went out", async ({ page }) => {
    await signIn(page, owner);
    const api = await apiAs(page);
    const report = (await api.get<{ revenue: number; expenses: number; profit: number }>(`/businesses/${businessId}/reports/profit-loss`)).body;
    // 1,200 + 1,200 taken, 200 refunded, the voided 300 doesn't count; 5,000 salary paid.
    expect(report).toMatchObject({ revenue: 2200, expenses: 5000, profit: -2800 });
    await nav(page, "Reports");
    await expect(page.getByText("₹2,200").first()).toBeVisible();
    await expect(page.getByText("₹5,000").first()).toBeVisible();
    await expect(page.getByText("More went out than came in")).toBeVisible();
  });

  test("settings persist and change new invoice numbers", async ({ page }) => {
    await signIn(page, owner);
    await nav(page, "Settings");
    await page.getByLabel("Invoice prefix").fill("PK");
    await page.getByLabel("Renewal reminder emails").uncheck();
    await page.getByRole("button", { name: "Save settings" }).click();
    await expect(page.getByText("Settings saved")).toBeVisible();
    await page.reload();
    await expect(page.getByLabel("Invoice prefix")).toHaveValue("PK");
    await expect(page.getByLabel("Renewal reminder emails")).not.toBeChecked();

    await nav(page, "Billing");
    await page.getByRole("radio", { name: "All" }).click();
    await expect(page.getByRole("row", { name: /^PK-/ }).first()).toBeVisible();
  });

  test("the activity log records what happened", async ({ page }) => {
    await signIn(page, owner);
    await nav(page, "Activity log");
    await expect(page.getByText(/Isha Patel/).first()).toBeVisible();
    await expect(page.getByText(/voided|Voided/).first()).toBeVisible();
  });

  test("the account name can be changed and stays changed", async ({ page }) => {
    await signIn(page, owner);
    await page.goto("/app/account");
    await page.getByLabel("Name").fill("Farah K. Khan");
    await page.getByRole("button", { name: "Save", exact: true }).click();
    await expect(page.getByText("Saved")).toBeVisible();
    await page.reload();
    await expect(page.getByLabel("Name")).toHaveValue("Farah K. Khan");
  });

  test("an early payment for a later instalment can be taken once, even when submitted twice", async ({ page }) => {
    await signIn(page, owner);
    const panel = await openCustomer(page, "Isha Patel");
    // The refund left ₹200 on the renewal's invoice, which isn't due yet: nothing is owed today.
    await expect(panel.getByText("₹200 in later instalments")).toBeVisible();
    await panel.getByRole("button", { name: "More actions" }).click();
    await page.getByRole("menuitem", { name: "Take an early payment" }).click();
    const pay = page.getByRole("dialog", { name: "Record payment from Isha Patel" });
    const submit = pay.getByRole("button", { name: "Record payment" });
    await submit.dblclick();
    await expect(page.getByText("Recorded ₹200 from Isha Patel")).toBeVisible();
    const api = await apiAs(page);
    const payments = (await api.get<Page_<{ amount: number; kind: string; voided: boolean }>>(`/businesses/${businessId}/payments?customerId=${customerId}&size=50`)).body.items;
    expect(payments.filter((p) => p.amount === 200 && p.kind === "PAYMENT" && !p.voided)).toHaveLength(1);
  });
});
