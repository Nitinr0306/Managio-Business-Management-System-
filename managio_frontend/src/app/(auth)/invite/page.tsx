import type { Metadata } from "next";
import { Suspense } from "react";
import { InvitationPage } from "@/features/auth/account-pages";

export const metadata: Metadata = { title: "Join a business" };

export default function Page() {
  return (
    <Suspense>
      <InvitationPage />
    </Suspense>
  );
}
