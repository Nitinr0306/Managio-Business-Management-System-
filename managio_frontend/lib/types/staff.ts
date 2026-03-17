export type StaffRole = 'OWNER' | 'MANAGER' | 'TRAINER' | 'RECEPTIONIST' | 'ACCOUNTANT' | 'SALES' | 'STAFF'
export type StaffStatus = 'ACTIVE' | 'ON_LEAVE' | 'SUSPENDED' | 'TERMINATED'

export type Permission =
  | 'VIEW_MEMBERS' | 'ADD_MEMBERS' | 'EDIT_MEMBERS' | 'DELETE_MEMBERS' | 'EDIT_MEMBER_NOTES'
  | 'IMPORT_MEMBERS' | 'EXPORT_MEMBERS'
  | 'VIEW_SUBSCRIPTIONS' | 'ASSIGN_SUBSCRIPTIONS' | 'CANCEL_SUBSCRIPTIONS'
  | 'EXTEND_SUBSCRIPTIONS' | 'VIEW_SUBSCRIPTION_HISTORY'
  | 'VIEW_PAYMENTS' | 'RECORD_PAYMENTS' | 'REFUND_PAYMENTS'
  | 'VIEW_PAYMENT_HISTORY' | 'EXPORT_PAYMENTS'
  | 'VIEW_REPORTS' | 'VIEW_DASHBOARD' | 'EXPORT_DATA' | 'VIEW_BUSINESS_STATS'
  | 'VIEW_STAFF' | 'ADD_STAFF' | 'EDIT_STAFF' | 'REMOVE_STAFF' | 'VIEW_AUDIT_LOGS'
  | 'RECORD_ATTENDANCE' | 'VIEW_ATTENDANCE'
  | 'MANAGE_BUSINESS_SETTINGS' | 'MANAGE_SUBSCRIPTION_PLANS' | 'ACCESS_API'
  | 'ALL_PERMISSIONS'

export interface Staff {
  id: string
  businessId: string
  userId: string
  userEmail: string
  userName: string
  role: StaffRole
  roleDisplay: string
  status: StaffStatus
  statusDisplay: string
  hireDate?: string
  terminationDate?: string
  department?: string
  designation?: string
  salary?: number
  employeeId?: string
  phone?: string
  email?: string
  address?: string
  notes?: string
  canLogin: boolean
  canManageMembers: boolean
  canManagePayments: boolean
  canManageSubscriptions: boolean
  canViewReports: boolean
  createdAt: string
  updatedAt: string
}

export interface StaffDetail extends Staff {
  businessName?: string
  emergencyContact?: string
  rolePermissions: Permission[]
  grantedPermissions: Permission[]
  revokedPermissions: Permission[]
  effectivePermissions: Permission[]
}

export interface StaffInvitation {
  id: string
  businessId: string
  businessName?: string
  email: string
  role: StaffRole
  roleDisplay?: string
  token: string
  expiresAt: string
  used: boolean
  usedAt?: string
  acceptedByUserId?: string
  acceptedByUserEmail?: string
  invitedBy: string
  invitedByUserEmail?: string
  message?: string
  department?: string
  designation?: string
  createdAt: string
  expired?: boolean
  valid?: boolean
}

export interface CreateStaffRequest {
  email: string
  role: StaffRole
  department?: string
  designation?: string
  hireDate?: string
  phone?: string
  salary?: number
  employeeId?: string
  notes?: string
  canLogin?: boolean
  canManageMembers?: boolean
  canManagePayments?: boolean
  canManageSubscriptions?: boolean
  canViewReports?: boolean
}

export interface InviteStaffRequest {
  email: string
  role: StaffRole
  message?: string
  department?: string
  designation?: string
}

export interface UpdateStaffRequest {
  role?: StaffRole
  status?: StaffStatus
  department?: string
  designation?: string
  phone?: string
  salary?: number
  notes?: string
  canLogin?: boolean
  canManageMembers?: boolean
  canManagePayments?: boolean
  canManageSubscriptions?: boolean
  canViewReports?: boolean
}

export interface AcceptInvitationRequest {
  token: string
  firstName: string
  lastName: string
  password: string
  phoneNumber?: string
  hireDate?: string
  address?: string
  emergencyContact?: string
}

export const STAFF_ROLE_COLORS: Record<StaffRole, string> = {
  OWNER: 'bg-violet-500/15 text-violet-400',
  MANAGER: 'bg-indigo-500/15 text-indigo-400',
  TRAINER: 'bg-emerald-500/15 text-emerald-400',
  RECEPTIONIST: 'bg-amber-500/15 text-amber-400',
  ACCOUNTANT: 'bg-cyan-500/15 text-cyan-400',
  SALES: 'bg-pink-500/15 text-pink-400',
  STAFF: 'bg-white/5 text-white/50',
}

export const STAFF_STATUS_COLORS: Record<StaffStatus, string> = {
  ACTIVE: 'bg-emerald-500/15 text-emerald-400',
  ON_LEAVE: 'bg-blue-500/15 text-blue-400',
  SUSPENDED: 'bg-amber-500/15 text-amber-400',
  TERMINATED: 'bg-red-500/15 text-red-400',
}