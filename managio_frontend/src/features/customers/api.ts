"use client";

import { keepPreviousData, skipToken, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { businessKey, useBusiness } from "@/features/business/context";
import { api, unwrap, type Schemas } from "@/lib/api/client";

export type CustomerView = NonNullable<CustomerListParams["view"]>;
export type CustomerListParams = {
  view?: "ACTIVE" | "ENDING_SOON" | "LAPSED" | "NO_PLAN" | "WITH_DUES" | "ARCHIVED" | "ALL";
  q?: string;
  groupId?: number;
  sort?: "NAME" | "NUMBER" | "NEWEST" | "ENDING" | "DUES";
  page?: number;
  size?: number;
};
export type MembershipState = Schemas["CustomerRow"]["membershipState"];

export function useCustomers(params: CustomerListParams, enabled = true) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "customers", "list", params),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/customers", { params: { path: { businessId: id }, query: params } })),
    placeholderData: keepPreviousData,
    enabled,
  });
}

export function useCustomerCounts() {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "customers", "counts"),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/customers/counts", { params: { path: { businessId: id } } })),
  });
}

export function useCustomer(customerId: number | null) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "customers", "one", customerId),
    queryFn:
      customerId == null
        ? skipToken
        : () => unwrap(api.GET("/api/v1/businesses/{businessId}/customers/{customerId}", { params: { path: { businessId: id, customerId } } })),
  });
}

export function useCustomerMemberships(customerId: number | null) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "customers", "memberships", customerId),
    queryFn:
      customerId == null
        ? skipToken
        : () =>
            unwrap(api.GET("/api/v1/businesses/{businessId}/customers/{customerId}/memberships", {
              params: { path: { businessId: id, customerId } },
            })),
  });
}

/** After anything that changes a customer's standing, refresh every view that shows it. */
export function useRefreshCustomerData() {
  const queryClient = useQueryClient();
  const { id } = useBusiness();
  return () =>
    Promise.all(
      ["customers", "memberships", "invoices", "payments", "today", "reports", "day-book"].map((part) =>
        queryClient.invalidateQueries({ queryKey: businessKey(id, part) }),
      ),
    );
}

export function useSaveCustomer(customerId?: number) {
  const { id } = useBusiness();
  const refresh = useRefreshCustomerData();
  return useMutation({
    mutationFn: (body: Schemas["CustomerRequest"]) =>
      customerId == null
        ? unwrap(api.POST("/api/v1/businesses/{businessId}/customers", { params: { path: { businessId: id } }, body }))
        : unwrap(api.PATCH("/api/v1/businesses/{businessId}/customers/{customerId}", {
            params: { path: { businessId: id, customerId } },
            body,
          })),
    onSuccess: refresh,
  });
}

export function useCustomerAction() {
  const { id } = useBusiness();
  const refresh = useRefreshCustomerData();
  return useMutation({
    mutationFn: ({ customerId, action }: { customerId: number; action: "archive" | "restore" | "invite" | "disable" }) => {
      const path = { businessId: id, customerId };
      switch (action) {
        case "archive":
          return unwrap(api.POST("/api/v1/businesses/{businessId}/customers/{customerId}/archive", { params: { path } }));
        case "restore":
          return unwrap(api.POST("/api/v1/businesses/{businessId}/customers/{customerId}/restore", { params: { path } }));
        case "invite":
          return unwrap(api.POST("/api/v1/businesses/{businessId}/customers/{customerId}/portal/invite", { params: { path } }));
        case "disable":
          return unwrap(api.POST("/api/v1/businesses/{businessId}/customers/{customerId}/portal/disable", { params: { path } }));
      }
    },
    onSuccess: refresh,
  });
}

/** How a customer's membership state reads, and its colour. */
export const MEMBERSHIP_STATE: Record<MembershipState, { label: string; tone: "ok" | "due" | "bad" | "neutral" | "info" }> = {
  ACTIVE: { label: "Active", tone: "ok" },
  ENDING_SOON: { label: "Ending soon", tone: "due" },
  UPCOMING: { label: "Starts later", tone: "info" },
  LAPSED: { label: "Lapsed", tone: "bad" },
  NONE: { label: "No plan", tone: "neutral" },
};
