"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { businessKey, myBusinessesKey, useBusiness } from "@/features/business/context";
import type { PermissionKey } from "@/features/business/access";
import { api, unwrap, type Schemas } from "@/lib/api/client";
import { useSubmissionKey } from "@/lib/api/submission-key";

export type TeamMember = Schemas["TeamMemberResponse"];
export type Role = Schemas["RoleResponse"];
export type PermissionName = Schemas["RoleRequest"]["permissions"][number];

/** Roles are read with permission keys ("customers.view") and saved with their names ("CUSTOMERS_VIEW"). */
export const toPermissionName = (key: string) => key.toUpperCase().replace(/\./g, "_") as PermissionName;

export function useTeam(includeLeft = false, enabled = true) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "team", "list", includeLeft),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/team", { params: { path: { businessId: id }, query: { includeLeft } } })),
    enabled,
  });
}

export function useInvitations(enabled = true) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "team", "invitations"),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/invitations", { params: { path: { businessId: id } } })),
    enabled,
  });
}

export function useRoles(enabled = true) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "team", "roles"),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/roles", { params: { path: { businessId: id } } })),
    enabled,
  });
}

export function usePermissionCatalogue() {
  return useQuery({
    queryKey: ["reference", "permissions"],
    queryFn: () => unwrap(api.GET("/api/v1/reference/permissions")),
    staleTime: Infinity,
  });
}

function useInvalidateTeam() {
  const queryClient = useQueryClient();
  const { id } = useBusiness();
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: businessKey(id, "team") }),
      queryClient.invalidateQueries({ queryKey: businessKey(id, "payroll") }),
      queryClient.invalidateQueries({ queryKey: myBusinessesKey }),
    ]);
}

export function useUpdateMember(memberId: number) {
  const { id } = useBusiness();
  const invalidate = useInvalidateTeam();
  return useMutation({
    mutationFn: (body: Schemas["UpdateMemberRequest"]) =>
      unwrap(api.PATCH("/api/v1/businesses/{businessId}/team/{memberId}", { params: { path: { businessId: id, memberId } }, body })),
    onSuccess: invalidate,
  });
}

export function useMemberAction() {
  const { id } = useBusiness();
  const invalidate = useInvalidateTeam();
  return useMutation({
    mutationFn: ({ memberId, action }: { memberId: number; action: "suspend" | "reactivate" | "remove" }) => {
      const path = { businessId: id, memberId };
      if (action === "suspend") return unwrap(api.POST("/api/v1/businesses/{businessId}/team/{memberId}/suspend", { params: { path } }));
      if (action === "reactivate") return unwrap(api.POST("/api/v1/businesses/{businessId}/team/{memberId}/reactivate", { params: { path } }));
      return unwrap(api.POST("/api/v1/businesses/{businessId}/team/{memberId}/remove", { params: { path } }));
    },
    onSuccess: invalidate,
  });
}

export function useInvite() {
  const { id } = useBusiness();
  const invalidate = useInvalidateTeam();
  return useMutation({
    mutationFn: (body: Schemas["InviteRequest"]) =>
      unwrap(api.POST("/api/v1/businesses/{businessId}/invitations", { params: { path: { businessId: id } }, body })),
    onSuccess: invalidate,
  });
}

export function useInvitationAction() {
  const { id } = useBusiness();
  const invalidate = useInvalidateTeam();
  return useMutation({
    mutationFn: ({ invitationId, action }: { invitationId: number; action: "resend" | "revoke" }) => {
      const path = { businessId: id, invitationId };
      return action === "resend"
        ? unwrap(api.POST("/api/v1/businesses/{businessId}/invitations/{invitationId}/resend", { params: { path } }))
        : unwrap(api.DELETE("/api/v1/businesses/{businessId}/invitations/{invitationId}", { params: { path } }));
    },
    onSuccess: invalidate,
  });
}

export function useSaveRole(roleId?: number) {
  const { id } = useBusiness();
  const invalidate = useInvalidateTeam();
  return useMutation({
    mutationFn: (body: Schemas["RoleRequest"]) =>
      roleId == null
        ? unwrap(api.POST("/api/v1/businesses/{businessId}/roles", { params: { path: { businessId: id } }, body }))
        : unwrap(api.PATCH("/api/v1/businesses/{businessId}/roles/{roleId}", { params: { path: { businessId: id, roleId } }, body })),
    onSuccess: invalidate,
  });
}

export function useDeleteRole() {
  const { id } = useBusiness();
  const invalidate = useInvalidateTeam();
  return useMutation({
    mutationFn: (roleId: number) => unwrap(api.DELETE("/api/v1/businesses/{businessId}/roles/{roleId}", { params: { path: { businessId: id, roleId } } })),
    onSuccess: invalidate,
  });
}

/* ── Payroll (PAY-1, PAY-2) ───────────────────────────────────────────────────────────── */

export function usePayroll(month?: string) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "payroll", month ?? "current"),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/payroll", { params: { path: { businessId: id }, query: { month } } })),
  });
}

function useInvalidatePayroll() {
  const queryClient = useQueryClient();
  const { id } = useBusiness();
  return () =>
    Promise.all(["payroll", "expenses", "today", "reports", "day-book"].map((part) => queryClient.invalidateQueries({ queryKey: businessKey(id, part) })));
}

export function usePreparePayroll() {
  const { id } = useBusiness();
  const invalidate = useInvalidatePayroll();
  return useMutation({
    mutationFn: (month?: string) => unwrap(api.POST("/api/v1/businesses/{businessId}/payroll/prepare", { params: { path: { businessId: id } }, body: { month } })),
    onSuccess: invalidate,
  });
}

export function usePaySalary() {
  const { id } = useBusiness();
  const invalidate = useInvalidatePayroll();
  const key = useSubmissionKey();
  return useMutation({
    mutationFn: ({ entryId, body }: { entryId: number; body: Schemas["PayBody"] }) =>
      unwrap(api.POST("/api/v1/businesses/{businessId}/payroll/{entryId}/payments", { params: { path: { businessId: id, entryId }, header: key.header() }, body })),
    onSuccess: () => {
      key.renew();
      return invalidate();
    },
  });
}

export function useAdjustSalary() {
  const { id } = useBusiness();
  const invalidate = useInvalidatePayroll();
  return useMutation({
    mutationFn: ({ entryId, body }: { entryId: number; body: Schemas["AdjustBody"] }) =>
      unwrap(api.PATCH("/api/v1/businesses/{businessId}/payroll/{entryId}", { params: { path: { businessId: id, entryId } }, body })),
    onSuccess: invalidate,
  });
}

/** Groups of permissions as owners think about them, for the role editor. */
export const PERMISSION_GROUPS: { title: string; keys: PermissionKey[] }[] = [
  { title: "Customers", keys: ["customers.view", "customers.manage", "customers.archive", "customers.import", "attendance.mark"] },
  { title: "Plans and sales", keys: ["offerings.manage", "memberships.manage"] },
  { title: "Money", keys: ["billing.view", "billing.collect", "billing.discount", "billing.void", "expenses.view", "expenses.record", "daybook.close"] },
  { title: "Team", keys: ["team.view", "team.manage", "payroll.manage", "tasks.manage", "requests.approve"] },
  { title: "Oversight", keys: ["reports.view", "activity.view", "settings.manage"] },
];
