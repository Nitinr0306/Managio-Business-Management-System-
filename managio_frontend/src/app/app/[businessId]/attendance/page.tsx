import type { Metadata } from "next";
import { AttendancePage } from "@/features/customers/attendance-page";

export const metadata: Metadata = { title: "Attendance" };

export default function Page() {
  return <AttendancePage />;
}
