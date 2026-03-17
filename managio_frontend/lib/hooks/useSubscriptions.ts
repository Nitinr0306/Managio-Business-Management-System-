import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { subscriptionsApi } from '@/lib/api/subscriptions'
import { getErrorMessage, getErrorStatus, isNetworkError } from '@/lib/utils/errors'
import type { CreatePlanRequest, AssignSubscriptionRequest } from '@/lib/types/subscription'

export function usePlans(businessId: string) {
  return useQuery({
    queryKey: ['plans', businessId],
    queryFn: () => subscriptionsApi.getPlans(businessId),
    enabled: !!businessId,
    staleTime: 2 * 60 * 1000,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403) return false
      return failureCount < 2
    },
  })
}

export function useSubscriptions(
  businessId: string,
  params?: { page?: number; size?: number; status?: string }
) {
  return useQuery({
    queryKey: ['subscriptions', businessId, params],
    queryFn: () => subscriptionsApi.getSubscriptions(businessId, params),
    enabled: !!businessId,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403) return false
      return failureCount < 2
    },
  })
}

export function useExpiringSubscriptions(businessId: string, days = 7) {
  return useQuery({
    queryKey: ['expiring-subscriptions', businessId, days],
    queryFn: () => subscriptionsApi.getExpiringSubscriptions(businessId, days),
    enabled: !!businessId && businessId !== 'undefined',
    staleTime: 2 * 60 * 1000,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403) return false
      return failureCount < 1
    },
  })
}

export function useCreatePlan(businessId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreatePlanRequest) => subscriptionsApi.createPlan(businessId, data),
    onSuccess: (plan) => {
      qc.invalidateQueries({ queryKey: ['plans', businessId] })
      toast.success(`Plan "${plan.name}" created successfully!`)
    },
    onError: (err: unknown) => {
      if (isNetworkError(err)) {
        toast.error('Cannot connect to server. Check your internet connection.')
      } else {
        toast.error(getErrorMessage(err, 'Failed to create plan. Please try again.'))
      }
    },
  })
}

export function useAssignSubscription(businessId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: AssignSubscriptionRequest) =>
      subscriptionsApi.assignSubscription(businessId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['subscriptions', businessId] })
      qc.invalidateQueries({ queryKey: ['business-stats', businessId] })
      qc.invalidateQueries({ queryKey: ['expiring-subscriptions', businessId] })
      toast.success('Subscription assigned successfully!')
    },
    onError: (err: unknown) => {
      if (isNetworkError(err)) {
        toast.error('Cannot connect to server. Check your internet connection.')
      } else {
        const msg = getErrorMessage(err)
        if (msg?.toLowerCase().includes('already has an active')) {
          toast.error('This member already has an active subscription.')
        } else {
          toast.error(msg || 'Failed to assign subscription. Please try again.')
        }
      }
    },
  })
}

export function useActiveSubscriptionCount(businessId: string) {
  return useQuery({
    queryKey: ['active-subscription-count', businessId],
    queryFn: () => subscriptionsApi.countActiveSubscriptions(businessId),
    enabled: !!businessId,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403) return false
      return failureCount < 1
    },
  })
}