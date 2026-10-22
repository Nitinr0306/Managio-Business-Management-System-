import type { Metadata } from "next";
import { Suspense } from "react";
import { SignInPage } from "@/features/auth/sign-in-page";

export const metadata: Metadata = { title: "Sign in" };

export default function Page() {
  return (
    <Suspense>
      <SignInPage />
    </Suspense>
  );
}
