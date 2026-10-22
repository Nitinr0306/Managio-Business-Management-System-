import type { Metadata } from "next";
import { Suspense } from "react";
import { ForgotPasswordPage } from "@/features/auth/account-pages";

export const metadata: Metadata = { title: "Reset your password" };

export default function Page() {
  return (
    <Suspense>
      <ForgotPasswordPage />
    </Suspense>
  );
}
