"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { api, unwrap, type Schemas } from "@/lib/api/client";
import type { operations } from "@/lib/api/schema";

export type TenantQuery = NonNullable<operations["businesses"]["parameters"]["query"]>;
export type PeopleQuery = NonNullable<operations["people"]["parameters"]["query"]>;
export type TenantView = NonNullable<TenantQuery["view"]>;
export type TenantSort = NonNullable<TenantQuery["sort"]>;
export type PeopleView = NonNullable<PeopleQuery["view"]>;
export type TenantRow = Schemas["TenantRow"];
export type PlatformUserRow = Schemas["PlatformUserRow"];

/** The platform console's data (D-028). The API answers 403 to anyone who isn't a platform administrator. */
export function useAdminAccess() {
  return useQuery({
    queryKey: ["admin", "access"],
    queryFn: async () => {
      await unwrap(api.GET("/api/v1/admin/access"));
      return true;
    },
    staleTime: 300_000,
  });
}

export function useAdminOverview() {
  return useQuery({
    queryKey: ["admin", "overview"],
    queryFn: () => unwrap(api.GET("/api/v1/admin/overview")),
    refetchInterval: 300_000,
  });
}

export function useAdminBusinesses(query: TenantQuery) {
  return useQuery({
    queryKey: ["admin", "businesses", query],
    queryFn: () => unwrap(api.GET("/api/v1/admin/businesses", { params: { query } })),
    placeholderData: keepPreviousData,
  });
}

export function useAdminPeople(query: PeopleQuery) {
  return useQuery({
    queryKey: ["admin", "people", query],
    queryFn: () => unwrap(api.GET("/api/v1/admin/people", { params: { query } })),
    placeholderData: keepPreviousData,
  });
}

export function useAdminHealth() {
  return useQuery({
    queryKey: ["admin", "health"],
    queryFn: () => unwrap(api.GET("/api/v1/admin/health")),
    refetchInterval: 60_000,
  });
}

/** Product areas as the overview's feature usage names them. */
export const FEATURE_LABELS: Record<string, string> = {
  CUSTOMERS: "Customers",
  CATALOG: "Plans, products and services",
  MEMBERSHIPS: "Memberships",
  BILLING: "Invoices and payments",
  EXPENSES: "Expenses",
  PAYROLL: "Payroll",
  TASKS: "Tasks",
  REQUESTS: "Requests",
  DAY_BOOK: "Day book",
  GROUPS: "Groups and batches",
  TEAM: "Team and roles",
  ATTENDANCE: "Attendance",
  PORTAL: "Customer portal",
};
