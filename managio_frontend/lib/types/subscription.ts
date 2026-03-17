export type SubscriptionStatus = 'ACTIVE' | 'EXPIRED' | 'CANCELLED' | 'PENDING'

export interface SubscriptionPlan {
  id: string
  businessId: string
  name: string
  description?: string
  price: number
  durationDays: number
  isActive: boolean
  createdAt: string
}

export interface MemberSubscription {
  id: number | string
  memberId: number | string
  memberName?: string
  memberEmail?: string
  memberPhone?: string
  planId: number | string
  planName?: string
  startDate: string
  endDate: string
  status: SubscriptionStatus
  amount?: number
  daysRemaining?: number
  createdAt: string
}

export interface CreatePlanRequest {
  name: string
  description?: string
  price: number
  durationDays: number
}

export interface UpdatePlanRequest extends Partial<CreatePlanRequest> {
  isActive?: boolean
}

export interface AssignSubscriptionRequest {
  memberId: number
  planId: number
  startDate?: string
}

export interface ExpiringSubscription {
  subscriptionId: string
  memberId: string
  memberName: string
  memberEmail?: string
  memberPhone?: string
  planName: string
  endDate: string
  daysRemaining: number
}