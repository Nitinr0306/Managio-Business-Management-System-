import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { paymentsApi } from '@/lib/api/payments'
import { getErrorMessage, getErrorStatus, isNetworkError } from '@/lib/utils/errors'
import type { RecordPaymentRequest } from '@/lib/types/payment'

export function usePayments(
  bid: string,
  params?: { page?: number; size?: number; paymentMethod?: string }
) {
  return useQuery({
    queryKey: ['payments', bid, params],
    queryFn: () => paymentsApi.getPayments(bid, params),
    enabled: !!bid && bid !== 'undefined',
    placeholderData: (prev: any) => prev,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403) return false
      return failureCount < 2
    },
  })
}

export function useRecentPayments(bid: string, days = 7) {
  return useQuery({
    queryKey: ['recent-payments', bid, days],
    queryFn: () => paymentsApi.getRecentPayments(bid, days),
    enabled: !!bid && bid !== 'undefined',
    staleTime: 60 * 1000,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403) return false
      return failureCount < 1
    },
  })
}

export function usePaymentMethodStats(bid: string) {
  return useQuery({
    queryKey: ['payment-method-stats', bid],
    queryFn: () => paymentsApi.getPaymentMethodStats(bid),
    enabled: !!bid && bid !== 'undefined',
    staleTime: 2 * 60 * 1000,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403) return false
      return failureCount < 1
    },
  })
}

export function useMonthlyRevenue(bid: string) {
  return useQuery({
    queryKey: ['monthly-revenue', bid],
    queryFn: () => paymentsApi.getMonthlyRevenue(bid),
    enabled: !!bid,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403) return false
      return failureCount < 1
    },
  })
}

export function useRevenueStats(bid: string) {
  return useQuery({
    queryKey: ['revenue-stats', bid],
    queryFn: () => paymentsApi.getRevenueStats(bid),
    enabled: !!bid && bid !== 'undefined',
    staleTime: 60 * 1000,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403) return false
      return failureCount < 1
    },
  })
}

export function useRecordPayment(bid: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: RecordPaymentRequest) => paymentsApi.recordPayment(bid, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['payments', bid] })
      qc.invalidateQueries({ queryKey: ['business-stats', bid] })
      qc.invalidateQueries({ queryKey: ['recent-payments', bid] })
      qc.invalidateQueries({ queryKey: ['revenue-stats', bid] })
      qc.invalidateQueries({ queryKey: ['payment-method-stats', bid] })
      toast.success('Payment recorded successfully!')
    },
    onError: (err: unknown) => {
      if (isNetworkError(err)) {
        toast.error('Cannot connect to server. Check your internet connection.')
      } else {
        toast.error(getErrorMessage(err, 'Failed to record payment. Please try again.'))
      }
    },
  })
}