import type { Metadata } from "next";
import { CatalogPage } from "@/features/memberships/catalog-page";

export const metadata: Metadata = { title: "Plans and products" };

export default function Page() {
  return <CatalogPage />;
}
