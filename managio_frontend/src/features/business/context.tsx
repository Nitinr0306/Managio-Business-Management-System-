"use client";

import { useQuery } from "@tanstack/react-query";
import { createContext, use, type ReactNode } from "react";
import { api, unwrap } from "@/lib/api/client";
import { toAccess, type BusinessAccess } from "./access";

export const myBusinessesKey = ["me", "businesses"] as const;

export function useMyBusinesses() {
  return useQuery({
    queryKey: myBusinessesKey,
    queryFn: () => unwrap(api.GET("/api/v1/me/businesses")),
    select: (businesses) => businesses.map(toAccess),
  });
}

const BusinessContext = createContext<BusinessAccess | null>(null);

export function BusinessProvider({ business, children }: { business: BusinessAccess; children: ReactNode }) {
  return <BusinessContext value={business}>{children}</BusinessContext>;
}

/** The business named in the URL, with the caller's permissions there. Only usable inside the business shell. */
export function useBusiness(): BusinessAccess {
  const business = use(BusinessContext);
  if (!business) {
    throw new Error("useBusiness() must be used inside the business shell");
  }
  return business;
}

/** Query keys are scoped by business, so switching business never shows another business's cached data. */
export function businessKey(businessId: number, ...parts: unknown[]) {
  return ["business", businessId, ...parts] as const;
}
