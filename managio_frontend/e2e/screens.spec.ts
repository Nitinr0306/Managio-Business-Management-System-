import { expect, test } from "@playwright/test";
import { apiAs } from "./api";
import { addMember, addPlan, createBusiness, email, followsEmailedLinks, lastEmailToken, nav, PASSWORD, runId, signIn, signUp } from "./support";

/**
 * The remaining screens, each exercised through its main actions: password reset, the customer list (filters,
 * export and import), renewals, the portfolio and business switching, the catalogue, groups and roll call,
 * custom roles, and portal self sign-up and reset.
 */
test.describe.serial("every other screen", () => {
  const owner = email("screens");
  let gymId = 0;
  let coachingId = 0;
  let gymCode = "";

  test("forgotten password: the emailed link sets a new one", async ({ page }) => {
    test.skip(!followsEmailedLinks, "Needs E2E_API_LOG to follow the reset link");
    await signUp(page, "Neha Gupta", owner);
    gymId = await createBusiness(page, `Core ${runId}`, "Gym or fitness studio");
    await page.context().clearCookies();

    await page.goto("/forgot-password");
    await page.getByLabel("Email").fill(owner);
    await page.getByRole("button", { name: "Send reset link" }).click();
    await expect(page.getByRole("heading", { name: "Check your email" })).toBeVisible();
    await page.goto(`/reset-password?token=${lastEmailToken(owner)}`);
    await page.getByLabel("New password").fill("new-tulip-77");
    await page.getByRole("button", { name: "Save password" }).click();
    await expect(page.getByRole("heading", { name: "Password changed" })).toBeVisible();

    await signIn(page, owner, PASSWORD, { expectFailure: true });
    await expect(page.getByRole("alert").filter({ hasText: "match" })).toBeVisible();
    await signIn(page, owner, "new-tulip-77");
    await expect(page.getByRole("heading", { name: /^Good/ })).toBeVisible();
  });

  test("the customer list filters by standing, searches, exports and imports", async ({ page }) => {
    await signIn(page, owner, "new-tulip-77");
    await addPlan(page, "Trial week", "300", "1");
    for (const name of ["Aarav Shah", "Bhavna Iyer", "Chetan Rao"]) {
      await addMember(page, name);
      await page.keyboard.press("Escape");
    }
    await nav(page, "Members");
    await expect(page.getByRole("radio", { name: /All\s*3/ })).toBeVisible();
    await page.getByLabel("Filter members").fill("bhav");
    await expect(page.getByRole("row")).toHaveCount(2);
    await expect(page.getByRole("row", { name: /Bhavna Iyer/ })).toBeVisible();
    await page.getByLabel("Filter members").fill("");
    await page.getByRole("radio", { name: /No plan/ }).click();
    await expect(page.getByRole("row", { name: /Aarav Shah/ })).toBeVisible();

    const download = page.waitForEvent("download");
    await page.getByRole("button", { name: "Export" }).click();
    expect((await download).suggestedFilename()).toMatch(/^members-.*\.csv$/);

    await page.getByRole("button", { name: "Import" }).click();
    const dialog = page.getByRole("dialog", { name: "Import members" });
    await dialog.getByLabel("CSV file").setInputFiles({
      name: "members.csv",
      mimeType: "text/csv",
      buffer: Buffer.from("full_name,phone,date_of_birth\nDev Malhotra,9800011122,1994-02-30\nEsha Kapoor,9800011133,1996-07-14\n"),
    });
    await dialog.getByRole("button", { name: "Import" }).click();
    await expect(dialog.getByText(/Added\s*1 member/)).toBeVisible();
    await expect(dialog.getByText(/Row 2:/)).toBeVisible();
    await dialog.getByRole("button", { name: "Done" }).click();
    await page.getByRole("radio", { name: /^All/ }).click();
    await expect(page.getByRole("row", { name: /Esha Kapoor/ })).toBeVisible();
  });

  test("a plan ending this week shows on the renewals worklist", async ({ page }) => {
    await signIn(page, owner, "new-tulip-77");
    await nav(page, "Membership plans");
    await page.getByRole("button", { name: "Add membership plan" }).first().click();
    const plan = page.getByRole("dialog");
    await plan.getByLabel("Name").fill("Three days");
    await plan.getByLabel("Price (before tax)").fill("150");
    await plan.getByLabel("Runs for").fill("3");
    await plan.getByLabel("Unit").selectOption("DAYS");
    await plan.getByRole("button", { name: "Add" }).click();
    await expect(page.getByRole("cell", { name: "Three days", exact: true })).toBeVisible();

    await page.getByRole("combobox", { name: /^Search / }).fill("Aarav");
    await page.getByRole("option", { name: /Aarav Shah/ }).click();
    const panel = page.getByRole("dialog", { name: "Aarav Shah" });
    await panel.getByRole("button", { name: "Start a membership plan" }).click();
    const start = page.getByRole("dialog", { name: /Start a membership plan for Aarav Shah/ });
    await start.getByLabel("Membership plan").selectOption({ label: "Three days · 3 days · ₹150" });
    await start.getByRole("button", { name: /Start and take ₹150/ }).click();
    await expect(panel.getByRole("button", { name: "Renew" })).toBeVisible();
    await panel.getByRole("button", { name: "Close" }).click();

    await nav(page, "Renewals");
    await expect(page.getByRole("row", { name: /Aarav Shah.*Three days.*Ending soon/ })).toBeVisible();
    await page.getByRole("radio", { name: "All active" }).click();
    await expect(page.getByRole("row", { name: /Aarav Shah/ })).toBeVisible();
  });

  test("the catalogue edits prices, hides plans and counts stock", async ({ page }) => {
    await signIn(page, owner, "new-tulip-77");
    await nav(page, "Settings");
    await page.getByLabel("Products and services").check();
    await page.getByLabel("Stock tracking").check();
    await page.getByRole("button", { name: "Save settings" }).click();
    await expect(page.getByText("Settings saved")).toBeVisible();
    gymCode = (await page.getByText(/^[0-9]{4}[A-Z]{4}$/).first().textContent()) ?? "";

    await nav(page, "Membership plans & products");
    await page.getByRole("button", { name: "Actions for Trial week" }).click();
    await page.getByRole("menuitem", { name: "Edit" }).click();
    await page.getByRole("dialog").getByLabel("Price (before tax)").fill("350");
    await page.getByRole("dialog").getByRole("button", { name: "Save" }).click();
    await expect(page.getByRole("row", { name: /Trial week.*₹350/ })).toBeVisible();
    await page.getByRole("button", { name: "Actions for Trial week" }).click();
    await page.getByRole("menuitem", { name: "Deactivate" }).click();
    await expect(page.getByRole("row", { name: /Trial week/ })).toHaveCount(0);

    await page.getByRole("tab", { name: "Products" }).click();
    await page.getByRole("button", { name: "Add product" }).first().click();
    const product = page.getByRole("dialog");
    await product.getByLabel("Name").fill("Protein bar");
    await product.getByLabel("Price (before tax)").fill("80");
    await product.getByLabel("Count stock").check();
    await product.getByLabel("Starting stock").fill("10");
    await product.getByRole("button", { name: "Add" }).click();
    await expect(page.getByRole("row", { name: /Protein bar.*10.*₹80/ })).toBeVisible();
    await page.getByRole("button", { name: "Actions for Protein bar" }).click();
    await page.getByRole("menuitem", { name: "Adjust stock" }).click();
    await page.getByRole("dialog").getByLabel("Change").fill("-3");
    await page.getByRole("dialog").getByRole("button", { name: "Update stock" }).click();
    await expect(page.getByRole("row", { name: /Protein bar.*7.*₹80/ })).toBeVisible();
  });

  test("a second business brings the portfolio and the business switcher", async ({ page }) => {
    await signIn(page, owner, "new-tulip-77");
    await page.goto("/app/new");
    coachingId = await createBusiness(page, `Scholars ${runId}`, "Coaching centre");
    await expect(page.getByRole("navigation", { name: "Main" }).getByRole("link", { name: "Students", exact: true })).toBeVisible();

    await page.goto("/app");
    await expect(page.getByRole("heading", { name: "Your businesses" })).toBeVisible();
    await expect(page.getByRole("link", { name: new RegExp(`Core ${runId}`) }).first()).toBeVisible();
    await expect(page.getByRole("link", { name: new RegExp(`Scholars ${runId}`) }).first()).toBeVisible();

    await page.goto(`/app/${coachingId}`);
    await page.getByRole("button", { name: /Switch business/ }).click();
    await page.getByRole("menuitem", { name: new RegExp(`Core ${runId}`) }).click();
    await expect(page).toHaveURL(new RegExp(`/app/${gymId}$`));
    await expect(page.getByRole("navigation", { name: "Main" }).getByRole("link", { name: "Members", exact: true })).toBeVisible();
  });

  test("a coaching centre's batches take a roll call", async ({ page }) => {
    await signIn(page, owner, "new-tulip-77");
    await page.goto(`/app/${coachingId}`);
    await addMember(page, "Ishaan Verma", {}, "Student");
    await page.keyboard.press("Escape");
    await nav(page, "Batches & attendance");
    await page.getByRole("tab", { name: "Batches" }).click();
    await page.getByRole("button", { name: "New batch" }).click();
    await page.getByRole("dialog").getByLabel("Name").fill("Evening 6 pm");
    await page.getByRole("dialog").getByRole("button", { name: "Add" }).click();
    await page.getByRole("button", { name: /Evening 6 pm/ }).click();
    await page.getByPlaceholder("Add a student to Evening 6 pm").fill("Ishaan");
    await page.getByRole("option", { name: /Ishaan Verma/ }).getByRole("button").click();
    await expect(page.getByText("Ishaan Verma").last()).toBeVisible();
    await page.getByRole("button", { name: "Roll call" }).click();
    const roll = page.getByRole("dialog", { name: /Roll call/ });
    await roll.getByLabel("Ishaan Verma").check();
    await roll.getByRole("button", { name: "Save attendance" }).click();
    await expect(page.getByText("Marked 1 of 1 present")).toBeVisible();
    await page.getByRole("tab", { name: "Check-ins" }).click();
    await expect(page.getByRole("button", { name: /Ishaan Verma #\d+/ })).toBeVisible();
  });

  test("the owner makes a custom role with chosen permissions", async ({ page }) => {
    await signIn(page, owner, "new-tulip-77");
    await page.goto(`/app/${gymId}/team`);
    await page.getByRole("tab", { name: "Roles" }).click();
    await page.getByRole("button", { name: "New role" }).click();
    const dialog = page.getByRole("dialog", { name: "New role" });
    await dialog.getByLabel("Name").fill("Night shift");
    await dialog.getByLabel("See customers").check();
    await dialog.getByLabel("Mark attendance").check();
    await dialog.getByRole("button", { name: "Add role" }).click();
    await expect(page.getByText("Added the Night shift role")).toBeVisible();
    const api = await apiAs(page);
    const roles = (await api.get<{ name: string; permissions: string[] }[]>(`/businesses/${gymId}/roles`)).body;
    expect(roles.find((r) => r.name === "Night shift")?.permissions.sort()).toEqual(["attendance.mark", "customers.view"]);
  });

  test("customers can sign themselves up once the business allows it, and reset their password", async ({ page }) => {
    test.skip(!followsEmailedLinks, "Needs E2E_API_LOG to follow the reset link");
    await signIn(page, owner, "new-tulip-77");
    await page.goto(`/app/${gymId}/settings`);
    await page.getByLabel("Let customers sign themselves up").check();
    await page.getByRole("button", { name: "Save settings" }).click();
    await expect(page.getByText("Settings saved")).toBeVisible();

    const customer = await page.context().browser()?.newContext();
    if (!customer) throw new Error("No browser");
    const portal = await customer.newPage();
    const me = email("walkin");
    await portal.goto(`/portal/${gymCode}`);
    await portal.getByRole("button", { name: "Create your account" }).click();
    await portal.getByLabel("Your name").fill("Riya Sen");
    await portal.getByLabel("Email").fill(me);
    await portal.getByLabel("Password").fill("chai-break-12");
    await portal.getByRole("button", { name: "Create account" }).click();
    await expect(portal.getByRole("heading", { name: "Hello, Riya" })).toBeVisible();
    await expect(portal.getByText("No plans yet.")).toBeVisible();

    await portal.getByRole("button", { name: "Sign out" }).click();
    await portal.getByRole("button", { name: "Forgot password?" }).click();
    await portal.getByLabel("Your email").fill(me);
    await portal.getByRole("button", { name: "Send reset link" }).click();
    await expect(portal.getByText(/reset link is on its way/)).toBeVisible();
    await portal.goto(`/portal/${gymCode}/set-password?token=${lastEmailToken(me)}`);
    await portal.getByLabel("Password").fill("chai-break-34");
    await portal.getByRole("button", { name: "Save and sign in" }).click();
    await expect(portal.getByRole("heading", { name: "Hello, Riya" })).toBeVisible();
    await customer.close();

    // The owner sees the new customer, marked as signed up through the portal.
    await page.goto(`/app/${gymId}/customers`);
    await expect(page.getByRole("row", { name: /Riya Sen/ })).toBeVisible();
  });
});
