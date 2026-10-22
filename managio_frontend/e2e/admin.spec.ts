import { expect, test } from "@playwright/test";
import { apiAs } from "./api";
import { ADMIN_EMAIL, createBusiness, email, followsEmailedLinks, PASSWORD, runId, signUp, signInAsAdmin } from "./support";

/**
 * The platform console (D-028): only a listed, verified administrator gets in, everyone else sees nothing of
 * it, and what it shows comes from the API's real figures.
 */
test.describe("platform console", () => {
  test.skip(!followsEmailedLinks, "Needs E2E_API_LOG to verify the administrator's email");

  test("an owner who isn't an administrator sees none of the console, and the API refuses them", async ({ page }) => {
    await signUp(page, "Kiran Owner", email("not-admin"));
    await page.goto("/admin");
    await expect(page.getByText("The platform console is for Managio's administrators")).toBeVisible();
    await expect(page.getByRole("navigation", { name: "Console" })).toHaveCount(0);
    await expect(page.getByRole("heading", { name: "Platform overview" })).toHaveCount(0);

    const api = await apiAs(page);
    for (const path of ["/admin/access", "/admin/overview", "/admin/businesses", "/admin/people", "/admin/health"]) {
      expect((await api.get(path)).status, path).toBe(403);
    }
  });

  test("signed out, the console asks for sign-in and comes back to it", async ({ page }) => {
    await signInAsAdmin(page); // make sure the account exists and is verified
    await page.context().clearCookies();
    await page.goto("/admin/health");
    await expect(page).toHaveURL(/\/sign-in\?next=%2Fadmin%2Fhealth/);
    await page.getByLabel("Email").fill(ADMIN_EMAIL);
    await page.getByLabel("Password").fill(PASSWORD);
    await page.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(page).toHaveURL(/\/admin\/health$/);
    await expect(page.getByRole("heading", { name: "System health" })).toBeVisible();
  });

  test("an administrator sees the real figures, finds a new business and its owner, and checks health", async ({ browser, page }) => {
    // A new owner and business, created the normal way in their own browser.
    const owner = await browser.newContext();
    const ownerPage = await owner.newPage();
    const ownerEmail = email("console-owner");
    const businessName = `Console Check ${runId}`;
    await signUp(ownerPage, "Console Owner", ownerEmail);
    await createBusiness(ownerPage, businessName, "Salon or spa");
    await owner.close();

    await signInAsAdmin(page);
    await page.goto("/admin");
    await expect(page.getByRole("heading", { name: "Platform overview" })).toBeVisible();
    const overview = (await (await apiAs(page)).get<{ totals: { businesses: number; users: number } }>("/admin/overview")).body;
    const figures = page.getByRole("region", { name: "Key figures" });
    await expect(figures.getByText(new Intl.NumberFormat("en-IN").format(overview.totals.businesses), { exact: true }).first()).toBeVisible();
    await expect(figures.getByText(new Intl.NumberFormat("en-IN").format(overview.totals.users), { exact: true }).first()).toBeVisible();

    await page.getByRole("navigation", { name: "Console" }).getByRole("link", { name: "Businesses" }).click();
    await page.getByLabel("Search businesses").fill(businessName);
    const row = page.getByRole("row").filter({ hasText: businessName });
    await expect(row).toHaveCount(1);
    await expect(row).toContainText(ownerEmail);
    await expect(row).toContainText("Salon or spa");
    await expect(row).toContainText("today");

    await page.getByRole("navigation", { name: "Console" }).getByRole("link", { name: "People" }).click();
    await page.getByRole("radio", { name: "Unverified email" }).click();
    await page.getByLabel("Search people").fill(ownerEmail);
    const person = page.getByRole("row").filter({ hasText: ownerEmail });
    await expect(person).toContainText("Unverified");
    await expect(person).toContainText("Password");

    await page.getByRole("navigation", { name: "Console" }).getByRole("link", { name: "System health" }).click();
    await expect(page.getByText("Schema up to date")).toBeVisible();
    await expect(page.getByText(/^PostgreSQL$/)).toBeVisible();
  });
});
