export type BusinessStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED'
export type BusinessType =
  | 'GYM' | 'FITNESS_STUDIO' | 'YOGA_STUDIO' | 'DANCE_STUDIO'
  | 'MARTIAL_ARTS' | 'SWIMMING_POOL' | 'SALON' | 'SPA'
  | 'RESTAURANT' | 'RETAIL' | 'OTHER'

export interface Business {
  id: string
  publicId?: string
  ownerId: string
  ownerPublicId?: string
  name: string
  description?: string
  type: BusinessType
  address?: string
  city?: string
  state?: string
  country?: string
  phone?: string
  email?: string
  status: BusinessStatus
  memberCount: number
  staffCount: number
  createdAt: string
  updatedAt: string
}

export interface BusinessStats {
  totalMembers: number
  activeMembers: number
  inactiveMembers: number
  newMembersThisMonth: number
  activeSubscriptions: number
  expiredSubscriptions: number
  expiringIn7Days: number
  expiringIn30Days: number
  totalRevenue: number
  monthlyRevenue: number
  todayRevenue: number
  averageRevenuePerMember: number
  totalPayments: number
  paymentsThisMonth: number
  totalStaff?: number
}

export interface CreateBusinessRequest {
  name: string
  description?: string
  type: BusinessType
  address?: string
  city?: string
  state?: string
  country?: string
  phone?: string
  email?: string
}

export interface UpdateBusinessRequest extends Partial<CreateBusinessRequest> {}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty?: boolean
  numberOfElements?: number
}