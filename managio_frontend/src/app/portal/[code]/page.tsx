import type { Metadata } from "next";
import { PortalPage } from "@/features/portal/portal-pages";

export const metadata: Metadata = { title: "Your account" };

export default function Page() {
  return <PortalPage />;
}
