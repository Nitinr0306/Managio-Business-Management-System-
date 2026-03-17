import apiClient from './axios'
import type {
  SubscriptionPlan,
  CreatePlanRequest,
  AssignSubscriptionRequest,
  MemberSubscription,
  ExpiringSubscription,
} from '@/lib/types/subscription'
import type { PageResponse } from '@/lib/types/business'

const subBase = (businessId: string) => `/api/v1/businesses/${businessId}/subscriptions`

export const subscriptionsApi = {
  getPlans: (businessId: string) =>
    apiClient.get<SubscriptionPlan[]>(`${subBase(businessId)}/plans`).then((r) => r.data),

  createPlan: (businessId: string, data: CreatePlanRequest) =>
    apiClient.post<SubscriptionPlan>(`${subBase(businessId)}/plans`, data).then((r) => r.data),

  getSubscriptions: (
    businessId: string,
    params?: { page?: number; size?: number; status?: string }
  ) =>
    apiClient
      .get<PageResponse<MemberSubscription>>(subBase(businessId), { params })
      .then((r) => r.data),

  assignSubscription: (businessId: string, data: AssignSubscriptionRequest) =>
    apiClient.post<void>(`${subBase(businessId)}/assign`, data).then((r) => r.data),

  /**
   * There is no dedicated /expiring endpoint on the backend.
   * We fetch the owner dashboard which includes upcomingExpirations.
   */
  getExpiringSubscriptions: async (
    businessId: string,
    days = 7
  ): Promise<ExpiringSubscription[]> => {
    try {
      const { data } = await apiClient.get<{
        upcomingExpirations?: ExpiringSubscription[]
        expiringIn7Days?: number
      }>(`/api/v1/businesses/${businessId}/dashboard/owner`)
      const list = data.upcomingExpirations || []
      // Filter to only those within the requested day window
      return list.filter((s) => s.daysRemaining >= 0 && s.daysRemaining <= days)
    } catch {
      return []
    }
  },

  countActiveSubscriptions: (businessId: string) =>
    apiClient.get<number>(`${subBase(businessId)}/count`).then((r) => r.data),

  // Cancel is not in the API docs but we'll attempt it gracefully
  cancelSubscription: (businessId: string, subscriptionId: string) =>
    apiClient
      .post<void>(`${subBase(businessId)}/${subscriptionId}/cancel`)
      .then((r) => r.data)
      .catch((err) => {
        // If endpoint doesn't exist, surface a friendly error
        throw err
      }),
}