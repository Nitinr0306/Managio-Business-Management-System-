import { expect, test } from "@playwright/test";
import { createBusiness, email, nav, runId, signUp } from "./support";

test("the landing page leads to sign-up and sign-in", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { level: 1, name: "Run every business you own from one screen." })).toBeVisible();
  await page.getByRole("link", { name: "Create your account" }).first().click();
  await expect(page).toHaveURL(/\/sign-up$/);
  await page.goto("/");
  await page.getByRole("navigation", { name: "Main" }).getByRole("link", { name: "Sign in" }).click();
  await expect(page.getByRole("heading", { name: "Who's signing in?" })).toBeVisible();
});

test("the sign-in tiles switch between staff and customer sign-in", async ({ page }) => {
  await page.goto("/sign-in");
  await expect(page.getByLabel("Email")).toBeVisible();
  await page.getByRole("radio", { name: /I'm a customer/ }).click();
  await expect(page.getByLabel("Business code")).toBeVisible();
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page.getByText("Enter the business code")).toBeVisible();
});

test("an unknown portal code says so", async ({ page }) => {
  await page.goto("/portal/0000NONE");
  await expect(page.getByRole("heading", { name: "We couldn't find that business" })).toBeVisible();
});

test("tasks repeat once they are done", async ({ page }) => {
  await signUp(page, "Kiran Das", email("tasks"));
  await createBusiness(page, `Bright Minds ${runId}`, "Coaching centre");
  await nav(page, "Tasks");
  await page.getByRole("button", { name: "New task" }).click();
  const dialog = page.getByRole("dialog", { name: "New task" });
  await dialog.getByLabel("What needs doing").fill("Open the centre");
  await dialog.getByLabel("Repeats").selectOption("DAILY");
  await dialog.getByRole("button", { name: "Add task" }).click();
  await page.getByRole("button", { name: "Mark “Open the centre” as done" }).click();
  await expect(page.getByText(/The next “Open the centre” is on the list/)).toBeVisible();
  await expect(page.getByRole("button", { name: "Mark “Open the centre” as done" })).toHaveCount(1);
  // A coaching centre calls its plans courses.
  await expect(page.getByRole("navigation", { name: "Main" }).getByRole("link", { name: "Students", exact: true })).toBeVisible();
});

test("@mobile the menu opens on a phone", async ({ page }, info) => {
  test.skip(info.project.name !== "mobile", "Phone layout only");
  await signUp(page, "Zoya Khan", email("mobile"));
  await createBusiness(page, `Zoya Fitness ${runId}`, "Gym or fitness studio");
  await expect(page.getByRole("navigation", { name: "Main" })).toBeHidden();
  await page.getByRole("button", { name: "Open menu" }).click();
  await page.getByRole("navigation", { name: "Main" }).getByRole("link", { name: "Membership plans" }).click();
  await expect(page.getByRole("heading", { level: 1, name: "Membership plans" })).toBeVisible();
  await expect(page.getByRole("navigation", { name: "Main" })).toBeHidden();
});
