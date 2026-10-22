import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";
import { mkdirSync, writeFileSync } from "node:fs";
import { addMember, addPlan, createBusiness, email, followsEmailedLinks, runId, signInAsAdmin, signUp } from "./support";

/**
 * Visits every screen with real data on desktop, tablet and phone sizes and checks, for each one: the page renders its
 * heading, the browser console has no errors, no API call fails, there is no sideways scrolling on a phone, and
 * axe finds no serious or critical accessibility problems. Writes e2e-results/screen-sweep.json as evidence.
 */
type Result = { screen: string; viewport: string; consoleErrors: string[]; failedRequests: string[]; overflowX: boolean; axe: string[] };

const SCREENS = (b: number) => [
  ["Today", `/app/${b}`],
  ["Members", `/app/${b}/customers`],
  ["Renewals", `/app/${b}/renewals`],
  ["Billing", `/app/${b}/billing`],
  ["Payments", `/app/${b}/billing?tab=payments`],
  ["Expenses", `/app/${b}/expenses`],
  ["Day book", `/app/${b}/day-book`],
  ["Tasks", `/app/${b}/tasks`],
  ["Requests", `/app/${b}/requests`],
  ["Attendance", `/app/${b}/attendance`],
  ["Membership plans", `/app/${b}/catalog`],
  ["Team", `/app/${b}/team`],
  ["Payroll", `/app/${b}/payroll`],
  ["Reports", `/app/${b}/reports`],
  ["Activity log", `/app/${b}/activity`],
  ["Settings", `/app/${b}/settings`],
  ["Your account", "/app/account"],
  ["Add a business", "/app/new"],
] as const;

const CONSOLE = [
  ["Platform overview", "/admin"],
  ["Console businesses", "/admin/businesses"],
  ["Console people", "/admin/people"],
  ["System health", "/admin/health"],
] as const;

const PUBLIC = [
  ["Landing", "/"],
  ["Sign in", "/sign-in"],
  ["Customer sign-in", "/sign-in?as=member"],
  ["Sign up", "/sign-up"],
  ["Forgot password", "/forgot-password"],
  ["Portal code", "/portal"],
  ["Privacy Policy", "/privacy"],
  ["Terms of Service", "/terms"],
  ["Security", "/security"],
  ["Contact", "/contact"],
] as const;

const VIEWPORTS = [
  ["desktop", { width: 1440, height: 900 }],
  ["tablet", { width: 768, height: 1024 }],
  ["phone", { width: 390, height: 844 }],
] as const;

async function check(page: Page, screen: string, path: string, viewport: string): Promise<Result> {
  const consoleErrors: string[] = [];
  const failedRequests: string[] = [];
  const onConsole = (m: { type(): string; text(): string; location(): { url: string } }) => {
    if (m.type() !== "error") return;
    // Google's sign-in library sometimes requests its stylesheet with a script call that Google answers without a
    // CORS header. That is Google's own console noise, outside this app; the button renders either way.
    const fromGoogleSignIn = m.text().includes("accounts.google.com/gsi/") || m.location().url.startsWith("https://accounts.google.com/gsi/");
    if (!fromGoogleSignIn) consoleErrors.push(m.text().slice(0, 300));
  };
  const onPageError = (e: Error) => consoleErrors.push(`pageerror: ${e.message}`);
  const onResponse = (r: { url(): string; status(): number }) => {
    if (r.url().includes("/api/") && r.status() >= 400) failedRequests.push(`${r.status()} ${new URL(r.url()).pathname}`);
  };
  page.on("console", onConsole);
  page.on("pageerror", onPageError);
  page.on("response", onResponse);
  await page.goto(path);
  await expect(page.getByRole("heading", { level: 1 }).first()).toBeVisible();
  await page.waitForLoadState("networkidle");
  const overflowX = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 1);
  const axe = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21aa"]).analyze();
  page.off("console", onConsole);
  page.off("pageerror", onPageError);
  page.off("response", onResponse);
  return {
    screen,
    viewport,
    consoleErrors,
    failedRequests,
    overflowX,
    axe: axe.violations
      .filter((v) => v.impact === "serious" || v.impact === "critical")
      .map((v) => `${v.id}: ${v.nodes.length} × ${v.nodes[0]?.target.join(" ")}`),
  };
}

test("every screen renders cleanly, accessibly and fits a phone", async ({ page }) => {
  test.setTimeout(900_000);
  const results: Result[] = [];

  for (const [screen, path] of PUBLIC) {
    for (const [viewport, size] of VIEWPORTS) {
      await page.setViewportSize(size);
      results.push(await check(page, screen, path, viewport));
    }
  }

  await page.setViewportSize({ width: 1440, height: 900 });
  await signUp(page, "Sweep Owner", email("sweep"));
  const businessId = await createBusiness(page, `Sweep ${runId}`, "Gym or fitness studio");
  await addPlan(page, "Monthly", "1500");
  await addMember(page, "Asha Verma", { phone: "9810012345" });

  for (const [screen, path] of SCREENS(businessId)) {
    for (const [viewport, size] of VIEWPORTS) {
      await page.setViewportSize(size);
      results.push(await check(page, screen, path, viewport));
    }
  }

  if (followsEmailedLinks) {
    await page.setViewportSize({ width: 1440, height: 900 });
    await signInAsAdmin(page);
    for (const [screen, path] of CONSOLE) {
      for (const [viewport, size] of VIEWPORTS) {
        await page.setViewportSize(size);
        results.push(await check(page, screen, path, viewport));
      }
    }
  }

  mkdirSync("e2e-results", { recursive: true });
  writeFileSync("e2e-results/screen-sweep.json", JSON.stringify(results, null, 2));
  const problems = results.filter((r) => r.consoleErrors.length || r.failedRequests.length || r.overflowX || r.axe.length);
  expect(problems, JSON.stringify(problems, null, 2)).toEqual([]);
});
