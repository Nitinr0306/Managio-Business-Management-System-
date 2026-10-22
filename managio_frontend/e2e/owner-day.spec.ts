import { expect, test } from "@playwright/test";
import { addMember, addPlan, createBusiness, email, nav, PASSWORD, runId, signIn, signUp } from "./support";

/**
 * An owner's first day, end to end: sign up, set up a gym, add a plan and a member, start the plan with a
 * payment, find the member again, take the rest of a part payment, record an expense and close the day.
 */
test.describe.serial("owner's first day", () => {
  const owner = email("owner");
  const gym = `Ironwood ${runId}`;

  test("signs up and sets up a gym", async ({ page }) => {
    await signUp(page, "Ananya Rao", owner);
    await createBusiness(page, gym, "Gym or fitness studio");
    await expect(page.getByRole("navigation", { name: "Main" }).getByRole("link", { name: "Members", exact: true })).toBeVisible();
    await expect(page.getByText("Renewal runway")).toBeVisible();
  });

  test("adds a plan and a member, and starts the plan with a part payment", async ({ page }) => {
    await signIn(page, owner);
    await expect(page.getByText("Renewal runway")).toBeVisible();
    await addPlan(page, "Monthly", "1500");
    await addMember(page, "Rohan Mehta", { phone: "9876543210" });

    const panel = page.getByRole("dialog", { name: "Rohan Mehta" });
    await panel.getByRole("button", { name: "Start a membership plan" }).click();
    const start = page.getByRole("dialog", { name: /Start a membership plan for Rohan Mehta/ });
    await start.getByLabel("Membership plan").selectOption({ label: "Monthly · 1 month · ₹1,500" });
    await start.getByLabel("Amount").fill("1000");
    await start.getByRole("button", { name: /Start and take/ }).click();
    await expect(page.getByText("Started Monthly for Rohan Mehta")).toBeVisible();

    // The panel now leads with the money still owed.
    await expect(panel.getByRole("button", { name: "Record payment · ₹500 due" })).toBeVisible();
    await expect(panel.getByText("Active").first()).toBeVisible();
  });

  test("finds the member from anywhere and takes the rest", async ({ page }) => {
    await signIn(page, owner);
    await expect(page.getByText("Renewal runway")).toBeVisible();
    await page.keyboard.press("/");
    await page.keyboard.type("43210");
    await expect(page.getByRole("option", { name: /Rohan Mehta/ })).toBeVisible();
    await page.keyboard.press("Enter");

    const panel = page.getByRole("dialog", { name: "Rohan Mehta" });
    await panel.getByRole("button", { name: /Record payment/ }).click();
    const pay = page.getByRole("dialog", { name: "Record payment from Rohan Mehta" });
    await pay.getByLabel("Method").selectOption("UPI");
    await pay.getByRole("button", { name: "Record payment" }).click();
    await expect(page.getByText("Recorded ₹500 from Rohan Mehta")).toBeVisible();
    await expect(panel.getByRole("button", { name: /^Renew/ })).toBeVisible();
  });

  test("sees the money in billing and today's collections", async ({ page }) => {
    await signIn(page, owner);
    await nav(page, "Billing");
    await page.getByRole("radio", { name: "Paid" }).click();
    await expect(page.getByRole("row", { name: /Rohan Mehta.*₹1,500.*Paid/ })).toBeVisible();
    await page.getByRole("tab", { name: "Payments" }).click();
    await expect(page.getByRole("row", { name: /Rohan Mehta.*Cash.*₹1,000/ })).toBeVisible();
    await expect(page.getByRole("row", { name: /Rohan Mehta.*UPI.*₹500/ })).toBeVisible();
  });

  test("records an expense and closes the day with the counted cash", async ({ page }) => {
    await signIn(page, owner);
    await nav(page, "Expenses");
    await page.getByRole("button", { name: "Record expense" }).first().click();
    const dialog = page.getByRole("dialog", { name: "Record an expense" });
    await dialog.getByLabel("Category").selectOption({ label: "Supplies" });
    await dialog.getByLabel("Amount").fill("150");
    await dialog.getByLabel("Paid to").fill("Cleaning mart");
    await dialog.getByRole("button", { name: "Record expense" }).click();
    await expect(page.getByRole("row", { name: /Supplies.*Cleaning mart.*₹150/ })).toBeVisible();

    await nav(page, "Day book");
    // ₹1,000 cash in, ₹150 cash out.
    await expect(page.getByText("₹850").first()).toBeVisible();
    await page.getByLabel("Cash you counted").fill("840");
    await expect(page.getByText("Short ₹10")).toBeVisible();
    await page.getByRole("button", { name: "Close the day" }).click();
    await page.getByRole("button", { name: "Yes, close the day" }).click();
    await expect(page.getByText(/Cash for this day is locked/)).toBeVisible();
  });

  test("can't take cash on a closed day", async ({ page }) => {
    await signIn(page, owner);
    await nav(page, "Expenses");
    await page.getByRole("button", { name: "Record expense" }).first().click();
    const dialog = page.getByRole("dialog", { name: "Record an expense" });
    await dialog.getByLabel("Category").selectOption({ label: "Supplies" });
    await dialog.getByLabel("Amount").fill("20");
    await dialog.getByRole("button", { name: "Record expense" }).click();
    await expect(dialog.getByRole("alert")).toContainText(/closed/i);
  });

  test("signs out and is sent back to sign in for app pages", async ({ page }) => {
    await signIn(page, owner);
    await expect(page.getByText("Renewal runway")).toBeVisible();
    const appUrl = page.url();
    await page.getByRole("button", { name: /Ananya Rao/ }).click();
    await page.getByRole("menuitem", { name: "Sign out" }).click();
    await expect(page).toHaveURL(/\/sign-in/);
    await page.goto(appUrl);
    await expect(page).toHaveURL(/\/sign-in\?next=/);
    await page.getByLabel("Email").fill(owner);
    await page.getByLabel("Password").fill(PASSWORD);
    await page.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(page).toHaveURL(appUrl);
  });
});

test("wrong passwords get one plain message", async ({ page }) => {
  await signIn(page, email("nobody"), "not-the-password", { expectFailure: true });
  await expect(page.getByRole("alert").filter({ hasText: "match" })).toHaveText("That email and password don't match. Try again.");
});
