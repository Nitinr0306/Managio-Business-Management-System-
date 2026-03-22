'use client'

import { useEffect } from 'react'
import { motion } from 'framer-motion'
import { useRouter } from 'next/navigation'
import { useBusinessStore } from '@/lib/store/businessStore'
import { useMyBusinesses } from '@/lib/hooks/useBusiness'
import { useOwnerDashboard } from '@/lib/hooks/useDashboard'
import { StatsCard } from '@/components/shared/StatsCard'
import { CardSkeleton, LoadingSpinner } from '@/components/shared/EmptyState'
import { PageHeader } from '@/components/shared/PageHeader'
import {
  Users, TrendingUp, CreditCard, Clock, Building2,
  Activity, BarChart3, ArrowRight, Plus, Sparkles,
} from 'lucide-react'
import Link from 'next/link'
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, PieChart, Pie, Cell,
} from 'recharts'
import { formatCurrency, formatRelative } from '@/lib/utils/formatters'
import { cn } from '@/lib/utils/cn'
import { useAuth } from '@/lib/hooks/useAuth'

const CHART_COLORS = ['#6366f1', '#10b981', '#f59e0b', '#ec4899', '#06b6d4']

/* ── No Business View ─────────────────────────────────────────── */
function NoBusiness() {
  const router = useRouter()
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] text-center">
      <motion.div
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.5 }}
        className="max-w-sm"
      >
        <div className="relative mx-auto w-24 h-24 mb-6">
          <div className="absolute inset-0 bg-indigo-500/15 rounded-3xl blur-xl" />
          <div className="relative w-24 h-24 rounded-3xl bg-indigo-500/10 border border-indigo-500/15 flex items-center justify-center">
            <Building2 className="w-11 h-11 text-indigo-400" />
          </div>
          <div className="absolute -top-2 -right-2 w-7 h-7 rounded-full bg-amber-500/15 border border-amber-500/20 flex items-center justify-center animate-float">
            <Sparkles className="w-3.5 h-3.5 text-amber-400" />
          </div>
        </div>

        <h2 className="text-2xl font-display font-700 mb-2">Welcome to Managio!</h2>
        <p className="text-white/40 text-sm mb-8 leading-relaxed">
          Create your first business to start managing members, staff, subscriptions, and revenue.
        </p>
        <div className="flex flex-col gap-3">
          <button
            onClick={() => router.push('/onboarding')}
            className="inline-flex items-center justify-center gap-2 px-6 py-3.5 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-xl transition-all shadow-xl shadow-indigo-600/25 hover:-translate-y-0.5"
          >
            <Sparkles className="w-4 h-4" />
            Start Setup Wizard
            <ArrowRight className="w-4 h-4" />
          </button>
          <Link
            href="/businesses/new"
            className="inline-flex items-center justify-center gap-2 px-6 py-3 border border-white/[0.08] text-white/50 hover:text-white/70 hover:bg-white/[0.03] rounded-xl transition-all text-sm"
          >
            <Plus className="w-4 h-4" />
            Create Manually
          </Link>
        </div>
      </motion.div>
    </div>
  )
}

