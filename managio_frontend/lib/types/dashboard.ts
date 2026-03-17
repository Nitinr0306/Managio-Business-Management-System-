export interface OwnerDashboardResponse {
  totalMembers: number
  activeMembers: number
  inactiveMembers: number
  newMembersThisMonth: number
  activeSubscriptions: number
  expiringIn7Days: number
  expiringIn30Days: number
  upcomingExpirations: {
    memberId: number
    memberName: string
    planName: string
    endDate: string
    daysRemaining: number
    phone?: string
    email?: string
  }[]
  totalRevenue: number
  monthlyRevenue: number
  todayRevenue: number
  averageRevenuePerMember: number
  totalPayments: number
  paymentsThisMonth: number
  recentPayments: {
    paymentId: number
    memberName: string
    amount: number
    paymentMethod: string
    paidAt: string
  }[]
  paymentMethodStats: {
    totalRevenue: number
    totalPayments: number
    byPaymentMethod: {
      method: string
      methodDisplay: string
      count: number
      totalAmount: number
      percentage: number
    }[]
    averagePaymentAmount: number
  }
  memberGrowth: {
    thisMonth: number
    lastMonth: number
    growthPercentage: number
    monthlyTrend: any[]
  }
  revenueGrowth: {
    thisMonth: number
    lastMonth: number
    growthPercentage: number
    monthlyTrend: any[]
  }
}

export interface StaffDashboardResponse {
  membersAddedToday: number
  totalActiveMembers: number
  expiringThisWeek: {
    memberId: number
    memberName: string
    planName: string
    endDate: string
    daysRemaining: number
  }[]
  recentPayments: {
    paymentId: number
    memberName: string
    amount: number
    paymentMethod: string
    paidAt: string
  }[]
  taskReminders: {
    title: string
    description: string
    priority: string
  }[]
}

export interface MemberDashboardResponse {
  memberName: string
  planName?: string
  subscriptionEndDate?: string
  daysRemaining?: number
  status: string
  paymentHistory: {
    date: string
    amount: number
    method: string
  }[]
  totalPaid: number
  membershipStats: {
    totalSubscriptions: number
    activeSubscriptions: number
    completedSubscriptions: number
  }
}

