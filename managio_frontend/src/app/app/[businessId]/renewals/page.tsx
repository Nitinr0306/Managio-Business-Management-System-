import type { Metadata } from "next";
import { RenewalsPage } from "@/features/memberships/renewals-page";

export const metadata: Metadata = { title: "Renewals" };

export default function Page() {
  return <RenewalsPage />;
}
