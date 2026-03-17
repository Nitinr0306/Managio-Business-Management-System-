import apiClient from './axios'
import { businessApi } from './business'
import type { Payment, RecordPaymentRequest, PaymentMethodStat, RevenueStats } from '@/lib/types/payment'
import type { PageResponse } from '@/lib/types/business'

const pbase = (bid: string) => `/api/v1/businesses/${bid}/payments`

export const paymentsApi = {
  getPayments: (bid: string, params?: { page?: number; size?: number; paymentMethod?: string }) =>
    apiClient.get<PageResponse<Payment>>(pbase(bid), { params }).then((r) => r.data),

  recordPayment: (bid: string, data: RecordPaymentRequest) =>
    apiClient.post<Payment>(pbase(bid), data).then((r) => r.data),

  getPaymentMethodStats: (bid: string) =>
    apiClient.get<PaymentMethodStat[]>(`${pbase(bid)}/stats`).then((r) => {
      const d = r.data as any
      // Handle both flat array and nested byPaymentMethod object from backend
      if (Array.isArray(d)) return d
      if (d?.byPaymentMethod) return d.byPaymentMethod
      return []
    }),

  getRecentPayments: (bid: string, days = 7) =>
    apiClient.get<Payment[]>(`${pbase(bid)}/recent`, { params: { days } }).then((r) => r.data),

  getMonthlyRevenue: (bid: string) =>
    apiClient.get<number>(`${pbase(bid)}/revenue/monthly`).then((r) => r.data),

  getMemberPaymentHistory: (bid: string, memberId: string) =>
    apiClient.get<Payment[]>(`${pbase(bid)}/member/${memberId}`).then((r) => r.data),

  exportPayments: (bid: string) =>
    apiClient.get<Blob>(`${pbase(bid)}/export`, { responseType: 'blob' }).then((r) => r.data),

  getRevenueStats: async (bid: string): Promise<RevenueStats> => {
    const stats = await businessApi.getBusinessStats(bid)
    return {
      totalRevenue: stats.totalRevenue,
      monthlyRevenue: stats.monthlyRevenue,
      todayRevenue: stats.todayRevenue,
      averagePerMember: stats.averageRevenuePerMember,
      revenueByMonth: [],
    }
  },
}