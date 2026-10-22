import { expect, test } from "@playwright/test";
import { addMember, addPlan, createBusiness, email, followsEmailedLinks, lastEmailToken, nav, PASSWORD, runId, signIn, signUp } from "./support";

/**
 * The team and the customer: an owner invites a front-desk person, who sees only what their role allows and
 * asks for an expense to be paid back; the owner approves it. Then a member is invited to the portal and sees
 * their own plan and dues. Following invitation links needs E2E_API_LOG (see support.ts).
 */
test.describe.serial("team, approvals and the portal", () => {
  test.skip(!followsEmailedLinks, "Needs E2E_API_LOG to follow emailed links");

  const owner = email("owner2");
  const staff = email("desk");
  const member = email("asha");
  const studio = `Glow ${runId}`;
  let businessCode = "";

  test("the owner confirms their email, sets up and invites the front desk", async ({ page }) => {
    await signUp(page, "Meera Iyer", owner);
    await page.goto(`/verify-email?token=${lastEmailToken(owner)}`);
    await expect(page.getByRole("heading", { name: "Email confirmed" })).toBeVisible();
    await page.goto("/app/new");
    await createBusiness(page, studio, "Gym or fitness studio");

    await nav(page, "Team");
    await page.getByRole("button", { name: "Invite someone" }).click();
    const dialog = page.getByRole("dialog");
    await dialog.getByLabel("Their email").fill(staff);
    await dialog.getByLabel("Role").selectOption({ label: "Front desk" });
    await dialog.getByRole("button", { name: "Send invitation" }).click();
    await expect(page.getByText(`Invitation sent to ${staff}`)).toBeVisible();

    await nav(page, "Settings");
    businessCode = (await page.getByText(/^[0-9]{4}[A-Z]{4}$/).first().textContent()) ?? "";
    expect(businessCode).toMatch(/^[0-9]{4}[A-Z]{4}$/);
  });

  test("the invitee joins and sees only the front desk's screens", async ({ page }) => {
    await page.goto(`/invite?token=${lastEmailToken(staff)}`);
    await expect(page.getByRole("heading", { name: `Join ${studio}` })).toBeVisible();
    await page.getByLabel("Your name").fill("Divya Nair");
    await page.getByLabel("Choose a password").fill(PASSWORD);
    await page.getByRole("button", { name: `Join ${studio}` }).click();
    await expect(page.getByRole("heading", { name: /^Good (morning|afternoon|evening), Divya/ })).toBeVisible();

    const menu = page.getByRole("navigation", { name: "Main" });
    await expect(menu.getByRole("link", { name: "Members", exact: true })).toBeVisible();
    await expect(menu.getByRole("link", { name: "Billing" })).toBeVisible();
    await expect(menu.getByRole("link", { name: "Expenses" })).toHaveCount(0);
    await expect(menu.getByRole("link", { name: "Team" })).toHaveCount(0);
    await expect(menu.getByRole("link", { name: "Settings" })).toHaveCount(0);
  });

  test("the front desk asks to be paid back for supplies", async ({ page }) => {
    await signIn(page, staff);
    await nav(page, "Requests");
    await page.getByRole("button", { name: "New request" }).click();
    const dialog = page.getByRole("dialog", { name: "New request" });
    await dialog.getByLabel("Title").fill("Cleaning supplies");
    await dialog.getByLabel("Amount").fill("640");
    await dialog.getByLabel("Category").selectOption({ label: "Supplies" });
    await dialog.getByRole("button", { name: "Send request" }).click();
    await expect(page.getByRole("row", { name: /Cleaning supplies.*Expense claim.*Waiting/ })).toBeVisible();
  });

  test("the owner approves it from the notification and it becomes an expense", async ({ page }) => {
    await signIn(page, owner);
    await page.getByRole("button", { name: /Notifications, 1 unread/ }).click();
    await page.getByRole("button", { name: /Divya Nair asked for approval/ }).click();
    const dialog = page.getByRole("dialog", { name: "Request" });
    await expect(dialog.getByText("Cleaning supplies")).toBeVisible();
    await dialog.getByRole("button", { name: "Approve" }).click();
    await dialog.getByLabel("How was it paid?").selectOption("UPI");
    await dialog.getByRole("button", { name: "Approve and record expense" }).click();
    await expect(page.getByText("Approved")).toBeVisible();

    await nav(page, "Expenses");
    await expect(page.getByRole("row", { name: /Supplies.*Approved request.*Divya Nair.*₹640/ })).toBeVisible();
  });

  test("a task assigned to the front desk is theirs to finish, and the owner sees it done", async ({ page }) => {
    await signIn(page, owner);
    await nav(page, "Tasks");
    await page.getByRole("button", { name: "New task" }).click();
    const dialog = page.getByRole("dialog", { name: "New task" });
    await dialog.getByLabel("What needs doing").fill("Call last week's lapsed members");
    await dialog.getByLabel("Assign to").selectOption({ label: "Divya Nair" });
    await dialog.getByLabel("Priority").selectOption("HIGH");
    await dialog.getByRole("button", { name: "Add task" }).click();
    await expect(page.getByText("Call last week's lapsed members")).toBeVisible();

    const desk = await page.context().browser()?.newContext();
    if (!desk) throw new Error("No browser");
    const deskPage = await desk.newPage();
    await signIn(deskPage, staff);
    await nav(deskPage, "Tasks");
    await expect(deskPage.getByText("Call last week's lapsed members").filter({ visible: true })).toBeVisible();
    await deskPage.getByRole("button", { name: "Mark “Call last week's lapsed members” as done" }).click();
    await expect(deskPage.getByText("Done", { exact: true })).toBeVisible();
    await desk.close();

    await page.reload();
    await page.getByRole("radio", { name: "Done" }).click();
    await expect(page.getByText("Call last week's lapsed members")).toBeVisible();
  });

  test("a leave request is rejected with a reason the requester can read", async ({ page }) => {
    const desk = await page.context().browser()?.newContext();
    if (!desk) throw new Error("No browser");
    const deskPage = await desk.newPage();
    await signIn(deskPage, staff);
    await nav(deskPage, "Requests");
    await deskPage.getByRole("button", { name: "New request" }).click();
    const form = deskPage.getByRole("dialog", { name: "New request" });
    await form.getByLabel("What are you asking for?").selectOption("LEAVE");
    await form.getByLabel("Title").fill("Sister's wedding");
    await form.getByRole("button", { name: "Send request" }).click();
    await expect(deskPage.getByRole("row", { name: /Sister's wedding.*Leave.*Waiting/ })).toBeVisible();

    await signIn(page, owner);
    await nav(page, "Requests");
    await page.getByRole("row", { name: /Sister's wedding/ }).click();
    const detail = page.getByRole("dialog", { name: "Request" });
    await detail.getByRole("button", { name: "Reject" }).click();
    await detail.getByLabel("Why not?").fill("Month-end stock count that week");
    await detail.getByRole("button", { name: "Reject" }).click();
    await expect(page.getByText("Rejected", { exact: true }).first()).toBeVisible();

    await deskPage.reload();
    await deskPage.getByRole("button", { name: /Notifications, \d+ unread/ }).click();
    await expect(deskPage.getByText("Your request was rejected")).toBeVisible();
    await expect(deskPage.getByText(/Month-end stock count that week/)).toBeVisible();
    await desk.close();
  });

  test("a member is invited to the portal and sees their own plan and dues", async ({ page, browser }) => {
    await signIn(page, owner);
    await addPlan(page, "Quarterly", "4200", "3");
    await addMember(page, "Asha Verma", { email: member });
    const panel = page.getByRole("dialog", { name: "Asha Verma" });
    await panel.getByRole("button", { name: "Start a membership plan" }).click();
    const start = page.getByRole("dialog", { name: /Start a membership plan for Asha Verma/ });
    await start.getByLabel("Take payment now").uncheck();
    await start.getByRole("button", { name: "Start" }).click();
    await expect(panel.getByRole("button", { name: /Record payment · ₹4,200 due/ })).toBeVisible();
    await panel.getByRole("button", { name: "More actions" }).click();
    await page.getByRole("menuitem", { name: "Invite to the portal" }).click();
    await expect(page.getByText(`Invitation sent to ${member}`)).toBeVisible();

    const customer = await browser.newContext();
    const portal = await customer.newPage();
    await portal.goto(`/portal/${businessCode}/set-password?token=${lastEmailToken(member)}`);
    await portal.getByLabel("Password").fill("mango-lassi-5");
    await portal.getByRole("button", { name: "Save and sign in" }).click();
    await expect(portal.getByRole("heading", { name: "Hello, Asha" })).toBeVisible();
    await expect(portal.getByText("₹4,200").first()).toBeVisible();
    await expect(portal.getByText("Quarterly")).toBeVisible();
    await customer.close();
  });

  test("the member signs in again with the business code", async ({ page }) => {
    await page.goto("/sign-in?as=member");
    await page.getByLabel("Business code").fill(businessCode.toLowerCase());
    await page.getByLabel("Email or phone").fill(member.toUpperCase());
    await page.getByLabel("Password").fill("mango-lassi-5");
    await page.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(page.getByRole("heading", { name: "Hello, Asha" })).toBeVisible();
    await expect(page).toHaveURL(new RegExp(`/portal/${businessCode}$`));
  });
});
