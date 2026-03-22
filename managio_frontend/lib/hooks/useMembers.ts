import { useMutation, useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query'
import { toast } from 'sonner'
import { membersApi } from '@/lib/api/members'
import { getErrorMessage, getErrorStatus, isNetworkError, isConflictError } from '@/lib/utils/errors'
import type { CreateMemberRequest, UpdateMemberRequest } from '@/lib/types/member'

export function useMembers(
  businessId: string,
  params?: { page?: number; size?: number; search?: string; status?: string }
) {
  return useQuery({
    queryKey: ['members', businessId, params],
    queryFn: () => membersApi.getMembers(businessId, params),
    enabled: !!businessId && businessId !== 'undefined',
    placeholderData: keepPreviousData,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403) return false
      return failureCount < 2
    },
  })
}

export function useMember(businessId: string, memberId: string) {
  return useQuery({
    queryKey: ['member', businessId, memberId],
    queryFn: () => membersApi.getMember(businessId, memberId),
    enabled: !!businessId && !!memberId,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403 || status === 404) return false
      return failureCount < 2
    },
  })
}

export function useMemberProfile(businessId: string, memberId: string) {
  return useQuery({
    queryKey: ['member-profile', businessId, memberId],
    queryFn: () => membersApi.getMemberProfile(businessId, memberId),
    enabled: !!businessId && !!memberId,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403 || status === 404) return false
      return failureCount < 2
    },
  })
}

export function useMemberStats(businessId: string, memberId: string) {
  return useQuery({
    queryKey: ['member-stats', businessId, memberId],
    queryFn: () => membersApi.getMemberStats(businessId, memberId),
    enabled: !!businessId && !!memberId,
    staleTime: 2 * 60 * 1000,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403 || status === 404) return false
      return failureCount < 1
    },
  })
}

export function useMemberSubscriptions(businessId: string, memberId: string) {
  return useQuery({
    queryKey: ['member-subscriptions', businessId, memberId],
    queryFn: () => membersApi.getMemberSubscriptionHistory(businessId, memberId),
    enabled: !!businessId && !!memberId && memberId !== '',
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403 || status === 404) return false
      return failureCount < 1
    },
  })
}

export function useMemberPayments(businessId: string, memberId: string) {
  return useQuery({
    queryKey: ['member-payments', businessId, memberId],
    queryFn: () => membersApi.getMemberPaymentHistory(businessId, memberId),
    enabled: !!businessId && !!memberId && memberId !== '',
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403 || status === 404) return false
      return failureCount < 1
    },
  })
}

export function useCreateMember(businessId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateMemberRequest) => membersApi.createMember(businessId, data),
    onSuccess: (member) => {
      qc.invalidateQueries({ queryKey: ['members', businessId] })
      qc.invalidateQueries({ queryKey: ['business-stats', businessId] })
      toast.success(`${member.fullName} added successfully!`)
    },
    onError: (err: unknown) => {
      if (isNetworkError(err)) {
        toast.error('Cannot connect to server. Check your internet connection.')
      } else if (isConflictError(err)) {
        toast.error('A member with this phone/email already exists in this business.')
      } else {
        toast.error(getErrorMessage(err, 'Failed to add member. Please try again.'))
      }
    },
  })
}

export function useUpdateMember(businessId: string, memberId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: UpdateMemberRequest) =>
      membersApi.updateMember(businessId, memberId, data),
    onSuccess: (member) => {
      qc.invalidateQueries({ queryKey: ['member', businessId, memberId] })
      qc.invalidateQueries({ queryKey: ['member-profile', businessId, memberId] })
      qc.invalidateQueries({ queryKey: ['members', businessId] })
      toast.success(`${member.fullName} updated successfully!`)
    },
    onError: (err: unknown) => {
      if (isNetworkError(err)) {
        toast.error('Cannot connect to server. Check your internet connection.')
      } else {
        toast.error(getErrorMessage(err, 'Failed to update member. Please try again.'))
      }
    },
  })
}

export function useDeactivateMember(businessId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (memberId: string) => membersApi.deactivateMember(businessId, memberId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['members', businessId] })
      qc.invalidateQueries({ queryKey: ['business-stats', businessId] })
      toast.success('Member deactivated successfully.')
    },
    onError: (err: unknown) => {
      if (isNetworkError(err)) {
        toast.error('Cannot connect to server. Check your internet connection.')
      } else {
        toast.error(getErrorMessage(err, 'Failed to deactivate member. Please try again.'))
      }
    },
  })
}

export function useDisableMemberPortal(businessId: string, memberId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => membersApi.disableMemberPortal(memberId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['member', businessId, memberId] })
      qc.invalidateQueries({ queryKey: ['members', businessId] })
      toast.success('Member portal access disabled.')
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err, 'Failed to disable member portal.')),
  })
}

export function useEnableMemberPortal(businessId: string, memberId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => membersApi.enableMemberPortal(memberId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['member', businessId, memberId] })
      qc.invalidateQueries({ queryKey: ['members', businessId] })
      toast.success('Member portal access enabled.')
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err, 'Failed to enable member portal.')),
  })
}