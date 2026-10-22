import type { Metadata } from "next";
import { Suspense } from "react";
import { SignUpPage } from "@/features/auth/account-pages";

export const metadata: Metadata = { title: "Create an account" };

export default function Page() {
  return (
    <Suspense>
      <SignUpPage />
    </Suspense>
  );
}
