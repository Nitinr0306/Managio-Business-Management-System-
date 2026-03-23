import apiClient from './axios'
import type { PageResponse } from '@/lib/types/business'
import type {
  CreateStaffTaskRequest,
  StaffTask,
  TaskStatus,
  UpdateStaffTaskRequest,
} from '@/lib/types/task'

const base = (businessId: string) => `/api/v1/businesses/${businessId}/tasks`

export const tasksApi = {
  getTasks: (
    businessId: string,
    params?: {
      page?: number
      size?: number
      status?: TaskStatus
      assignedStaffId?: string
      assignedToMe?: boolean
    }
  ) =>
    apiClient.get<PageResponse<StaffTask>>(base(businessId), { params }).then((r) => r.data),

  getTask: (businessId: string, taskId: string) =>
    apiClient.get<StaffTask>(`${base(businessId)}/${taskId}`).then((r) => r.data),

  createTask: (businessId: string, data: CreateStaffTaskRequest) =>
    apiClient.post<StaffTask>(base(businessId), data).then((r) => r.data),

  updateTask: (businessId: string, taskId: string, data: UpdateStaffTaskRequest) =>
    apiClient.put<StaffTask>(`${base(businessId)}/${taskId}`, data).then((r) => r.data),

  updateStatus: (businessId: string, taskId: string, status: TaskStatus) =>
    apiClient.patch<StaffTask>(`${base(businessId)}/${taskId}/status`, { status }).then((r) => r.data),

  deleteTask: (businessId: string, taskId: string) =>
    apiClient.delete<void>(`${base(businessId)}/${taskId}`).then((r) => r.data),
}
