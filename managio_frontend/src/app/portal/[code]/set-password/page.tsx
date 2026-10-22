import type { Metadata } from "next";
import { PortalSetPasswordPage } from "@/features/portal/portal-pages";

export const metadata: Metadata = { title: "Choose your password" };

export default function Page() {
  return <PortalSetPasswordPage />;
}
