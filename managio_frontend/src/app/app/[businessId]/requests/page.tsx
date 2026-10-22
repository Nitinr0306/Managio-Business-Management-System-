import type { Metadata } from "next";
import { RequestsPage } from "@/features/operations/requests-page";

export const metadata: Metadata = { title: "Requests" };

export default function Page() {
  return <RequestsPage />;
}
