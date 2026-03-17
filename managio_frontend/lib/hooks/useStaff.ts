import { useMutation, useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query'
import { toast } from 'sonner'
import { staffApi } from '@/lib/api/staff'
import { getErrorMessage, getErrorStatus, isNetworkError, isConflictError, isNotFoundError } from '@/lib/utils/errors'
import type { CreateStaffRequest, InviteStaffRequest, UpdateStaffRequest } from '@/lib/types/staff'

export function useStaff(
  bid: string,
  params?: { page?: number; size?: number; search?: string; status?: string }
) {
  return useQuery({
    queryKey: ['staff', bid, params],
    queryFn: () => staffApi.getStaff(bid, params),
    enabled: !!bid && bid !== 'undefined',
    placeholderData: keepPreviousData,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403) return false
      return failureCount < 2
    },
  })
}

export function useStaffMember(bid: string, staffId: string) {
  return useQuery({
    queryKey: ['staff-member', bid, staffId],
    queryFn: () => staffApi.getStaffMember(bid, staffId),
    enabled: !!bid && !!staffId,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403 || status === 404) return false
      return failureCount < 2
    },
  })
}

export function useStaffDetail(bid: string, staffId: string) {
  return useQuery({
    queryKey: ['staff-detail', bid, staffId],
    queryFn: () => staffApi.getStaffDetail(bid, staffId),
    enabled: !!bid && !!staffId,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403 || status === 404) return false
      return failureCount < 1
    },
  })
}

export function useStaffInvitations(bid: string, params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: ['staff-invitations', bid, params],
    queryFn: () => staffApi.getInvitations(bid, params),
    enabled: !!bid,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403) return false
      return failureCount < 2
    },
  })
}

export function usePendingInvitations(bid: string) {
  return useQuery({
    queryKey: ['pending-invitations', bid],
    queryFn: () => staffApi.getPendingInvitations(bid),
    enabled: !!bid,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403) return false
      return failureCount < 1
    },
  })
}

export function useCreateStaff(bid: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateStaffRequest) => staffApi.createStaff(bid, data),
    onSuccess: (staff) => {
      qc.invalidateQueries({ queryKey: ['staff', bid] })
      qc.invalidateQueries({ queryKey: ['business-stats', bid] })
      toast.success(`${staff.userName} added to your team!`)
    },
    onError: (err: unknown) => {
      if (isNetworkError(err)) {
        toast.error('Cannot connect to server. Check your internet connection.')
      } else if (isNotFoundError(err)) {
        toast.error('No user found with that email. They must register on Managio first.')
      } else if (isConflictError(err)) {
        toast.error('This person is already a staff member of this business.')
      } else {
        toast.error(getErrorMessage(err, 'Failed to add staff member. Please try again.'))
      }
    },
  })
}

export function useInviteStaff(bid: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: InviteStaffRequest) => staffApi.inviteStaff(bid, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['staff-invitations', bid] })
      qc.invalidateQueries({ queryKey: ['pending-invitations', bid] })
      toast.success('Invitation sent! They will receive an email to join your team.')
    },
    onError: (err: unknown) => {
      if (isNetworkError(err)) {
        toast.error('Cannot connect to server. Check your internet connection.')
      } else if (isConflictError(err)) {
        toast.error('An invitation has already been sent to this email address.')
      } else {
        toast.error(getErrorMessage(err, 'Failed to send invitation. Please try again.'))
      }
    },
  })
}

export function useUpdateStaff(bid: string, staffId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: UpdateStaffRequest) => staffApi.updateStaff(bid, staffId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['staff-member', bid, staffId] })
      qc.invalidateQueries({ queryKey: ['staff', bid] })
      toast.success('Staff member updated successfully!')
    },
    onError: (err: unknown) => {
      toast.error(getErrorMessage(err, 'Failed to update staff member. Please try again.'))
    },
  })
}

export function useTerminateStaff(bid: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({
      staffId,
      terminationDate,
    }: {
      staffId: string
      terminationDate?: string
    }) => staffApi.terminateStaff(bid, staffId, terminationDate),
    onMutate: async ({ staffId }) => {
      await qc.cancelQueries({ queryKey: ['staff', bid] })
      await qc.cancelQueries({ queryKey: ['staff-member', bid, staffId] })
      await qc.cancelQueries({ queryKey: ['staff-detail', bid, staffId] })

      const prevStaffQueries = qc.getQueriesData({ queryKey: ['staff', bid] })
      const prevMember = qc.getQueryData(['staff-member', bid, staffId])
      const prevDetail = qc.getQueryData(['staff-detail', bid, staffId])

      // Patch all staff list caches (any params variant)
      for (const [key, data] of prevStaffQueries) {
        if (!data) continue
        // PageResponse shape: { content, ... }
        const page: any = data as any
        if (!Array.isArray(page.content)) continue
        qc.setQueryData(key, {
          ...page,
          content: page.content.map((s: any) => (String(s.id) === String(staffId) ? { ...s, status: 'TERMINATED' } : s)),
        })
      }

      if (prevMember) qc.setQueryData(['staff-member', bid, staffId], { ...(prevMember as any), status: 'TERMINATED' })
      if (prevDetail) qc.setQueryData(['staff-detail', bid, staffId], { ...(prevDetail as any), status: 'TERMINATED' })

      return { prevStaffQueries, prevMember, prevDetail }
    },
    onError: (err: unknown, { staffId }, ctx) => {
      if (ctx?.prevStaffQueries) {
        for (const [key, data] of ctx.prevStaffQueries) qc.setQueryData(key, data)
      }
      if (ctx?.prevMember) qc.setQueryData(['staff-member', bid, staffId], ctx.prevMember as any)
      if (ctx?.prevDetail) qc.setQueryData(['staff-detail', bid, staffId], ctx.prevDetail as any)

      if (isNetworkError(err)) toast.error('Cannot connect to server. Check your internet connection.')
      else toast.error(getErrorMessage(err, 'Failed to terminate staff member. Please try again.'))
    },
    onSuccess: (_data, { staffId }) => {
      qc.invalidateQueries({ queryKey: ['staff', bid] })
      qc.invalidateQueries({ queryKey: ['staff-member', bid, staffId] })
      qc.invalidateQueries({ queryKey: ['staff-detail', bid, staffId] })
      qc.invalidateQueries({ queryKey: ['business-stats', bid] })
      toast.success('Staff member terminated successfully.')
    },
  })
}

