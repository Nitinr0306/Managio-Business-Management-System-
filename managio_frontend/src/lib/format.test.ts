import { describe, expect, it } from "vitest";
import {
  addDays,
  daysAgo,
  daysBetween,
  formatDate,
  formatLongDate,
  formatMoney,
  formatMonth,
  formatShortDate,
  humanize,
  initials,
  plural,
  relativeDays,
} from "./format";

describe("formatMoney", () => {
  it("uses Indian digit grouping and drops paise for whole rupees", () => {
    expect(formatMoney(125000)).toBe("₹1,25,000");
    expect(formatMoney(10000000)).toBe("₹1,00,00,000");
    expect(formatMoney(0)).toBe("₹0");
  });

  it("keeps two decimals when there are paise", () => {
    expect(formatMoney(1499.5)).toBe("₹1,499.50");
    expect(formatMoney(0.1 + 0.2)).toBe("₹0.30");
  });

  it("shows a dash for missing values", () => {
    expect(formatMoney(null)).toBe("—");
    expect(formatMoney(undefined)).toBe("—");
    expect(formatMoney(Number.NaN)).toBe("—");
  });

  it("keeps the sign of negative amounts", () => {
    expect(formatMoney(-2500)).toContain("2,500");
    expect(formatMoney(-2500).startsWith("-")).toBe(true);
  });
});

describe("business dates", () => {
  it("are shown as written, without shifting by the browser's time zone", () => {
    expect(formatDate("2026-10-20")).toBe("20 Oct 2026");
    expect(formatDate("2026-01-01")).toBe("1 Jan 2026");
    expect(formatShortDate("2026-12-31")).toBe("31 Dec");
    expect(formatMonth("2026-10-01")).toBe("Oct 2026");
    expect(formatDate(null)).toBe("—");
  });

  it("include the weekday in the long form", () => {
    expect(formatLongDate("2026-10-20")).toBe("Tuesday, 20 October 2026");
  });

  it("count days across month and year ends", () => {
    expect(daysBetween("2026-10-20", "2026-10-22")).toBe(2);
    expect(daysBetween("2026-12-31", "2027-01-01")).toBe(1);
    expect(daysBetween("2026-10-22", "2026-10-20")).toBe(-2);
    expect(addDays("2026-10-31", 1)).toBe("2026-11-01");
    expect(addDays("2026-03-01", -1)).toBe("2026-02-28");
    expect(addDays("2028-03-01", -1)).toBe("2028-02-29");
  });

  it("read naturally relative to today", () => {
    expect(relativeDays(0)).toBe("today");
    expect(relativeDays(1)).toBe("tomorrow");
    expect(relativeDays(-1)).toBe("yesterday");
    expect(relativeDays(5)).toBe("in 5 days");
    expect(relativeDays(-3)).toBe("3 days ago");
  });
});

describe("text helpers", () => {
  it("make initials from one or more names", () => {
    expect(initials("Ananya Rao")).toBe("AR");
    expect(initials("Ironwood Fitness Studio")).toBe("IS");
    expect(initials("Madonna")).toBe("MA");
    expect(initials(null)).toBe("?");
  });

  it("turn API constants into words", () => {
    expect(humanize("BANK_TRANSFER")).toBe("Bank transfer");
    expect(humanize("")).toBe("");
  });

  it("pluralise with grouped counts", () => {
    expect(plural(1, "invoice")).toBe("1 invoice");
    expect(plural(3, "invoice")).toBe("3 invoices");
    expect(plural(1200, "person", "people")).toBe("1,200 people");
  });
});

describe("daysAgo", () => {
  const now = new Date(2026, 9, 21, 9, 0);
  it("counts calendar days, not 24-hour periods", () => {
    expect(daysAgo(new Date(2026, 9, 21, 0, 5).toISOString(), now)).toBe("today");
    expect(daysAgo(new Date(2026, 9, 20, 23, 55).toISOString(), now)).toBe("yesterday");
    expect(daysAgo(new Date(2026, 9, 9, 12, 0).toISOString(), now)).toBe("12 days ago");
  });
  it("says never when there is no time", () => {
    expect(daysAgo(null, now)).toBe("never");
  });
});
