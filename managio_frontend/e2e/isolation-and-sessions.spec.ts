import { expect, test } from "@playwright/test";
import { apiAs, businessIdFrom } from "./api";
import { addMember, createBusiness, email, followsEmailedLinks, lastEmailToken, nav, PASSWORD, runId, signIn, signUp } from "./support";

/**
 * Tenant isolation, roles and sessions: two separate owners can't reach each other's data through the UI or the
 * API, people only open the screens their role allows, and signing out leaves nothing behind for the next person.
 */
test.describe.serial("isolation and sessions", () => {
  const ownerA = email("tenant-a");
  const ownerB = email("tenant-b");
  let businessA = 0;
  let customerA = 0;

  test("set-up: two owners with a business each", async ({ page }) => {
    await signUp(page, "Arjun Mehra", ownerA);
    businessA = await createBusiness(page, `Alpha ${runId}`, "Salon or spa");
    await addMember(page, "Kavya Rao", { phone: "9822233344" }, "Client");
    const api = await apiAs(page);
    const customers = await api.get<{ items: { id: number }[] }>(`/businesses/${businessA}/customers`);
    customerA = customers.body.items[0]?.id ?? 0;

    await page.context().clearCookies();
    await signUp(page, "Bela Singh", ownerB);
    await createBusiness(page, `Beta ${runId}`, "Shop");
  });

  test("another owner can't open the business, its records or its reports", async ({ page }) => {
    await signIn(page, ownerB);
    await expect(page.getByRole("heading", { name: /^Good/ })).toBeVisible();
    await page.goto(`/app/${businessA}`);
    await expect(page.getByText("You don't have access to this business")).toBeVisible();
    await page.goto(`/app/${businessA}/billing`);
    await expect(page.getByText("You don't have access to this business")).toBeVisible();

    const api = await apiAs(page);
    for (const path of [
      `/businesses/${businessA}`,
      `/businesses/${businessA}/customers`,
      `/businesses/${businessA}/customers/${customerA}`,
      `/businesses/${businessA}/invoices`,
      `/businesses/${businessA}/payments`,
      `/businesses/${businessA}/team`,
      `/businesses/${businessA}/reports/profit-loss`,
      `/businesses/${businessA}/today`,
    ]) {
      expect((await api.get(path)).status, path).toBe(404);
    }
    expect((await api.post(`/businesses/${businessA}/customers`, { fullName: "Intruder" })).status).toBe(404);
    expect((await api.post(`/businesses/${businessA}/expenses`, { categoryId: 1, amount: 10, method: "CASH" })).status).toBe(404);
  });

  test("the portfolio lists only the owner's own businesses", async ({ page }) => {
    await signIn(page, ownerB);
    await expect(page.getByRole("heading", { name: /^Good/ })).toBeVisible();
    const api = await apiAs(page);
    const mine = (await api.get<{ id: number; name: string }[]>("/me/businesses")).body;
    expect(mine.map((b) => b.name)).toEqual([`Beta ${runId}`]);
  });

  test("signing out and in as someone else leaves nothing behind", async ({ page }) => {
    await signIn(page, ownerA);
    await expect(page.getByText(`Alpha ${runId}`).first()).toBeVisible();
    await page.getByRole("button", { name: /Arjun Mehra/ }).click();
    await page.getByRole("menuitem", { name: "Sign out" }).click();
    await expect(page).toHaveURL(/\/sign-in/);

    await page.getByLabel("Email").fill(ownerB);
    await page.getByLabel("Password").fill(PASSWORD);
    await page.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(page.getByText(`Beta ${runId}`).first()).toBeVisible();
    await expect(page.getByText(`Alpha ${runId}`)).toHaveCount(0);
    await expect(page.getByText("Kavya Rao")).toHaveCount(0);
  });

  test("without a session, app pages send people to sign in and bring them back", async ({ page }) => {
    await page.goto(`/app/${businessA}/customers`);
    await expect(page).toHaveURL(new RegExp(`/sign-in\\?next=%2Fapp%2F${businessA}%2Fcustomers`));
    await page.getByLabel("Email").fill(ownerA);
    await page.getByLabel("Password").fill(PASSWORD);
    await page.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(page).toHaveURL(new RegExp(`/app/${businessA}/customers$`));
  });

  test("an ended session is noticed and leads back to sign in", async ({ page }) => {
    await signIn(page, ownerA);
    await expect(page.getByRole("heading", { name: /^Good/ })).toBeVisible();
    // The refresh cookie is revoked elsewhere (for example a sign-out on another device).
    const refreshCookie = (await page.context().cookies()).find((c) => c.name === "mg_rt");
    expect(refreshCookie).toBeTruthy();
    await page.request.post("/api/v1/auth/sign-out");
    await page.context().addCookies(refreshCookie ? [refreshCookie] : []);
    await page.reload();
    await expect(page).toHaveURL(/\/sign-in/);
  });

  test("a direct link to a screen outside the role shows a clear message", async ({ page }) => {
    test.skip(!followsEmailedLinks, "Needs E2E_API_LOG to follow the invitation link");
    await signIn(page, ownerA);
    await page.goto(`/verify-email?token=${lastEmailToken(ownerA)}`);
    await expect(page.getByRole("heading", { name: "Email confirmed" })).toBeVisible();
    await page.goto(`/app/${businessA}`);
    await nav(page, "Team");
    await page.getByRole("button", { name: "Invite someone" }).click();
    const staff = email("stylist");
    await page.getByRole("dialog").getByLabel("Their email").fill(staff);
    await page.getByRole("dialog").getByLabel("Role").selectOption({ label: "Stylist" });
    await page.getByRole("dialog").getByRole("button", { name: "Send invitation" }).click();
    await expect(page.getByText(`Invitation sent to ${staff}`)).toBeVisible();

    await page.context().clearCookies();
    await page.goto(`/invite?token=${lastEmailToken(staff)}`);
    await page.getByLabel("Your name").fill("Tara Joseph");
    await page.getByLabel("Choose a password").fill(PASSWORD);
    await page.getByRole("button", { name: /^Join / }).click();
    await expect(page.getByRole("heading", { name: /^Good .*Tara/ })).toBeVisible();
    expect(businessIdFrom(page)).toBe(businessA);

    await page.goto(`/app/${businessA}/billing`);
    await expect(page.getByText("This screen isn't part of your role")).toBeVisible();
    const api = await apiAs(page);
    expect((await api.get(`/businesses/${businessA}/invoices`)).status).toBe(403);
    expect((await api.get(`/businesses/${businessA}/customers`)).status).toBe(200);
  });
});
