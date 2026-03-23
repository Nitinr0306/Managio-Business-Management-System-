'use client'
import { motion } from 'framer-motion'
import { useAuthStore } from '@/lib/store/authStore'
import { useAuth } from '@/lib/hooks/useAuth'
import { useStaffDashboard } from '@/lib/hooks/useDashboard'
import { StatsCard } from '@/components/shared/StatsCard'
import { LoadingSpinner } from '@/components/shared/EmptyState'
import {
  Users, Clock, CreditCard, TrendingUp,
  LogOut, UserCog, Shield, ArrowRight,
  CheckSquare,
} from 'lucide-react'
import {
  formatCurrency, formatRelative, getInitials,
  getUserDisplayName, formatDate,
} from '@/lib/utils/formatters'
import { cn } from '@/lib/utils/cn'
import Link from 'next/link'

export default function StaffDashboardPage() {
  const { user, logout } = useAuth()
  const { staffContext, businessContext } = useAuthStore()
  const businessId = staffContext?.businessId ? String(staffContext.businessId) : ''

  const { data: dash, isLoading: dashLoading } = useStaffDashboard(businessId)

  const displayName = getUserDisplayName(user)

  if (!businessId) {
    return (
      <div className="min-h-screen bg-[#070710] flex items-center justify-center">
        <div className="text-center">
          <p className="text-white/40 text-sm mb-3">No business context found.</p>
          <button onClick={() => logout()} className="text-sm text-red-400 hover:text-red-300 transition-colors">
            Sign out and try again
          </button>
        </div>
      </div>
    )
  }

  const quickActions = [
    { label: 'My Tasks', href: '/staff/tasks', icon: CheckSquare },
    ...(staffContext?.canManageMembers  ? [{ label: 'Members',       href: `/businesses/${businessId}/members`,       icon: Users }] : []),
    ...(staffContext?.canManagePayments ? [{ label: 'Record Payment', href: `/businesses/${businessId}/payments/new`,   icon: CreditCard }] : []),
    ...(staffContext?.canViewReports    ? [{ label: 'Statistics',     href: `/businesses/${businessId}/statistics`,    icon: TrendingUp }] : []),
  ]

  return (
    <div className="min-h-screen bg-[#070710] p-6">
      <div className="fixed inset-0 pointer-events-none">
        <div className="absolute top-0 right-0 w-96 h-96 bg-violet-600/5 rounded-full blur-[120px]" />
      </div>

      <div className="max-w-4xl mx-auto relative z-10">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-violet-500/15 border border-violet-500/20 flex items-center justify-center text-sm font-display font-700 text-violet-300">
              {getInitials(displayName)}
            </div>
            <div>
              <h1 className="text-lg font-display font-700">{displayName}</h1>
              <div className="flex items-center gap-2 mt-0.5">
                <span className="text-xs text-white/40 flex items-center gap-1">
                  <UserCog className="w-3 h-3" />
                  {staffContext?.staffRole || 'Staff'}
                </span>
                {businessContext?.name && (
                  <>
                    <span className="text-white/20">·</span>
                    <span className="text-xs text-white/40">{businessContext.name}</span>
                  </>
                )}
              </div>
            </div>
          </div>
          <button onClick={() => logout()}
            className="flex items-center gap-2 px-3 py-2 rounded-xl border border-white/10 text-sm text-white/50 hover:text-white hover:bg-white/5 transition-all">
            <LogOut className="w-3.5 h-3.5" /> Sign Out
          </button>
        </div>

        {/* KPI cards */}
        {dashLoading ? (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
            {Array.from({length:4}).map((_,i) => (
              <div key={i} className="h-24 rounded-2xl border border-white/6 bg-white/[0.02] animate-pulse" />
            ))}
          </div>
        ) : dash && (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
            <StatsCard title="Members Added Today" value={dash.membersAddedToday} icon={Users} accent="indigo" index={0} />
            <StatsCard title="Active Members" value={dash.totalActiveMembers} icon={Users} accent="emerald" index={1} />
            <StatsCard title="Expiring This Week" value={dash.expiringThisWeek?.length ?? 0} icon={Clock} accent="amber" index={2} />
            <StatsCard title="Recent Payments" value={dash.recentPayments?.length ?? 0} icon={CreditCard} accent="cyan" index={3} />
          </div>
        )}

        {/* Permissions banner */}
        <motion.div
          initial={{ opacity:0, y:8 }}
          animate={{ opacity:1, y:0 }}
          transition={{ delay:0.1 }}
          className="flex flex-wrap gap-2 mb-6"
        >
          {[
            { label: 'Manage Members',       active: staffContext?.canManageMembers       },
            { label: 'Manage Payments',      active: staffContext?.canManagePayments      },
            { label: 'Manage Subscriptions', active: staffContext?.canManageSubscriptions },
            { label: 'View Reports',         active: staffContext?.canViewReports         },
          ].map(p => (
            <span key={p.label} className={cn(
              'inline-flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-full font-medium',
              p.active
                ? 'bg-emerald-500/15 text-emerald-400'
                : 'bg-white/5 text-white/25 line-through'
            )}>
              <Shield className="w-2.5 h-2.5" />
              {p.label}
            </span>
          ))}
        </motion.div>

        {/* Quick actions */}
        {quickActions.length > 0 && (
          <div className="grid grid-cols-2 md:grid-cols-3 gap-3 mb-8">
            {quickActions.map((action, i) => (
              <motion.div key={action.href} initial={{ opacity:0, y:8 }} animate={{ opacity:1, y:0 }} transition={{ delay: 0.15 + i * 0.05 }}>
                <Link href={action.href}
                  className="group flex items-center justify-between p-4 rounded-xl border border-white/6 bg-white/[0.02] hover:bg-white/[0.05] hover:border-white/12 transition-all">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-indigo-500/15 flex items-center justify-center">
                      <action.icon className="w-4 h-4 text-indigo-400" />
                    </div>
                    <span className="text-sm font-medium text-white/70 group-hover:text-white transition-colors">{action.label}</span>
                  </div>
                  <ArrowRight className="w-3.5 h-3.5 text-white/20 group-hover:text-white/60 group-hover:translate-x-0.5 transition-all" />
                </Link>
              </motion.div>
            ))}
          </div>
        )}

        <div className="grid md:grid-cols-2 gap-5">
          {/* Expiring subscriptions */}
          <div className="p-5 rounded-2xl border border-white/6 bg-white/[0.02]">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-sm font-display font-600 text-white/80 flex items-center gap-2">
                <Clock className="w-4 h-4 text-amber-400" />
                Expiring This Week
                {dash?.expiringThisWeek?.length ? (
                  <span className="text-xs px-1.5 py-0.5 rounded-md bg-amber-500/15 text-amber-400">{dash.expiringThisWeek.length}</span>
                ) : null}
              </h3>
              {staffContext?.canManageSubscriptions && (
                <Link href={`/businesses/${businessId}/subscriptions`}
                  className="text-xs text-indigo-400 hover:text-indigo-300 flex items-center gap-1 transition-colors">
                  View all <ArrowRight className="w-3 h-3" />
                </Link>
              )}
            </div>
            {dash?.expiringThisWeek?.length ? (
              <div className="space-y-2">
                {dash.expiringThisWeek.slice(0, 6).map((sub: any) => (
                  <div key={`${sub.memberId}-${sub.endDate}`}
                    className="flex items-center justify-between py-2.5 px-3 rounded-xl bg-white/[0.02] hover:bg-white/[0.04] transition-all">
                    <div className="min-w-0 flex-1">
                      <div className="text-sm font-medium text-white/80 truncate">{sub.memberName}</div>
                      <div className="text-xs text-white/35 truncate">{sub.planName}</div>
                    </div>
                    <span className={cn(
                      'text-xs font-medium px-2 py-1 rounded-lg ml-3 flex-shrink-0',
                      sub.daysRemaining <= 2 ? 'bg-red-500/15 text-red-400' :
                      sub.daysRemaining <= 5 ? 'bg-amber-500/15 text-amber-400' :
                      'bg-white/5 text-white/50'
                    )}>
                      {sub.daysRemaining === 0 ? 'Today' : `${sub.daysRemaining}d`}
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <div className="py-10 text-center">
                <div className="text-2xl mb-2">🎉</div>
                <p className="text-sm text-white/30">No subscriptions expiring this week</p>
              </div>
            )}
          </div>

          {/* Recent payments */}
          <div className="p-5 rounded-2xl border border-white/6 bg-white/[0.02]">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-sm font-display font-600 text-white/80 flex items-center gap-2">
                <CreditCard className="w-4 h-4 text-emerald-400" />
                Recent Payments (3d)
              </h3>
              {staffContext?.canManagePayments && (
                <Link href={`/businesses/${businessId}/payments`}
                  className="text-xs text-indigo-400 hover:text-indigo-300 flex items-center gap-1 transition-colors">
                  View all <ArrowRight className="w-3 h-3" />
                </Link>
              )}
            </div>
            {dash?.recentPayments?.length ? (
              <div className="space-y-2">
                {dash.recentPayments.slice(0, 6).map((p: any) => (
                  <div key={p.paymentId}
                    className="flex items-center justify-between py-2.5 px-3 rounded-xl bg-white/[0.02] hover:bg-white/[0.04] transition-all">
                    <div className="min-w-0 flex-1">
                      <div className="text-sm font-medium text-white/80 truncate">{p.memberName}</div>
                      <div className="text-xs text-white/35">{formatRelative(p.paidAt)} · {p.paymentMethod}</div>
                    </div>
                    <span className="text-sm font-display font-700 text-emerald-400 ml-3 flex-shrink-0">
                      {formatCurrency(p.amount)}
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <div className="py-10 text-center">
                <CreditCard className="w-7 h-7 text-white/15 mx-auto mb-2" />
                <p className="text-sm text-white/30">No recent payments</p>
                {staffContext?.canManagePayments && (
                  <Link href={`/businesses/${businessId}/payments/new`}
                    className="text-xs text-indigo-400 hover:text-indigo-300 mt-1 inline-block transition-colors">
                    Record a payment →
                  </Link>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}