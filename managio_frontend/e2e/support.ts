import { expect, type Page } from "@playwright/test";
import { readFileSync } from "node:fs";
import { apiAs } from "./api";

/** A unique suffix, so every run creates its own people and businesses in the shared dev database. */
export const runId = `${Date.now().toString(36)}${Math.floor(Math.random() * 1000)}`;

export const email = (name: string) => `${name}.${runId}@example.com`;

export const PASSWORD = "tulip-garden-9";

/** The latest link token emailed to `to`, read from the API's log (the dev API logs emails instead of sending them). */
export function lastEmailToken(to: string): string {
  const logFile = process.env.E2E_API_LOG;
  if (!logFile) throw new Error("Set E2E_API_LOG to the API's log file to follow emailed links");
  const log = readFileSync(logFile, "utf8");
  const start = log.lastIndexOf(`Email (not sent) to=${to}`);
  if (start < 0) throw new Error(`No email to ${to} in the API log`);
  const match = /token=([A-Za-z0-9_-]+)/.exec(log.slice(start));
  if (!match) throw new Error(`The email to ${to} has no link`);
  return match[1];
}

export const followsEmailedLinks = Boolean(process.env.E2E_API_LOG);

export async function signUp(page: Page, name: string, address: string) {
  await page.goto("/sign-up");
  await page.getByLabel("Your name").fill(name);
  await page.getByLabel("Email").fill(address);
  await page.getByLabel("Password").fill(PASSWORD);
  await page.getByRole("button", { name: "Create account" }).click();
  await expect(page.getByRole("heading", { name: "Set up your first business" })).toBeVisible();
}

export async function createBusiness(page: Page, name: string, kind: string) {
  await page.getByRole("radio", { name: kind }).click();
  await page.getByLabel("Business name").fill(name);
  await page.getByRole("button", { name: "Create business" }).click();
  await expect(page.getByRole("heading", { name: /^Good (morning|afternoon|evening)/ })).toBeVisible();
  return Number(new URL(page.url()).pathname.split("/")[2]);
}

/** Signs in as staff. Unless `expectFailure`, waits until the app has taken over from the sign-in page. */
export async function signIn(page: Page, address: string, password = PASSWORD, { expectFailure = false } = {}) {
  await page.goto("/sign-in");
  await page.getByLabel("Email").fill(address);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  if (!expectFailure) {
    await page.waitForURL((url) => url.pathname === "/app" || url.pathname.startsWith("/app/"));
  }
}

/** The platform administrator for the console tests. The API's PLATFORM_ADMIN_EMAILS must list it (D-028). */
export const ADMIN_EMAIL = process.env.E2E_ADMIN_EMAIL ?? "platform.admin@example.com";

/** Signs in as the platform administrator, creating and verifying the account the first time. */
export async function signInAsAdmin(page: Page) {
  const created = await page.request.post("/api/v1/auth/sign-up", {
    data: { fullName: "Platform Admin", email: ADMIN_EMAIL, password: PASSWORD },
  });
  expect([201, 409], "create the administrator account or find it already there").toContain(created.status());
  await signIn(page, ADMIN_EMAIL);
  const api = await apiAs(page);
  if ((await api.get("/admin/access")).status === 403) {
    await api.post("/me/verification-email");
    await page.request.post("/api/v1/auth/verify-email", { data: { token: lastEmailToken(ADMIN_EMAIL) } });
  }
  expect((await api.get("/admin/access")).status, `the API's PLATFORM_ADMIN_EMAILS must include ${ADMIN_EMAIL}`).toBe(204);
}

/** Opens a customer's panel through the always-visible search. */
export async function openCustomer(page: Page, name: string) {
  const search = page.getByRole("combobox", { name: /^Search / });
  await search.click();
  await search.fill(name);
  await page.getByRole("option", { name: new RegExp(name) }).click();
  return page.getByRole("dialog", { name });
}

export async function nav(page: Page, label: string) {
  await page.getByRole("navigation", { name: "Main" }).getByRole("link", { name: label, exact: true }).click();
}

/** Adds a membership plan from the catalogue page. */
export async function addPlan(page: Page, name: string, price: string, months = "1") {
  await nav(page, "Membership plans");
  await page.getByRole("button", { name: /^Add membership plan$/ }).first().click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("Name").fill(name);
  await dialog.getByLabel("Price (before tax)").fill(price);
  await dialog.getByLabel("Runs for").fill(months);
  await dialog.getByRole("button", { name: "Add" }).click();
  await expect(page.getByRole("cell", { name, exact: true })).toBeVisible();
}

/** Adds a customer from the "New" menu. `noun` is the business type's word for them (Member, Client, Student...). */
export async function addMember(page: Page, name: string, extra: { phone?: string; email?: string } = {}, noun = "Member") {
  await page.getByRole("button", { name: "New" }).click();
  await page.getByRole("menuitem", { name: noun, exact: true }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("Full name").fill(name);
  if (extra.phone) await dialog.getByLabel(/^Phone/).fill(extra.phone);
  if (extra.email) await dialog.getByLabel(/^Email/).fill(extra.email);
  await dialog.getByRole("button", { name: `Add ${noun.toLowerCase()}` }).click();
  // Adding opens the new member's panel.
  await expect(page.getByRole("dialog", { name: name })).toBeVisible();
}
