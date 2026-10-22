import { describe, expect, it } from "vitest";
import { canOpen, navigation } from "@/components/shell/nav";
import type { Schemas } from "@/lib/api/client";
import { toAccess } from "./access";

const gym: Schemas["MyBusinessResponse"] = {
  id: 7,
  code: "4821IRWD",
  name: "Ironwood Fitness",
  type: "FITNESS",
  labels: { customer: "Member", customers: "Members", group: "Batch", groups: "Batches", plan: "Membership plan", plans: "Membership plans" },
  modules: ["MEMBERSHIPS", "ATTENDANCE", "PAYROLL"],
  roleId: 3,
  roleName: "Front desk",
  owner: false,
  permissions: ["customers.view", "customers.manage", "billing.view", "billing.collect", "daybook.close", "attendance.mark", "memberships.manage"],
};

const labelsOf = (business: Schemas["MyBusinessResponse"]) => {
  const nav = navigation(toAccess(business));
  return [...nav.main, ...nav.manage, ...(nav.settings ? [nav.settings] : [])].map((item) => item.label);
};

describe("business access", () => {
  it("answers permission and module questions from the role", () => {
    const access = toAccess(gym);
    expect(access.can("billing.collect")).toBe(true);
    expect(access.can("billing.void")).toBe(false);
    expect(access.has("MEMBERSHIPS")).toBe(true);
    expect(access.has("CATALOG")).toBe(false);
  });

  it("shows the front desk only what their role can use, in the business's own words", () => {
    expect(labelsOf(gym)).toEqual(["Today", "Members", "Renewals", "Billing", "Day book", "Tasks", "Requests", "Attendance", "Membership plans"]);
  });

  it("hides money and team screens from someone who only sees customers", () => {
    const labels = labelsOf({ ...gym, permissions: ["customers.view"] });
    expect(labels).not.toContain("Billing");
    expect(labels).not.toContain("Team");
    expect(labels).not.toContain("Payroll");
    expect(labels).not.toContain("Settings");
    expect(labels).toEqual(expect.arrayContaining(["Today", "Members", "Tasks", "Requests"]));
  });

  it("gives the owner every screen the business has turned on", () => {
    const allKeys = [
      "customers.view", "customers.manage", "billing.view", "billing.collect", "expenses.view", "daybook.close", "offerings.manage",
      "team.view", "payroll.manage", "reports.view", "activity.view", "settings.manage",
    ];
    const labels = labelsOf({ ...gym, owner: true, roleName: "Owner", permissions: allKeys });
    expect(labels).toEqual(expect.arrayContaining(["Expenses", "Team", "Payroll", "Reports", "Activity log", "Settings"]));
  });

  it("drops screens for modules that are off", () => {
    const shop = { ...gym, type: "RETAIL" as const, modules: ["CATALOG" as const], labels: { ...gym.labels, customers: "Customers", plans: "Plans" } };
    const labels = labelsOf({ ...shop, permissions: [...gym.permissions, "offerings.manage"] });
    expect(labels).not.toContain("Renewals");
    expect(labels).not.toContain("Attendance");
    expect(labels).toContain("Plans & products");
  });

  it("blocks direct links to screens the role can't use, and allows the rest", () => {
    const desk = toAccess(gym);
    expect(canOpen(desk, "/app/7")).toBe(true);
    expect(canOpen(desk, "/app/7/customers")).toBe(true);
    expect(canOpen(desk, "/app/7/billing/invoices/12")).toBe(true);
    expect(canOpen(desk, "/app/7/team")).toBe(false);
    expect(canOpen(desk, "/app/7/settings")).toBe(false);
    expect(canOpen(desk, "/app/7/expenses")).toBe(false);
    expect(canOpen(toAccess({ ...gym, modules: [] }), "/app/7/renewals")).toBe(false);
  });
});
