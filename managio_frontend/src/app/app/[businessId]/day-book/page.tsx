import type { Metadata } from "next";
import { DayBookPage } from "@/features/operations/day-book-page";

export const metadata: Metadata = { title: "Day book" };

export default function Page() {
  return <DayBookPage />;
}
