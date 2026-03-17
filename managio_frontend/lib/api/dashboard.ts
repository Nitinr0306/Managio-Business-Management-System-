import apiClient from './axios'
import type {
  OwnerDashboardResponse,
  StaffDashboardResponse,
  MemberDashboardResponse,
} from '@/lib/types/dashboard'

export const dashboardApi = {
  getOwnerDashboard: (businessId: string) =>
    apiClient
      .get<OwnerDashboardResponse>(`/api/v1/businesses/${businessId}/dashboard/owner`)
      .then((r) => r.data),

  getStaffDashboard: (businessId: string) =>
    apiClient
      .get<StaffDashboardResponse>(`/api/v1/businesses/${businessId}/dashboard/staff`)
      .then((r) => r.data),

  getMemberDashboard: (memberId: string) =>
    apiClient
      .get<MemberDashboardResponse>(`/api/v1/members/${memberId}/dashboard`)
      .then((r) => r.data),
}

