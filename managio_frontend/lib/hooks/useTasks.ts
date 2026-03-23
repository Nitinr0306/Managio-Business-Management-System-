import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { tasksApi } from '@/lib/api/tasks'
import type {
  CreateStaffTaskRequest,
  TaskStatus,
  UpdateStaffTaskRequest,
} from '@/lib/types/task'
import { getErrorMessage } from '@/lib/utils/errors'

export function useTasks(
  businessId: string,
  params?: {
    page?: number
    size?: number
    status?: TaskStatus
    assignedStaffId?: string
    assignedToMe?: boolean
  }
) {
  return useQuery({
    queryKey: ['tasks', businessId, params],
    queryFn: () => tasksApi.getTasks(businessId, params),
    enabled: !!businessId,
  })
}

export function useCreateTask(businessId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateStaffTaskRequest) => tasksApi.createTask(businessId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tasks', businessId] })
      toast.success('Task created')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Failed to create task')),
  })
}

export function useUpdateTaskStatus(businessId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ taskId, status }: { taskId: string; status: TaskStatus }) =>
      tasksApi.updateStatus(businessId, taskId, status),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tasks', businessId] })
      toast.success('Task updated')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Failed to update task')),
  })
}

export function useUpdateTask(businessId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ taskId, data }: { taskId: string; data: UpdateStaffTaskRequest }) =>
      tasksApi.updateTask(businessId, taskId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tasks', businessId] })
      toast.success('Task updated')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Failed to update task')),
  })
}

export function useDeleteTask(businessId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (taskId: string) => tasksApi.deleteTask(businessId, taskId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tasks', businessId] })
      toast.success('Task deleted')
    },
    onError: (err) => toast.error(getErrorMessage(err, 'Failed to delete task')),
  })
}
