"use client";

import { useQuery } from "@tanstack/react-query";
import { businessKey, useBusiness } from "@/features/business/context";
import { api, unwrap } from "@/lib/api/client";

export function useToday() {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "today"),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/today", { params: { path: { businessId: id } } })),
    refetchInterval: 120_000,
  });
}

export function useRunway(days = 30, enabled = true) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "reports", "runway", days),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/reports/runway", { params: { path: { businessId: id }, query: { days } } })),
    enabled,
  });
}

export function useProfitAndLoss(from?: string, to?: string) {
  const { id } = useBusiness();
  return useQuery({
    queryKey: businessKey(id, "reports", "profit-loss", from, to),
    queryFn: () =>
      unwrap(api.GET("/api/v1/businesses/{businessId}/reports/profit-loss", { params: { path: { businessId: id }, query: { from, to } } })),
  });
}

export function usePortfolio() {
  return useQuery({
    queryKey: ["me", "portfolio"],
    queryFn: () => unwrap(api.GET("/api/v1/me/portfolio")),
  });
}