/* ── Dashboard Content ────────────────────────────────────────── */
function DashboardContent({ businessId }: { businessId: string }) {
  const { data: dash, isLoading: dashLoading } = useOwnerDashboard(businessId)
  const { currentBusiness } = useBusinessStore()

  const revenueTrend = dash?.revenueGrowth?.monthlyTrend ?? []

  const statCards = dash
    ? [
        {
          title: 'Total Members',
          value: dash.totalMembers,
          icon: Users,
          accent: 'indigo' as const,
          change: dash.newMembersThisMonth > 0 ? `+${dash.newMembersThisMonth} new` : undefined,
          changeType: 'up' as const,
        },
        {
          title: 'Active Members',
          value: dash.activeMembers,
          icon: Activity,
          accent: 'emerald' as const,
        },
        {
          title: 'Monthly Revenue',
          value: formatCurrency(dash.monthlyRevenue),
          icon: TrendingUp,
          accent: 'amber' as const,
        },
        {
          title: 'Active Subscriptions',
          value: dash.activeSubscriptions,
          icon: CreditCard,
          accent: 'cyan' as const,
        },
        {
          title: 'Expiring (7d)',
          value: dash.expiringIn7Days,
          icon: Clock,
          accent: 'pink' as const,
          changeType: (dash.expiringIn7Days > 5 ? 'down' : 'neutral') as 'down' | 'neutral',
        },
        {
          title: "Today's Revenue",
          value: formatCurrency(dash.todayRevenue),
          icon: BarChart3,
          accent: 'indigo' as const,
        },
      ]
    : []

  const paymentMethods =
    dash?.paymentMethodStats?.byPaymentMethod?.map((m) => ({
      method: m.method,
      total: m.totalAmount,
      count: m.count,
      percentage: m.percentage,
    })) ?? []

  const expiring = dash?.upcomingExpirations ?? []
  const recentPayments = dash?.recentPayments ?? []

  return (
    <div>
      <PageHeader
        title="Dashboard"
        description={currentBusiness ? `Overview · ${currentBusiness.name}` : 'Business overview'}
        icon={BarChart3}
        actions={
          <Link
            href="/businesses/new"
            className="flex items-center gap-2 px-4 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-xl transition-all shadow-lg shadow-indigo-600/20 hover:-translate-y-px"
          >
            <Plus className="w-4 h-4" />
            <span className="hidden sm:inline">New Business</span>
          </Link>
        }
      />

      {/* KPI Cards */}
      {dashLoading ? (
        <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-4 mb-6">
          {Array.from({ length: 6 }).map((_, i) => <CardSkeleton key={i} />)}
        </div>
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-4 mb-6">
          {statCards.map((card, i) => (
            <StatsCard key={card.title} {...card} index={i} />
          ))}
        </div>
      )}

      {/* Charts row */}
      <div className="grid lg:grid-cols-3 gap-5 mb-5">
        {/* Revenue area chart */}
        <div className="lg:col-span-2 p-5 rounded-2xl border border-white/[0.06] bg-[hsl(var(--card))]">
          <div className="flex items-center justify-between mb-5">
            <h3 className="text-sm font-display font-600 text-white/75">Revenue Trend</h3>
            <span className="text-[11px] text-white/25 bg-white/[0.04] px-2.5 py-1 rounded-lg">Last 12 months</span>
          </div>
          {revenueTrend.length > 0 ? (
            <ResponsiveContainer width="100%" height={200}>
              <AreaChart data={revenueTrend} margin={{ top: 5, right: 5, bottom: 0, left: 0 }}>
                <defs>
                  <linearGradient id="revGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.03)" />
                <XAxis dataKey="month" tick={{ fill: 'rgba(255,255,255,0.25)', fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fill: 'rgba(255,255,255,0.25)', fontSize: 11 }} axisLine={false} tickLine={false} tickFormatter={(v) => `₹${v / 1000}k`} />
                <Tooltip
                  contentStyle={{ background: 'hsl(228 14% 8%)', border: '1px solid rgba(255,255,255,0.06)', borderRadius: 12, fontSize: 12 }}
                  labelStyle={{ color: 'rgba(255,255,255,0.5)' }}
                  formatter={(v: number) => [formatCurrency(v), 'Revenue']}
                />
                <Area type="monotone" dataKey="revenue" stroke="#6366f1" strokeWidth={2} fill="url(#revGrad)" />
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-[200px] flex flex-col items-center justify-center text-white/20">
              <BarChart3 className="w-8 h-8 mb-2 opacity-25" />
              <span className="text-sm">Revenue data will appear after recording payments</span>
            </div>
          )}
        </div>

        {/* Payment method pie */}
        <div className="p-5 rounded-2xl border border-white/[0.06] bg-[hsl(var(--card))]">
          <h3 className="text-sm font-display font-600 text-white/75 mb-5">Payment Methods</h3>
          {paymentMethods.length > 0 ? (
            <>
              <ResponsiveContainer width="100%" height={150}>
                <PieChart>
                  <Pie data={paymentMethods} cx="50%" cy="50%" innerRadius={45} outerRadius={70} dataKey="total" paddingAngle={3}>
                    {paymentMethods.map((_: unknown, i: number) => (
                      <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{ background: 'hsl(228 14% 8%)', border: '1px solid rgba(255,255,255,0.06)', borderRadius: 8, fontSize: 11 }}
                    formatter={(v: number) => [formatCurrency(v), 'Revenue']}
                  />
                </PieChart>
              </ResponsiveContainer>
              <div className="space-y-2 mt-3">
                {paymentMethods.slice(0, 4).map((m: any, i: number) => (
                  <div key={m.method} className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <div className="w-2 h-2 rounded-full" style={{ background: CHART_COLORS[i % CHART_COLORS.length] }} />
                      <span className="text-xs text-white/45">{m.method}</span>
                    </div>
                    <span className="text-xs font-medium text-white/60">{m.percentage?.toFixed(0)}%</span>
                  </div>
                ))}
              </div>
            </>
          ) : (
            <div className="h-[200px] flex flex-col items-center justify-center text-white/20">
              <CreditCard className="w-7 h-7 mb-2 opacity-25" />
              <span className="text-xs text-center">Record payments to see method breakdown</span>
            </div>
          )}
        </div>
      </div>

      {/* Bottom row */}
      <div className="grid lg:grid-cols-2 gap-5">
        {/* Expiring subs */}
        <div className="p-5 rounded-2xl border border-white/[0.06] bg-[hsl(var(--card))]">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-display font-600 text-white/75 flex items-center gap-2">
              <Clock className="w-4 h-4 text-amber-400" />
              Expiring in 7 Days
              {expiring && expiring.length > 0 && (
                <span className="text-[10px] px-1.5 py-0.5 rounded-md bg-amber-500/10 text-amber-400 font-medium">{expiring.length}</span>
              )}
            </h3>
            <Link href={`/businesses/${businessId}/subscriptions`} className="text-xs text-indigo-400 hover:text-indigo-300 flex items-center gap-1 transition-colors">
              View all <ArrowRight className="w-3 h-3" />
            </Link>
          </div>
          {expiring && expiring.length > 0 ? (
            <div className="space-y-1.5">
              {expiring.slice(0, 6).map((sub) => (
                <div key={`${sub.memberId}-${sub.endDate}`} className="flex items-center justify-between py-2.5 px-3 rounded-xl bg-white/[0.02] hover:bg-white/[0.04] transition-all group">
                  <div className="min-w-0 flex-1">
                    <div className="text-sm font-medium text-white/75 truncate">{sub.memberName}</div>
                    <div className="text-xs text-white/30 truncate mt-0.5">{sub.planName}</div>
                  </div>
                  <div className={cn(
                    'text-xs font-medium px-2 py-1 rounded-lg ml-3 flex-shrink-0',
                    sub.daysRemaining <= 2 ? 'bg-red-500/10 text-red-400' :
                    sub.daysRemaining <= 5 ? 'bg-amber-500/10 text-amber-400' :
                    'bg-white/[0.04] text-white/45'
                  )}>
                    {sub.daysRemaining === 0 ? 'Today' : `${sub.daysRemaining}d`}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="py-10 text-center">
              <div className="text-2xl mb-2">🎉</div>
              <p className="text-sm text-white/25">No subscriptions expiring this week</p>
            </div>
          )}
        </div>

        {/* Recent payments */}
        <div className="p-5 rounded-2xl border border-white/[0.06] bg-[hsl(var(--card))]">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-display font-600 text-white/75 flex items-center gap-2">
              <CreditCard className="w-4 h-4 text-emerald-400" />
              Recent Payments
            </h3>
            <Link href={`/businesses/${businessId}/payments`} className="text-xs text-indigo-400 hover:text-indigo-300 flex items-center gap-1 transition-colors">
              View all <ArrowRight className="w-3 h-3" />
            </Link>
          </div>
          {recentPayments && recentPayments.length > 0 ? (
            <div className="space-y-1.5">
              {recentPayments.slice(0, 6).map((p) => (
                <div key={p.paymentId} className="flex items-center justify-between py-2.5 px-3 rounded-xl bg-white/[0.02] hover:bg-white/[0.04] transition-all">
                  <div className="min-w-0 flex-1">
                    <div className="text-sm font-medium text-white/75 truncate">{p.memberName}</div>
                    <div className="text-xs text-white/30 mt-0.5">{formatRelative(p.paidAt)} · {p.paymentMethod}</div>
                  </div>
                  <div className="text-sm font-display font-600 text-emerald-400 ml-3 flex-shrink-0">
                    {formatCurrency(p.amount)}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="py-10 text-center">
              <CreditCard className="w-8 h-8 text-white/10 mx-auto mb-2" />
              <p className="text-sm text-white/25">No recent payments</p>
              <Link href={`/businesses/${businessId}/payments/new`} className="text-xs text-indigo-400 hover:text-indigo-300 mt-1 inline-block transition-colors">
                Record a payment →
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

/* ── Main Export ───────────────────────────────────────────────── */
export default function DashboardPage() {
  const { currentBusiness, setCurrentBusiness } = useBusinessStore()
  const { data: businesses, isLoading } = useMyBusinesses()
  const router = useRouter()
  const { userType } = useAuth()

  useEffect(() => {
    if (userType === 'staff') router.replace('/staff/dashboard')
    else if (userType === 'member') router.replace('/member/dashboard')
  }, [userType, router])

  // Auto-select first business if none is selected
  useEffect(() => {
    if (!currentBusiness && businesses?.length) {
      setCurrentBusiness(businesses[0])
    }
  }, [currentBusiness, businesses, setCurrentBusiness])

  if (isLoading) return <LoadingSpinner />
  if (!businesses?.length) return <NoBusiness />

  const activeBizId = String(currentBusiness?.id || businesses[0]?.id)
  return <DashboardContent businessId={activeBizId} />
}