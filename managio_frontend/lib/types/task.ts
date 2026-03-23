export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export interface StaffTask {
  id: number
  publicId: string
  businessId: number
  title: string
  description?: string
  status: TaskStatus
  priority: TaskPriority
  dueDate?: string
  assignedStaffId?: number
  assignedStaffPublicId?: string
  assignedStaffName?: string
  createdByUserId: number
  createdByUserPublicId?: string
  createdByUserName?: string
  completedAt?: string
  createdAt: string
  updatedAt: string
}

export interface CreateStaffTaskRequest {
  title: string
  description?: string
  priority?: TaskPriority
  dueDate?: string
  assignedStaffId?: string
}

export interface UpdateStaffTaskRequest {
  title?: string
  description?: string
  priority?: TaskPriority
  dueDate?: string
  assignedStaffId?: string
}

export interface UpdateTaskStatusRequest {
  status: TaskStatus
}
