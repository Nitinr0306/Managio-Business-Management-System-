import apiClient from './axios'
import type { Member, CreateMemberRequest, UpdateMemberRequest, MemberStats } from '@/lib/types/member'
import type { MemberSubscription } from '@/lib/types/subscription'
import type { Payment } from '@/lib/types/payment'
import type { PageResponse } from '@/lib/types/business'

const base = (businessId: string) => `/api/v1/businesses/${businessId}/members`

export const membersApi = {
  getMembers: (
    businessId: string,
    params?: { page?: number; size?: number; search?: string; status?: string; sort?: string }
  ) => apiClient.get<PageResponse<Member>>(base(businessId), { params }).then((r) => r.data),

  getMember: (businessId: string, memberId: string) =>
    apiClient.get<Member>(`${base(businessId)}/${memberId}`).then((r) => r.data),

  getMemberProfile: (businessId: string, memberId: string) =>
    apiClient
      .get<{
        id: string
        businessId: string
        firstName: string
        lastName: string
        fullName: string
        phone?: string
        email?: string
        dateOfBirth?: string
        gender?: string
        address?: string
        status: string
        notes?: string
        createdAt: string
        updatedAt: string
        activeSubscription?: {
          subscriptionId: number
          planId: number
          planName: string
          startDate: string
          endDate: string
          status: string
          daysRemaining: number
          amount: number
        }
        paymentHistory: Payment[]
        totalPaid: number
        totalSubscriptions: number
      }>(`${base(businessId)}/${memberId}/profile`)
      .then((r) => r.data),

  /**
   * Backend has no /stats endpoint — derive from the profile response.
   */
  getMemberStats: async (businessId: string, memberId: string): Promise<MemberStats> => {
    try {
      const profile = await membersApi.getMemberProfile(businessId, memberId)
      const activeSubs = profile.activeSubscription ? 1 : 0
      return {
        totalSubscriptions: profile.totalSubscriptions || 0,
        activeSubscriptions: activeSubs,
        completedSubscriptions: Math.max(0, (profile.totalSubscriptions || 0) - activeSubs),
        totalAmountPaid: profile.totalPaid || 0,
        memberSince: profile.createdAt,
      }
    } catch {
      return {
        totalSubscriptions: 0,
        activeSubscriptions: 0,
        completedSubscriptions: 0,
        totalAmountPaid: 0,
        memberSince: new Date().toISOString(),
      }
    }
  },

  getMemberSubscriptionHistory: (businessId: string, memberId: string) =>
    apiClient
      .get<MemberSubscription[]>(`${base(businessId)}/${memberId}/subscription-history`)
      .then((r) => r.data),

  getMemberPaymentHistory: (businessId: string, memberId: string) =>
    apiClient
      .get<Payment[]>(`${base(businessId)}/${memberId}/payment-history`)
      .then((r) => r.data),

  createMember: (businessId: string, data: CreateMemberRequest) =>
    apiClient.post<Member>(base(businessId), data).then((r) => r.data),

  updateMember: (businessId: string, memberId: string, data: UpdateMemberRequest) =>
    apiClient.put<Member>(`${base(businessId)}/${memberId}`, data).then((r) => r.data),

  deactivateMember: (businessId: string, memberId: string) =>
    apiClient.post<void>(`${base(businessId)}/${memberId}/deactivate`).then((r) => r.data),

  disableMemberPortal: (memberId: string) =>
    apiClient.post<void>(`/api/v1/members/auth/${memberId}/disable`).then((r) => r.data),

  enableMemberPortal: (memberId: string) =>
    apiClient.post<void>(`/api/v1/members/auth/${memberId}/enable`).then((r) => r.data),

  searchMembers: (businessId: string, query: string, params?: { page?: number; size?: number }) =>
    apiClient
      .get<PageResponse<Member>>(`${base(businessId)}/search`, { params: { query, ...params } })
      .then((r) => r.data),

  exportMembers: (businessId: string) =>
    apiClient
      .get<Blob>(`${base(businessId)}/export`, { responseType: 'blob' })
      .then((r) => r.data),

  importMembers: (businessId: string, file: File) => {
    const fd = new FormData()
    fd.append('file', file)
    return apiClient
      .post(`${base(businessId)}/import`, fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data)
  },

  downloadImportTemplate: (businessId: string) =>
    apiClient
      .get<Blob>(`${base(businessId)}/import-template`, { responseType: 'blob' })
      .then((r) => r.data),

  countMembers: (businessId: string) =>
    apiClient.get<number>(`${base(businessId)}/count`).then((r) => r.data),

  countActiveMembers: (businessId: string) =>
    apiClient.get<number>(`${base(businessId)}/count/active`).then((r) => r.data),
}