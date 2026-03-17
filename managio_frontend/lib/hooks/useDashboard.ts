import { useQuery } from '@tanstack/react-query'
import { dashboardApi } from '@/lib/api/dashboard'
import { getErrorStatus } from '@/lib/utils/errors'

export function useOwnerDashboard(businessId: string) {
  return useQuery({
    queryKey: ['owner-dashboard', businessId],
    queryFn: () => dashboardApi.getOwnerDashboard(businessId),
    enabled: !!businessId && businessId !== 'undefined',
    staleTime: 30 * 1000,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403 || status === 404) return false
      return failureCount < 1
    },
  })
}

export function useStaffDashboard(businessId: string) {
  return useQuery({
    queryKey: ['staff-dashboard', businessId],
    queryFn: () => dashboardApi.getStaffDashboard(businessId),
    enabled: !!businessId && businessId !== 'undefined',
    staleTime: 30 * 1000,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403 || status === 404) return false
      return failureCount < 1
    },
  })
}

export function useMemberDashboard(memberId: string) {
  return useQuery({
    queryKey: ['member-dashboard', memberId],
    queryFn: () => dashboardApi.getMemberDashboard(memberId),
    enabled: !!memberId && memberId !== 'undefined',
    staleTime: 30 * 1000,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403 || status === 404) return false
      return failureCount < 1
    },
  })
}

