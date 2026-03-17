'use client'

import Link from 'next/link'
import { ArrowLeft, Receipt, CreditCard } from 'lucide-react'
import { useAuthStore } from '@/lib/store/authStore'
import { useMemberPayments } from '@/lib/hooks/useMembers'
import { LoadingSpinner, EmptyState } from '@/components/shared/EmptyState'
import { formatCurrency, formatRelative } from '@/lib/utils/formatters'
import { cn } from '@/lib/utils/cn'

export default function MemberPaymentsPage() {
  const memberId = useAuthStore((s) => (s.userType === 'member' ? String(s.user?.id ?? '') : ''))
  const businessId = useAuthStore((s) => String(s.businessContext?.id ?? ''))
  const { data, isLoading } = useMemberPayments(businessId, memberId)

  if (isLoading) return <LoadingSpinner />

  return (
    <div className="max-w-3xl mx-auto px-6 py-10">
      <div className="flex items-center justify-between mb-6">
        <Link
          href="/member/dashboard"
          className="inline-flex items-center gap-2 text-sm text-white/50 hover:text-white/80 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" /> Back to dashboard
        </Link>
        <Link
          href="/member/subscription"
          className="inline-flex items-center gap-2 text-sm text-emerald-400 hover:text-emerald-300 transition-colors"
        >
          <CreditCard className="w-4 h-4" /> Subscription
        </Link>
      </div>

      <div className="p-6 rounded-2xl border border-white/6 bg-white/[0.02] mb-6">
        <h1 className="text-2xl font-display font-700 flex items-center gap-2">
          <Receipt className="w-6 h-6 text-emerald-400" /> Payments
        </h1>
        <p className="text-sm text-white/45 mt-1">Your payment history for this gym.</p>
      </div>

      {!data?.length ? (
        <EmptyState
          icon={Receipt}
          title="No payments yet"
          description="Payments recorded by your gym will appear here."
        />
      ) : (
        <div className="space-y-3">
          {data.map((p) => (
            <div
              key={p.id}
              className="flex items-center justify-between p-4 rounded-2xl border border-white/6 bg-white/[0.02]"
            >
              <div className="min-w-0">
                <div className="text-sm font-medium text-white/80 truncate">
                  {p.planName || 'Payment'}
                </div>
                <div className="text-xs text-white/40 mt-0.5">
                  {formatRelative(p.createdAt)} • {p.paymentMethod}
                </div>
              </div>
              <div className="text-right flex-shrink-0">
                <div className="text-sm font-display font-700 text-emerald-400">
                  {formatCurrency(p.amount)}
                </div>
                <div
                  className={cn(
                    'text-[10px] inline-block mt-1 px-2 py-0.5 rounded-full',
                    'bg-white/5 text-white/40'
                  )}
                >
                  RECORDED
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

