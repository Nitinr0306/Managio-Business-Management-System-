import type { Metadata } from "next";
import { PeoplePage } from "@/features/admin/people-page";

export const metadata: Metadata = { title: "People on Managio" };

export default function Page() {
  return <PeoplePage />;
}
