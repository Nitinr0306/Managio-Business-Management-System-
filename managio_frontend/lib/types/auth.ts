export interface User {
  id: string
  email: string
  firstName?: string
  lastName?: string
  fullName?: string
  phoneNumber?: string
  roles: string[]
  emailVerified: boolean
  enabled: boolean
  accountLocked: boolean
  accountStatus: string
  profileImageUrl?: string
  preferredLanguage?: string
  timezone?: string
  twoFactorEnabled: boolean
  lastLoginAt?: string
  createdAt: string
  updatedAt: string
}

export interface AuthTokens {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface LoginRequest {
  email: string
  password: string
  deviceId?: string
  rememberMe?: boolean
  twoFactorCode?: string
}

export interface RegisterRequest {
  email: string
  password: string
  firstName?: string
  lastName?: string
  phoneNumber?: string
  preferredLanguage?: string
  timezone?: string
}

export interface StaffLoginRequest {
  email: string
  password: string
  businessId: number
  deviceId?: string
  rememberMe?: boolean
  twoFactorCode?: string
}

export interface MemberLoginRequest {
  identifier: string
  password: string
  deviceId?: string
}

export interface MemberRegisterRequest {
  firstName: string
  lastName: string
  email?: string
  phone?: string
  password: string
  businessId: number
}

export interface StaffInfo {
  staffId: number
  businessId: number
  role: string
  roleDisplay: string
  status: string
  department?: string
  designation?: string
  employeeId?: string
  permissions: string[]
  canLogin: boolean
  canManageMembers: boolean
  canManagePayments: boolean
  canManageSubscriptions: boolean
  canViewReports: boolean
}

export interface BusinessInfo {
  id: number
  name: string
  address?: string
  phone?: string
  email?: string
}

export interface StaffLoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: User
  staff: StaffInfo
  business: BusinessInfo
  requiresTwoFactor: boolean
  lastLoginAt?: string
  message?: string
}

export interface MemberInfo {
  id: number
  businessId: number
  firstName: string
  lastName: string
  fullName: string
  phone?: string
  email?: string
  dateOfBirth?: string
  gender?: string
  status: string
  memberSince: string
}

export interface ActiveSubscriptionInfo {
  subscriptionId: number
  planName: string
  startDate: string
  endDate: string
  status: string
  daysRemaining: number
  amountPaid: number
}

export interface MemberLoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  member: MemberInfo
  activeSubscription?: ActiveSubscriptionInfo
  business: BusinessInfo
  lastLoginAt?: string
  message?: string
}

export interface JWTClaims {
  sub: string
  email: string
  userId?: number
  roles?: string[]
  staffId?: number
  businessId?: number
  staffRole?: string
  memberId?: number
  userType?: 'owner' | 'staff' | 'member'
  type: string
  exp: number
  iat: number
}