import type { Schemas } from "@/lib/api/client";

/** The permission catalogue (docs/PRODUCT.md, Permissions). Mirrors com.managio.platform.tenancy.Permission. */
export type PermissionKey =
  | "customers.view"
  | "customers.manage"
  | "customers.archive"
  | "customers.import"
  | "attendance.mark"
  | "offerings.manage"
  | "memberships.manage"
  | "billing.view"
  | "billing.collect"
  | "billing.discount"
  | "billing.void"
  | "expenses.view"
  | "expenses.record"
  | "daybook.close"
  | "team.view"
  | "team.manage"
  | "payroll.manage"
  | "tasks.manage"
  | "requests.approve"
  | "reports.view"
  | "activity.view"
  | "settings.manage";

export type ModuleKey = Schemas["MyBusinessResponse"]["modules"][number];
export type BusinessType = Schemas["MyBusinessResponse"]["type"];

/** What the signed-in person may do in one business. The server enforces the same rules; this only shapes the UI. */
export type BusinessAccess = Schemas["MyBusinessResponse"] & {
  can: (permission: PermissionKey) => boolean;
  has: (module: ModuleKey) => boolean;
};

export function toAccess(business: Schemas["MyBusinessResponse"]): BusinessAccess {
  const permissions = new Set(business.permissions);
  const modules = new Set(business.modules);
  return {
    ...business,
    can: (permission) => permissions.has(permission),
    has: (module) => modules.has(module),
  };
}

export const BUSINESS_TYPE_LABELS: Record<BusinessType, string> = {
  FITNESS: "Gym or fitness studio",
  SALON: "Salon or spa",
  COACHING: "Coaching centre",
  SCHOOL: "School or academy",
  CLINIC: "Clinic",
  RETAIL: "Shop",
  OTHER: "Other business",
};

export const MODULE_LABELS: Record<ModuleKey, { name: string; description: string }> = {
  MEMBERSHIPS: { name: "Memberships and renewals", description: "Plans that run for a period, with renewal reminders." },
  CATALOG: { name: "Products and services", description: "Sell items and services at the counter." },
  INVENTORY: { name: "Stock tracking", description: "Count stock for products. Needs products and services." },
  GROUPS: { name: "Groups and batches", description: "Put customers into batches or classes." },
  ATTENDANCE: { name: "Attendance", description: "Check customers in and see who came." },
  PAYROLL: { name: "Payroll", description: "Prepare and pay monthly salaries." },
};
