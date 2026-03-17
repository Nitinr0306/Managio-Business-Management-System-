import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { businessApi } from '@/lib/api/business'
import { useBusinessStore } from '@/lib/store/businessStore'
import { getErrorMessage, getErrorStatus, isNetworkError, isConflictError } from '@/lib/utils/errors'
import type { CreateBusinessRequest, UpdateBusinessRequest } from '@/lib/types/business'

export function useMyBusinesses() {
  const setBusinesses = useBusinessStore((s) => s.setBusinesses)
  return useQuery({
    queryKey: ['businesses'],
    queryFn: async () => {
      const data = await businessApi.getMyBusinesses()
      setBusinesses(data)
      return data
    },
    staleTime: 2 * 60 * 1000,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403) return false
      return failureCount < 2
    },
  })
}

export function useBusiness(id: string | undefined) {
  return useQuery({
    queryKey: ['business', id],
    queryFn: () => businessApi.getBusiness(id!),
    enabled: !!id && id !== 'undefined',
    staleTime: 2 * 60 * 1000,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403 || status === 404) return false
      return failureCount < 2
    },
  })
}

export function useBusinessStats(id: string | undefined) {
  return useQuery({
    queryKey: ['business-stats', id],
    queryFn: () => businessApi.getBusinessStats(id!),
    enabled: !!id && id !== 'undefined',
    staleTime: 60 * 1000,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403 || status === 404) return false
      return failureCount < 1
    },
  })
}

export function useCreateBusiness() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateBusinessRequest) => businessApi.createBusiness(data),
    onSuccess: (biz) => {
      qc.invalidateQueries({ queryKey: ['businesses'] })
      toast.success(`"${biz.name}" created successfully!`)
    },
    onError: (err: unknown) => {
      if (isNetworkError(err)) {
        toast.error('Cannot connect to server. Check your internet connection.')
      } else if (isConflictError(err)) {
        toast.error('A business with this name already exists.')
      } else {
        toast.error(getErrorMessage(err, 'Failed to create business. Please try again.'))
      }
    },
  })
}

export function useUpdateBusiness(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: UpdateBusinessRequest) => businessApi.updateBusiness(id, data),
    onSuccess: (biz) => {
      qc.invalidateQueries({ queryKey: ['business', id] })
      qc.invalidateQueries({ queryKey: ['businesses'] })
      toast.success(`"${biz.name}" updated successfully!`)
    },
    onError: (err: unknown) => {
      if (isNetworkError(err)) {
        toast.error('Cannot connect to server. Check your internet connection.')
      } else {
        toast.error(getErrorMessage(err, 'Failed to update business. Please try again.'))
      }
    },
  })
}

export function useDeleteBusiness() {
  const qc = useQueryClient()
  const clearBiz = useBusinessStore((s) => s.clearBusinessContext)
  return useMutation({
    mutationFn: (id: string) => businessApi.deleteBusiness(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['businesses'] })
      clearBiz()
      toast.success('Business deleted successfully.')
    },
    onError: (err: unknown) => {
      if (isNetworkError(err)) {
        toast.error('Cannot connect to server. Check your internet connection.')
      } else {
        toast.error(getErrorMessage(err, 'Failed to delete business. Please try again.'))
      }
    },
  })
}