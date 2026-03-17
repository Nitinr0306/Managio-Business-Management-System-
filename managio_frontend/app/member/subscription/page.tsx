'use client'

export const dynamic = 'force-dynamic'

import Link from 'next/link'
import { CreditCard, ArrowLeft, Calendar, Activity } from 'lucide-react'
import { useAuthStore } from '@/lib/store/authStore'
import { useMemberProfile } from '@/lib/hooks/useMembers'
import { LoadingSpinner, EmptyState } from '@/components/shared/EmptyState'
import { formatDate, getDaysRemaining, formatCurrency } from '@/lib/utils/formatters'
import { cn } from '@/lib/utils/cn'

export default function MemberSubscriptionPage() {
  const memberId = useAuthStore((s) => (s.userType === 'member' ? String(s.user?.id ?? '') : ''))
  const businessId = useAuthStore((s) => String(s.businessContext?.id ?? ''))

  const { data, isLoading } = useMemberProfile(businessId, memberId)

  if (isLoading) return <LoadingSpinner />

  const active = data?.activeSubscription
  const daysLeft = active?.endDate ? getDaysRemaining(active.endDate) : null

  return (
    <div className="max-w-3xl mx-auto px-6 py-10">
      <div className="flex items-center justify-between mb-6">
        <Link
          href="/member/dashboard"
          className="inline-flex items-center gap-2 text-sm text-white/50 hover:text-white/80 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" /> Back to dashboard
        </Link>
      </div>

      <div className="p-6 rounded-2xl border border-white/6 bg-white/[0.02] mb-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-display font-700 flex items-center gap-2">
              <CreditCard className="w-6 h-6 text-emerald-400" /> Subscription
            </h1>
            <p className="text-sm text-white/45 mt-1">
              View your current plan and membership status.
            </p>
          </div>
        </div>
      </div>

      {!active ? (
        <EmptyState
          icon={CreditCard}
          title="No active subscription"
          description="Ask your gym to assign a plan to your account."
        />
      ) : (
        <div className="p-6 rounded-2xl border border-white/6 bg-white/[0.02]">
          <div className="flex items-start justify-between gap-4">
            <div>
              <div className="text-sm text-white/40">Plan</div>
              <div className="text-lg font-display font-700 text-white/85 mt-0.5">{active.planName}</div>
              <div className="text-xs text-white/40 mt-2 flex items-center gap-2">
                <Calendar className="w-3.5 h-3.5" />
                {formatDate(active.startDate)} → {formatDate(active.endDate)}
              </div>
            </div>
            <div className="text-right">
              <div
                className={cn(
                  'inline-flex items-center gap-2 text-xs px-3 py-1.5 rounded-full font-medium',
                  active.status === 'ACTIVE'
                    ? 'bg-emerald-500/15 text-emerald-400'
                    : 'bg-red-500/15 text-red-400'
                )}
              >
                <Activity className="w-3.5 h-3.5" /> {active.status}
              </div>
              <div className="text-xs text-white/40 mt-2">
                {daysLeft == null ? '—' : daysLeft < 0 ? 'Expired' : `${daysLeft} days left`}
              </div>
              <div className="text-sm font-display font-700 text-amber-400 mt-1">
                {formatCurrency(active.amount)}
              </div>
            </div>
          </div>

          <div className="mt-5 pt-5 border-t border-white/6 text-xs text-white/35">
            For plan changes or renewals, please contact your gym staff.
          </div>
        </div>
      )}
    </div>
  )
}

