export type AuditAction = string

export type AuditEntityType =
  | 'MEMBER'
  | 'STAFF'
  | 'PAYMENT'
  | 'SUBSCRIPTION'
  | 'PLAN'
  | 'BUSINESS'
  | 'USER'

export interface AuditLog {
  id: number
  businessId: number
  userId: number
  action: string
  entityType: string
  entityId?: number
  details?: string
  createdAt: string
}