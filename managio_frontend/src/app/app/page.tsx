import type { Metadata } from "next";
import { PortfolioPage } from "@/features/business/portfolio-page";

export const metadata: Metadata = { title: "Your businesses" };

export default function Page() {
  return <PortfolioPage />;
}
