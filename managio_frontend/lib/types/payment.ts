export type PaymentMethod =
  | 'CASH' | 'UPI' | 'NETBANKING' | 'DEBIT_CARD' | 'CREDIT_CARD'
  | 'PAYTM' | 'PHONEPE' | 'GOOGLEPAY' | 'RAZORPAY' | 'STRIPE'
  | 'CHEQUE' | 'BANK_TRANSFER' | 'CARD' | 'OTHER'

export interface Payment {
  id: string
  memberId: string
  memberName: string
  memberPhone?: string
  planName?: string
  subscriptionId?: string
  amount: number
  paymentMethod: PaymentMethod
  paymentMethodDisplay?: string
  notes?: string
  recordedBy?: string | number
  createdAt: string
}

export interface RecordPaymentRequest {
  memberId: number
  subscriptionId?: number
  amount: number
  paymentMethod: PaymentMethod
  referenceNumber?: string
  notes?: string
  paidAt?: string
}

export interface PaymentMethodStat {
  method: string
  total: number
  count: number
  percentage: number
}

export interface RevenueStats {
  totalRevenue: number
  monthlyRevenue: number
  todayRevenue: number
  averagePerMember: number
  revenueByMonth: { month: string; revenue: number }[]
}

export const PAYMENT_METHOD_LABELS: Record<string, string> = {
  CASH: 'Cash',
  UPI: 'UPI',
  NETBANKING: 'Net Banking',
  DEBIT_CARD: 'Debit Card',
  CREDIT_CARD: 'Credit Card',
  PAYTM: 'Paytm',
  PHONEPE: 'PhonePe',
  GOOGLEPAY: 'Google Pay',
  RAZORPAY: 'Razorpay',
  STRIPE: 'Stripe',
  CHEQUE: 'Cheque',
  BANK_TRANSFER: 'Bank Transfer',
  CARD: 'Card',
  OTHER: 'Other',
}