export function useSuspendStaff(bid: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (staffId: string) => staffApi.suspendStaff(bid, staffId),
    onMutate: async (staffId: string) => {
      await qc.cancelQueries({ queryKey: ['staff', bid] })
      await qc.cancelQueries({ queryKey: ['staff-member', bid, staffId] })
      await qc.cancelQueries({ queryKey: ['staff-detail', bid, staffId] })

      const prevStaffQueries = qc.getQueriesData({ queryKey: ['staff', bid] })
      const prevMember = qc.getQueryData(['staff-member', bid, staffId])
      const prevDetail = qc.getQueryData(['staff-detail', bid, staffId])

      for (const [key, data] of prevStaffQueries) {
        if (!data) continue
        const page: any = data as any
        if (!Array.isArray(page.content)) continue
        qc.setQueryData(key, {
          ...page,
          content: page.content.map((s: any) => (String(s.id) === String(staffId) ? { ...s, status: 'SUSPENDED' } : s)),
        })
      }

      if (prevMember) qc.setQueryData(['staff-member', bid, staffId], { ...(prevMember as any), status: 'SUSPENDED' })
      if (prevDetail) qc.setQueryData(['staff-detail', bid, staffId], { ...(prevDetail as any), status: 'SUSPENDED' })

      return { prevStaffQueries, prevMember, prevDetail }
    },
    onError: (err: unknown, staffId, ctx) => {
      if (ctx?.prevStaffQueries) for (const [key, data] of ctx.prevStaffQueries) qc.setQueryData(key, data)
      if (ctx?.prevMember) qc.setQueryData(['staff-member', bid, staffId], ctx.prevMember as any)
      if (ctx?.prevDetail) qc.setQueryData(['staff-detail', bid, staffId], ctx.prevDetail as any)
      toast.error(getErrorMessage(err, 'Failed to suspend staff member. Please try again.'))
    },
    onSuccess: (_data, staffId) => {
      qc.invalidateQueries({ queryKey: ['staff', bid] })
      qc.invalidateQueries({ queryKey: ['staff-member', bid, staffId] })
      qc.invalidateQueries({ queryKey: ['staff-detail', bid, staffId] })
      toast.success('Staff member suspended.')
    },
  })
}

export function useActivateStaff(bid: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (staffId: string) => staffApi.activateStaff(bid, staffId),
    onMutate: async (staffId: string) => {
      await qc.cancelQueries({ queryKey: ['staff', bid] })
      await qc.cancelQueries({ queryKey: ['staff-member', bid, staffId] })
      await qc.cancelQueries({ queryKey: ['staff-detail', bid, staffId] })

      const prevStaffQueries = qc.getQueriesData({ queryKey: ['staff', bid] })
      const prevMember = qc.getQueryData(['staff-member', bid, staffId])
      const prevDetail = qc.getQueryData(['staff-detail', bid, staffId])

      for (const [key, data] of prevStaffQueries) {
        if (!data) continue
        const page: any = data as any
        if (!Array.isArray(page.content)) continue
        qc.setQueryData(key, {
          ...page,
          content: page.content.map((s: any) => (String(s.id) === String(staffId) ? { ...s, status: 'ACTIVE' } : s)),
        })
      }

      if (prevMember) qc.setQueryData(['staff-member', bid, staffId], { ...(prevMember as any), status: 'ACTIVE' })
      if (prevDetail) qc.setQueryData(['staff-detail', bid, staffId], { ...(prevDetail as any), status: 'ACTIVE' })

      return { prevStaffQueries, prevMember, prevDetail }
    },
    onError: (err: unknown, staffId, ctx) => {
      if (ctx?.prevStaffQueries) for (const [key, data] of ctx.prevStaffQueries) qc.setQueryData(key, data)
      if (ctx?.prevMember) qc.setQueryData(['staff-member', bid, staffId], ctx.prevMember as any)
      if (ctx?.prevDetail) qc.setQueryData(['staff-detail', bid, staffId], ctx.prevDetail as any)
      toast.error(getErrorMessage(err, 'Failed to activate staff member. Please try again.'))
    },
    onSuccess: (_data, staffId) => {
      qc.invalidateQueries({ queryKey: ['staff', bid] })
      qc.invalidateQueries({ queryKey: ['staff-member', bid, staffId] })
      qc.invalidateQueries({ queryKey: ['staff-detail', bid, staffId] })
      toast.success('Staff member activated successfully.')
    },
  })
}