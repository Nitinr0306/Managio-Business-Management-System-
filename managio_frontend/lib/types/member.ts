export type MemberStatus = 'ACTIVE' | 'INACTIVE'
export type MemberGender = 'MALE' | 'FEMALE' | 'OTHER'

export interface Member {
  id: string
  businessId: string
  firstName: string
  lastName: string
  fullName: string
  phone?: string
  email?: string
  dateOfBirth?: string
  gender?: MemberGender
  address?: string
  emergencyContactName?: string
  emergencyContactPhone?: string
  notes?: string
  status: MemberStatus
  createdAt: string
  updatedAt: string
}

export interface MemberStats {
  totalSubscriptions: number
  activeSubscriptions: number
  completedSubscriptions: number
  totalAmountPaid: number
  memberSince: string
}

export interface CreateMemberRequest {
  firstName: string
  lastName: string
  phone?: string
  email?: string
  dateOfBirth?: string
  gender?: MemberGender
  address?: string
  emergencyContactName?: string
  emergencyContactPhone?: string
  notes?: string
}

export interface UpdateMemberRequest extends Partial<CreateMemberRequest> {}