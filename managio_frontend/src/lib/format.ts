/**
 * Display formatting. Money is Indian rupees with Indian digit grouping (₹1,25,000). Business dates arrive
 * from the API as "YYYY-MM-DD" in the business's own time zone and are shown as written, never shifted by the
 * browser's zone.
 */

const wholeRupees = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });
const paise = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", minimumFractionDigits: 2, maximumFractionDigits: 2 });
const plainNumber = new Intl.NumberFormat("en-IN");

export function formatMoney(amount: number | null | undefined): string {
  if (amount == null || Number.isNaN(amount)) {
    return "—";
  }
  return Math.round(amount * 100) % 100 === 0 ? wholeRupees.format(amount) : paise.format(amount);
}

export function formatNumber(value: number | null | undefined): string {
  return value == null ? "—" : plainNumber.format(value);
}

const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
const WEEKDAYS = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];

/** Splits "YYYY-MM-DD" without going through Date parsing, which would apply the browser's time zone. */
export function parseIsoDate(iso: string): { year: number; month: number; day: number } {
  const [year, month, day] = iso.slice(0, 10).split("-").map(Number);
  return { year, month, day };
}

/** "20 Oct 2026" */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  const { year, month, day } = parseIsoDate(iso);
  return `${day} ${MONTHS[month - 1]} ${year}`;
}

/** "20 Oct" */
export function formatShortDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  const { month, day } = parseIsoDate(iso);
  return `${day} ${MONTHS[month - 1]}`;
}

/** "Tuesday, 20 October 2026" */
export function formatLongDate(iso: string): string {
  const { year, month, day } = parseIsoDate(iso);
  const weekday = WEEKDAYS[new Date(Date.UTC(year, month - 1, day)).getUTCDay()];
  const longMonth = new Intl.DateTimeFormat("en-IN", { month: "long", timeZone: "UTC" })
    .format(new Date(Date.UTC(year, month - 1, day)));
  return `${weekday}, ${day} ${longMonth} ${year}`;
}

/** "Oct 2026" for a month bucket such as "2026-10-01". */
export function formatMonth(iso: string): string {
  const { year, month } = parseIsoDate(iso);
  return `${MONTHS[month - 1]} ${year}`;
}

/** The calendar date of a timestamp in the viewer's own time zone: "20 Oct 2026". */
export function formatInstantDate(instant: string | null | undefined): string {
  if (!instant) return "—";
  return new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short", year: "numeric" }).format(new Date(instant));
}

/** A timestamp in the viewer's own time zone: "20 Oct, 10:30 am". */
export function formatDateTime(instant: string | null | undefined): string {
  if (!instant) return "—";
  return new Intl.DateTimeFormat("en-IN", {
    day: "numeric",
    month: "short",
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date(instant));
}

/** "10:30 am" */
export function formatTime(instant: string | null | undefined): string {
  if (!instant) return "—";
  return new Intl.DateTimeFormat("en-IN", { hour: "numeric", minute: "2-digit" }).format(new Date(instant));
}

/** Whole days from one business date to another (negative when `to` is earlier). */
export function daysBetween(from: string, to: string): number {
  const a = parseIsoDate(from);
  const b = parseIsoDate(to);
  return Math.round((Date.UTC(b.year, b.month - 1, b.day) - Date.UTC(a.year, a.month - 1, a.day)) / 86_400_000);
}

/** Adds days to a business date and returns "YYYY-MM-DD". */
export function addDays(iso: string, days: number): string {
  const { year, month, day } = parseIsoDate(iso);
  return new Date(Date.UTC(year, month - 1, day + days)).toISOString().slice(0, 10);
}

/** Today's date in the browser, for form defaults. The API decides the business's own today. */
export function localToday(): string {
  const now = new Date();
  return new Date(Date.UTC(now.getFullYear(), now.getMonth(), now.getDate())).toISOString().slice(0, 10);
}

/** "in 3 days", "today", "2 days ago" */
export function relativeDays(days: number): string {
  if (days === 0) return "today";
  if (days === 1) return "tomorrow";
  if (days === -1) return "yesterday";
  return days > 0 ? `in ${days} days` : `${-days} days ago`;
}

/** How long ago a timestamp was, in calendar days where the viewer is: "today", "yesterday", "12 days ago". */
export function daysAgo(instant: string | null | undefined, now = new Date()): string {
  if (!instant) return "never";
  const startOfDay = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
  return relativeDays(-Math.round((startOfDay(now) - startOfDay(new Date(instant))) / 86_400_000));
}

export function plural(count: number, one: string, many = `${one}s`): string {
  return `${formatNumber(count)} ${count === 1 ? one : many}`;
}

export function initials(name: string | null | undefined): string {
  if (!name) return "?";
  const parts = name.trim().split(/\s+/).filter(Boolean);
  const letters = parts.length === 1 ? parts[0].slice(0, 2) : parts[0][0] + parts[parts.length - 1][0];
  return letters.toUpperCase();
}

/** "BANK_TRANSFER" → "Bank transfer" */
export function humanize(value: string | null | undefined): string {
  if (!value) return "";
  const text = value.replace(/_/g, " ").toLowerCase();
  return text.charAt(0).toUpperCase() + text.slice(1);
}
