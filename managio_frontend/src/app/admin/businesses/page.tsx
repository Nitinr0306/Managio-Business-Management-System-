import type { Metadata } from "next";
import { BusinessesPage } from "@/features/admin/businesses-page";

export const metadata: Metadata = { title: "Businesses on Managio" };

export default function Page() {
  return <BusinessesPage />;
}
