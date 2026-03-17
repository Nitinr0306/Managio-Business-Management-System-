import { useQuery } from '@tanstack/react-query'
import { auditApi } from '@/lib/api/audit'
import type { AuditEntityType } from '@/lib/types/audit'

export function useAuditLogs(
  businessId: string,
  params?: {
    page?: number
    size?: number
    entityType?: AuditEntityType
    days?: number
  }
) {
  return useQuery({
    queryKey: ['audit-logs', businessId, params],
    queryFn: () => auditApi.getAuditLogs(businessId, params),
    enabled: !!businessId,
  })
}

export function useRecentAuditLogs(businessId: string, days = 7) {
  return useQuery({
    queryKey: ['recent-audit-logs', businessId, days],
    queryFn: () => auditApi.getRecentAuditLogs(businessId, days),
    enabled: !!businessId,
  })
}