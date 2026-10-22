"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { businessKey, useBusiness } from "@/features/business/context";
import { useRefreshCustomerData } from "@/features/customers/api";
import { api, unwrap, type Schemas } from "@/lib/api/client";
import { useSubmissionKey } from "@/lib/api/submission-key";

export type MembershipView = "ALL" | "ACTIVE" | "ENDING_SOON" | "UPCOMING" | "EXPIRED" | "CANCELLED";
export type MembershipItemState = Schemas["MembershipRow"]["state"];
export type OfferingKind = Schemas["OfferingResponse"]["kind"];

export const MEMBERSHIP_ITEM_STATE: Record<MembershipItemState, { label: string; tone: "ok" | "due" | "bad" | "neutral" | "info" }> = {
  ACTIVE: { label: "Active", tone: "ok" },
  ENDING_SOON: { label: "Ending soon", tone: "due" },
  UPCOMING: { label: "Starts later", tone: "info" },
  EXPIRED: { label: "Ended", tone: "bad" },
  CANCELLED: { label: "Cancelled", tone: "neutral" },
};

export function useMemberships(params: { view?: MembershipView; needsRenewal?: boolean; page?: number; size?: number }, enabled = true) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "memberships", "list", params),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/memberships", { params: { path: { businessId: id }, query: params } })),
    placeholderData: keepPreviousData,
    enabled,
  });
}

export function useStartMembership() {
  const { id } = useBusiness();
  const refresh = useRefreshCustomerData();
  const key = useSubmissionKey();
  return useMutation({
    mutationFn: (body: Schemas["StartBody"]) => unwrap(api.POST("/api/v1/businesses/{businessId}/memberships", { params: { path: { businessId: id }, header: key.header() }, body })),
    onSuccess: () => {
      key.renew();
      return refresh();
    },
  });
}

export function useRenewMembership() {
  const { id } = useBusiness();
  const refresh = useRefreshCustomerData();
  const key = useSubmissionKey();
  return useMutation({
    mutationFn: ({ membershipId, body }: { membershipId: number; body: Schemas["RenewBody"] }) =>
      unwrap(api.POST("/api/v1/businesses/{businessId}/memberships/{membershipId}/renew", { params: { path: { businessId: id, membershipId }, header: key.header() }, body })),
    onSuccess: () => {
      key.renew();
      return refresh();
    },
  });
}

export function useCancelMembership() {
  const { id } = useBusiness();
  const refresh = useRefreshCustomerData();
  return useMutation({
    mutationFn: ({ membershipId, reason }: { membershipId: number; reason?: string }) =>
      unwrap(api.POST("/api/v1/businesses/{businessId}/memberships/{membershipId}/cancel", {
        params: { path: { businessId: id, membershipId } },
        body: { reason },
      })),
    onSuccess: refresh,
  });
}

/* ── Offerings: plans, services and products ───────────────────────────────────────────── */

export function useOfferings(kind?: OfferingKind, includeInactive = false) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "offerings", kind ?? "all", includeInactive),
    queryFn: () =>
      unwrap(api.GET("/api/v1/businesses/{businessId}/offerings", { params: { path: { businessId: id }, query: { kind, includeInactive } } })),
  });
}

export function useSaveOffering(offeringId?: number) {
  const { id } = useBusiness();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: Schemas["OfferingRequest"]) =>
      offeringId == null
        ? unwrap(api.POST("/api/v1/businesses/{businessId}/offerings", { params: { path: { businessId: id } }, body }))
        : unwrap(api.PATCH("/api/v1/businesses/{businessId}/offerings/{offeringId}", { params: { path: { businessId: id, offeringId } }, body })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: businessKey(id, "offerings") }),
  });
}

/** Changes an existing offering chosen at call time (for row actions such as deactivate). */
export function useUpdateOffering() {
  const { id } = useBusiness();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ offeringId, body }: { offeringId: number; body: Schemas["OfferingRequest"] }) =>
      unwrap(api.PATCH("/api/v1/businesses/{businessId}/offerings/{offeringId}", { params: { path: { businessId: id, offeringId } }, body })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: businessKey(id, "offerings") }),
  });
}

export function useDeleteOffering() {
  const { id } = useBusiness();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (offeringId: number) =>
      unwrap(api.DELETE("/api/v1/businesses/{businessId}/offerings/{offeringId}", { params: { path: { businessId: id, offeringId } } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: businessKey(id, "offerings") }),
  });
}

export function useAdjustStock() {
  const { id } = useBusiness();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ offeringId, change, reason }: { offeringId: number; change: number; reason?: string }) =>
      unwrap(api.POST("/api/v1/businesses/{businessId}/offerings/{offeringId}/stock", {
        params: { path: { businessId: id, offeringId } },
        body: { change, reason },
      })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: businessKey(id, "offerings") }),
  });
}

/** "30 days", "3 months" */
export function durationLabel(offering: Pick<Schemas["OfferingResponse"], "durationValue" | "durationUnit">): string {
  if (!offering.durationValue || !offering.durationUnit) return "";
  const unit = offering.durationUnit === "MONTHS" ? "month" : "day";
  return `${offering.durationValue} ${unit}${offering.durationValue === 1 ? "" : "s"}`;
}
