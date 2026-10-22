import type { Metadata } from "next";
import { OverviewPage } from "@/features/admin/overview-page";

export const metadata: Metadata = { title: "Platform overview" };

export default function Page() {
  return <OverviewPage />;
}
