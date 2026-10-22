import {
  Banknote,
  BookOpen,
  CalendarCheck,
  ChartColumn,
  IdCard,
  Inbox,
  LayoutDashboard,
  ListChecks,
  ReceiptIndianRupee,
  RefreshCcw,
  ScrollText,
  Settings,
  Tags,
  Users,
  Wallet,
  type LucideIcon,
} from "lucide-react";
import type { BusinessAccess } from "@/features/business/access";

export type NavItem = { href: string; label: string; icon: LucideIcon; exact?: boolean };

type Screen = {
  /** The first path segment after /app/[businessId]; "" is Today. */
  segment: string;
  section: "main" | "manage" | "settings";
  icon: LucideIcon;
  label: (b: BusinessAccess) => string;
  /** Who may open it. The API enforces the same rule; this keeps the UI honest about it. */
  allowed: (b: BusinessAccess) => boolean;
};

/** Every screen of the business app, in sidebar order: one definition for the menu and for direct links. */
const SCREENS: Screen[] = [
  { segment: "", section: "main", icon: LayoutDashboard, label: () => "Today", allowed: () => true },
  { segment: "customers", section: "main", icon: Users, label: (b) => b.labels.customers, allowed: (b) => b.can("customers.view") },
  { segment: "renewals", section: "main", icon: RefreshCcw, label: () => "Renewals", allowed: (b) => b.can("customers.view") && b.has("MEMBERSHIPS") },
  { segment: "billing", section: "main", icon: ReceiptIndianRupee, label: () => "Billing", allowed: (b) => b.can("billing.view") },
  { segment: "expenses", section: "main", icon: Wallet, label: () => "Expenses", allowed: (b) => b.can("expenses.view") },
  { segment: "day-book", section: "main", icon: BookOpen, label: () => "Day book", allowed: (b) => b.can("daybook.close") },
  { segment: "tasks", section: "main", icon: ListChecks, label: () => "Tasks", allowed: () => true },
  { segment: "requests", section: "main", icon: Inbox, label: () => "Requests", allowed: () => true },
  {
    segment: "attendance",
    section: "main",
    icon: CalendarCheck,
    label: (b) => (b.has("GROUPS") ? `${b.labels.groups} & attendance` : "Attendance"),
    allowed: (b) => (b.has("GROUPS") || b.has("ATTENDANCE")) && b.can("customers.view"),
  },
  {
    segment: "catalog",
    section: "manage",
    icon: Tags,
    label: (b) => (b.has("CATALOG") ? `${b.labels.plans} & products` : b.labels.plans),
    allowed: (b) => b.can("offerings.manage") || b.can("billing.collect"),
  },
  { segment: "team", section: "manage", icon: IdCard, label: () => "Team", allowed: (b) => b.can("team.view") },
  { segment: "payroll", section: "manage", icon: Banknote, label: () => "Payroll", allowed: (b) => b.has("PAYROLL") && b.can("payroll.manage") },
  { segment: "reports", section: "manage", icon: ChartColumn, label: () => "Reports", allowed: (b) => b.can("reports.view") },
  { segment: "activity", section: "manage", icon: ScrollText, label: () => "Activity log", allowed: (b) => b.can("activity.view") },
  { segment: "settings", section: "settings", icon: Settings, label: () => "Settings", allowed: (b) => b.can("settings.manage") },
];

/**
 * The sidebar: every screen the person may use in this business, worded with the business type's labels
 * (Members, Clients, Students...). Screens they can't use are hidden rather than shown disabled.
 */
export function navigation(business: BusinessAccess): { main: NavItem[]; manage: NavItem[]; settings: NavItem | null } {
  const base = `/app/${business.id}`;
  const items = SCREENS.filter((s) => s.allowed(business)).map((s) => ({
    section: s.section,
    item: { href: s.segment ? `${base}/${s.segment}` : base, label: s.label(business), icon: s.icon, exact: s.segment === "" },
  }));
  return {
    main: items.filter((i) => i.section === "main").map((i) => i.item),
    manage: items.filter((i) => i.section === "manage").map((i) => i.item),
    settings: items.find((i) => i.section === "settings")?.item ?? null,
  };
}

/** Whether the screen at this path may be opened; paths that aren't screens (like /account) are left to their page. */
export function canOpen(business: BusinessAccess, pathname: string): boolean {
  const segment = pathname.split("/")[3] ?? "";
  const screen = SCREENS.find((s) => s.segment === segment);
  return screen ? screen.allowed(business) : true;
}
