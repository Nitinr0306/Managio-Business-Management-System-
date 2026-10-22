import type { Metadata } from "next";
import { Suspense } from "react";
import { ResetPasswordPage } from "@/features/auth/account-pages";

export const metadata: Metadata = { title: "Choose a new password" };

export default function Page() {
  return (
    <Suspense>
      <ResetPasswordPage />
    </Suspense>
  );
}
