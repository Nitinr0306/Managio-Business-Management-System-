import type { Metadata } from "next";
import { CreateBusinessPage } from "@/features/business/create-business-page";

export const metadata: Metadata = { title: "Add a business" };

export default function Page() {
  return <CreateBusinessPage />;
}
