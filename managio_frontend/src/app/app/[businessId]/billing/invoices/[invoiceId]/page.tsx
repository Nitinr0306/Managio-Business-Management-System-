import type { Metadata } from "next";
import { InvoicePage } from "@/features/billing/invoice-page";

export const metadata: Metadata = { title: "Invoice" };

export default function Page() {
  return <InvoicePage />;
}
