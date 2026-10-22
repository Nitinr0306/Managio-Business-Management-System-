import type { Metadata } from "next";
import { ActivityPage } from "@/features/reports/activity-page";

export const metadata: Metadata = { title: "Activity log" };

export default function Page() {
  return <ActivityPage />;
}
