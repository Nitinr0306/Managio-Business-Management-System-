import type { Metadata } from "next";
import { PortalCodePage } from "@/features/portal/portal-pages";

export const metadata: Metadata = { title: "Customer sign-in" };

export default function Page() {
  return <PortalCodePage />;
}
