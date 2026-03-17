'use client'
import { motion } from 'framer-motion'
import { useAuth } from '@/lib/hooks/useAuth'
import { useAuthStore } from '@/lib/store/authStore'
import { useMemberDashboard } from '@/lib/hooks/useDashboard'
import {
  LogOut, CreditCard, Calendar, Activity, TrendingUp,
  CheckCircle, AlertTriangle, Clock, Dumbbell,
} from 'lucide-react'
import { getInitials, getUserDisplayName, formatDate, formatCurrency, getDaysRemaining } from '@/lib/utils/formatters'
import { cn } from '@/lib/utils/cn'
import Link from 'next/link'

export default function MemberDashboardPage() {
  const { user, logout } = useAuth()
  const { businessContext } = useAuthStore()
  const displayName = getUserDisplayName(user)

  const memberId = useAuthStore((s) => (s.userType === 'member' ? (s.user?.id ?? '') : ''))
  const { data: dash } = useMemberDashboard(String(memberId))

  // Prefer backend dashboard data; fall back to stored login snapshot
  const activeSub = useAuthStore(s => s.activeSubscription)
  const planName = dash?.planName ?? activeSub?.planName
  const endDate = dash?.subscriptionEndDate ?? activeSub?.endDate
  const startDate = activeSub?.startDate
  const amountPaid = activeSub?.amountPaid

  const daysLeft   = endDate ? getDaysRemaining(endDate) : null
  const isExpired  = daysLeft !== null && daysLeft < 0
  const isExpiring = daysLeft !== null && daysLeft >= 0 && daysLeft <= 7

  return (
    <div className="min-h-[calc(100vh-73px)] p-6">
      <div className="max-w-2xl mx-auto">
        <div className="flex items-center gap-2 mb-4">
          <Link
            href="/member/subscription"
            className="inline-flex items-center gap-2 px-3 py-2 rounded-xl border border-white/8 bg-white/[0.01] text-xs text-white/60 hover:text-white/80 hover:bg-white/[0.03] transition-all"
          >
            <CreditCard className="w-3.5 h-3.5" /> Subscription
          </Link>
          <Link
            href="/member/payments"
            className="inline-flex items-center gap-2 px-3 py-2 rounded-xl border border-white/8 bg-white/[0.01] text-xs text-white/60 hover:text-white/80 hover:bg-white/[0.03] transition-all"
          >
            <TrendingUp className="w-3.5 h-3.5" /> Payments
          </Link>
        </div>
        {/* Welcome card */}
        <motion.div
          initial={{ opacity:0, y:16 }}
          animate={{ opacity:1, y:0 }}
          className="relative p-6 rounded-2xl border border-white/8 bg-gradient-to-br from-emerald-500/10 to-teal-500/5 mb-6 overflow-hidden"
        >
          <div className="absolute top-0 right-0 w-48 h-48 bg-emerald-500/8 rounded-full blur-3xl pointer-events-none" />
          <div className="flex items-center gap-4 relative">
            <div className="w-14 h-14 rounded-2xl bg-emerald-600/25 border border-emerald-500/25 flex items-center justify-center text-lg font-display font-700 text-emerald-300">
              {getInitials(displayName)}
            </div>
            <div>
              <p className="text-white/50 text-sm">Welcome back,</p>
              <h1 className="text-2xl font-display font-700">{displayName}</h1>
              {businessContext?.name && (
                <p className="text-xs text-white/40 mt-0.5 flex items-center gap-1">
                  <Dumbbell className="w-3 h-3" /> {businessContext.name}
                </p>
              )}
            </div>
          </div>
        </motion.div>

        {/* Membership status banner */}
        {planName && endDate ? (
          <motion.div
            initial={{ opacity:0, y:12 }}
            animate={{ opacity:1, y:0 }}
            transition={{ delay:0.1 }}
            className={cn(
              'p-5 rounded-2xl border mb-6',
              isExpired  ? 'border-red-500/20 bg-red-500/5'    :
              isExpiring ? 'border-amber-500/20 bg-amber-500/5' :
                           'border-emerald-500/20 bg-emerald-500/5'
            )}
          >
            <div className="flex items-start justify-between gap-4">
              <div className="flex items-center gap-3">
                {isExpired
                  ? <AlertTriangle className="w-5 h-5 text-red-400 flex-shrink-0" />
                  : isExpiring
                  ? <Clock className="w-5 h-5 text-amber-400 flex-shrink-0" />
                  : <CheckCircle className="w-5 h-5 text-emerald-400 flex-shrink-0" />
                }
                <div>
                  <h3 className={cn('text-sm font-display font-600',
                    isExpired ? 'text-red-400' : isExpiring ? 'text-amber-400' : 'text-white/80'
                  )}>
                    {isExpired ? 'Membership Expired' : isExpiring ? 'Expiring Soon' : 'Membership Active'}
                  </h3>
                  <p className="text-xs text-white/45 mt-0.5">{planName}</p>
                </div>
              </div>
              <div className="text-right flex-shrink-0">
                <div className={cn('text-sm font-display font-700',
                  isExpired ? 'text-red-400' : isExpiring ? 'text-amber-400' : 'text-emerald-400'
                )}>
                  {isExpired ? 'Expired' : `${daysLeft}d left`}
                </div>
                <div className="text-xs text-white/35 mt-0.5">Until {formatDate(endDate)}</div>
              </div>
            </div>
            <div className="mt-3 pt-3 border-t border-white/5 grid grid-cols-2 gap-3 text-xs text-white/40">
              <span>Start: {startDate ? formatDate(startDate) : '—'}</span>
              <span>End: {formatDate(endDate)}</span>
            </div>
          </motion.div>
        ) : (
          <motion.div
            initial={{ opacity:0, y:12 }}
            animate={{ opacity:1, y:0 }}
            transition={{ delay:0.1 }}
            className="p-5 rounded-2xl border border-amber-500/20 bg-amber-500/5 mb-6"
          >
            <div className="flex items-center gap-3">
              <AlertTriangle className="w-5 h-5 text-amber-400 flex-shrink-0" />
              <div>
                <h3 className="text-sm font-display font-600 text-amber-400">No Active Subscription</h3>
                <p className="text-xs text-white/40 mt-0.5">Contact your gym to assign a subscription plan.</p>
              </div>
            </div>
          </motion.div>
        )}

        {/* Quick info grid */}
        <div className="grid grid-cols-2 gap-4 mb-6">
          {[
            {
              label: 'Member Since',
              value: user?.createdAt ? formatDate(user.createdAt) : '—',
              icon: Calendar,
              accent: 'text-indigo-400',
              bg: 'bg-indigo-500/10 border-indigo-500/15',
            },
            {
              label: 'Active Plan',
              value: planName ?? 'None',
              icon: CreditCard,
              accent: 'text-emerald-400',
              bg: 'bg-emerald-500/10 border-emerald-500/15',
            },
            {
              label: 'Days Remaining',
              value: daysLeft !== null && daysLeft >= 0 ? `${daysLeft} days` : isExpired ? 'Expired' : '—',
              icon: Activity,
              accent: isExpiring || isExpired ? 'text-amber-400' : 'text-cyan-400',
              bg: 'bg-cyan-500/10 border-cyan-500/15',
            },
            {
              label: 'Amount Paid',
              value: amountPaid ? formatCurrency(amountPaid) : '—',
              icon: TrendingUp,
              accent: 'text-amber-400',
              bg: 'bg-amber-500/10 border-amber-500/15',
            },
          ].map((stat, i) => (
            <motion.div
              key={stat.label}
              initial={{ opacity:0, y:12 }}
              animate={{ opacity:1, y:0 }}
              transition={{ delay: 0.15 + i * 0.06 }}
              className={cn('p-4 rounded-2xl border', stat.bg)}
            >
              <div className="flex items-center gap-2 mb-2">
                <stat.icon className={cn('w-4 h-4', stat.accent)} />
                <span className="text-xs text-white/40">{stat.label}</span>
              </div>
              <div className="text-base font-display font-700 text-white">{stat.value}</div>
            </motion.div>
          ))}
        </div>

        {/* Contact gym note */}
        <motion.div
          initial={{ opacity:0 }}
          animate={{ opacity:1 }}
          transition={{ delay:0.4 }}
          className="p-4 rounded-2xl border border-white/5 bg-white/[0.02] text-center"
        >
          <p className="text-xs text-white/35 leading-relaxed">
            For queries about your membership, payments, or subscription renewal,<br />
            please contact your gym or studio directly.
          </p>
        </motion.div>

        {/* Sign out */}
        <div className="mt-6 flex justify-center">
          <button
            onClick={() => logout()}
            className="flex items-center gap-2 px-5 py-2.5 rounded-xl border border-white/10 text-sm text-white/50 hover:text-white hover:bg-white/5 transition-all"
          >
            <LogOut className="w-4 h-4" /> Sign Out
          </button>
        </div>
      </div>
    </div>
  )
}