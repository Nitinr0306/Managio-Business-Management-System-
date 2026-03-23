export type AuditAction = string

export type AuditEntityType =
  | 'MEMBER'
  | 'STAFF'
  | 'PAYMENT'
  | 'SUBSCRIPTION'
  | 'PLAN'
  | 'BUSINESS'
  | 'USER'
  | 'TASK'

export interface AuditLog {
  id: number
  logId?: string
  businessId: number
  userId: number
  actorType?: 'USER' | 'MEMBER' | 'SYSTEM' | string
  actorPublicId?: string
  action: string
  entityType: string
  entityId?: number
  entityPublicId?: string
  details?: string
  ipAddress?: string
  userAgent?: string
  createdAt: string
}