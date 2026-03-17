import apiClient from './axios'
import type { AuditLog, AuditEntityType } from '@/lib/types/audit'
import type { PageResponse } from '@/lib/types/business'

const abase = (businessId: string) => `/api/v1/businesses/${businessId}/audit-logs`

export const auditApi = {
  getAuditLogs: (
    businessId: string,
    params?: {
      page?: number
      size?: number
      entityType?: AuditEntityType
      days?: number
    }
  ) =>
    params?.entityType
      ? apiClient
          .get<PageResponse<AuditLog>>(
            `${abase(businessId)}/entity/${params.entityType}`,
            { params: { page: params.page, size: params.size } }
          )
          .then((r) => r.data)
      : apiClient
          .get<PageResponse<AuditLog>>(abase(businessId), {
            params: { page: params?.page, size: params?.size },
          })
          .then((r) => r.data),

  getRecentAuditLogs: (businessId: string, days = 7) =>
    apiClient
      .get<AuditLog[]>(`${abase(businessId)}/recent`, { params: { days } })
      .then((r) => r.data),
}