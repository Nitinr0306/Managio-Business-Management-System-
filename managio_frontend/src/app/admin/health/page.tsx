import type { Metadata } from "next";
import { HealthPage } from "@/features/admin/health-page";

export const metadata: Metadata = { title: "System health" };

export default function Page() {
  return <HealthPage />;
}
