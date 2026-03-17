'use client'
import { useParams } from 'next/navigation'
import { BarChart3, TrendingUp, Users, CreditCard, Clock } from 'lucide-react'
import { motion } from 'framer-motion'
import { useBusinessStats } from '@/lib/hooks/useBusiness'
import { useRevenueStats, usePaymentMethodStats } from '@/lib/hooks/usePayments'
import { PageHeader } from '@/components/shared/PageHeader'
import { StatsCard } from '@/components/shared/StatsCard'
import { CardSkeleton } from '@/components/shared/EmptyState'
import { formatCurrency } from '@/lib/utils/formatters'
import {
  BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts'

const PIE_COLORS = ['#6366f1', '#10b981', '#8b5cf6', '#06b6d4', '#f59e0b', '#ec4899']

export default function StatisticsPage() {
  const { id: businessId } = useParams<{ id: string }>()
  const { data: stats, isLoading } = useBusinessStats(businessId)
  const { data: revenue }          = useRevenueStats(businessId)
  const { data: methods }          = usePaymentMethodStats(businessId)

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {Array.from({ length: 8 }).map((_, i) => <CardSkeleton key={i} />)}
        </div>
      </div>
    )
  }

  return (
    <div>
      <PageHeader title="Statistics" description="Business performance analytics" icon={BarChart3} />

      {/* ── Member KPIs ─────────────────────────────────────────────────────── */}
      {stats && (
        <>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
            <StatsCard title="Total Members"   value={stats.totalMembers}          icon={Users}      accent="indigo"  index={0} />
            <StatsCard title="Active Members"  value={stats.activeMembers}         icon={Users}      accent="emerald" index={1} />
            <StatsCard title="Inactive"        value={stats.inactiveMembers}       icon={Users}      accent="pink"    index={2} />
            <StatsCard title="New This Month"  value={stats.newMembersThisMonth}   icon={TrendingUp} accent="cyan"    index={3} />
          </div>

          {/* ── Revenue KPIs ───────────────────────────────────────────────── */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
            <StatsCard title="Total Revenue"   value={formatCurrency(stats.totalRevenue)}             icon={TrendingUp} accent="amber"  index={0} />
            <StatsCard title="Monthly Revenue" value={formatCurrency(stats.monthlyRevenue)}           icon={TrendingUp} accent="amber"  index={1} />
            <StatsCard title="Today's Revenue" value={formatCurrency(stats.todayRevenue)}             icon={CreditCard} accent="emerald" index={2} />
            <StatsCard title="Avg / Member"    value={formatCurrency(stats.averageRevenuePerMember)}  icon={CreditCard} accent="indigo"  index={3} />
          </div>

          {/* ── Subscription KPIs ─────────────────────────────────────────── */}
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-8">
            <StatsCard title="Active Subscriptions"  value={stats.activeSubscriptions} icon={CreditCard} accent="indigo" index={0} />
            <StatsCard title="Expiring in 7 Days"    value={stats.expiringIn7Days}     icon={Clock}      accent="amber"  index={1} />
            <StatsCard title="Expiring in 30 Days"   value={stats.expiringIn30Days}    icon={Clock}      accent="pink"   index={2} />
          </div>
        </>
      )}

      {/* ── Charts ──────────────────────────────────────────────────────────── */}
      <div className="grid lg:grid-cols-2 gap-6">
        {/* Monthly Revenue Bar */}
        <motion.div
          initial={{ opacity:0, y:12 }}
          animate={{ opacity:1, y:0 }}
          className="p-5 rounded-2xl border border-white/6 bg-white/[0.02]"
        >
          <h3 className="text-sm font-display font-600 text-white/80 mb-5">Monthly Revenue</h3>
          {revenue?.revenueByMonth?.length ? (
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={revenue.revenueByMonth} margin={{ top:5, right:5, bottom:0, left:0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" />
                <XAxis
                  dataKey="month"
                  tick={{ fill:'rgba(255,255,255,0.3)', fontSize:11 }}
                  axisLine={false}
                  tickLine={false}
                />
                <YAxis
                  tick={{ fill:'rgba(255,255,255,0.3)', fontSize:11 }}
                  axisLine={false}
                  tickLine={false}
                  tickFormatter={v => `₹${(v/1000).toFixed(0)}k`}
                />
                <Tooltip
                  contentStyle={{ background:'#0f0f1a', border:'1px solid rgba(255,255,255,0.1)', borderRadius:12 }}
                  formatter={(v: number) => [formatCurrency(v), 'Revenue']}
                />
                <Bar dataKey="revenue" fill="#6366f1" radius={[6,6,0,0]} />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-[220px] flex flex-col items-center justify-center text-white/25">
              <BarChart3 className="w-8 h-8 mb-2 opacity-30" />
              <span className="text-sm">Revenue data appears after recording payments</span>
            </div>
          )}
        </motion.div>

        {/* Payment Method Pie */}
        <motion.div
          initial={{ opacity:0, y:12 }}
          animate={{ opacity:1, y:0 }}
          transition={{ delay:0.1 }}
          className="p-5 rounded-2xl border border-white/6 bg-white/[0.02]"
        >
          <h3 className="text-sm font-display font-600 text-white/80 mb-5">Payment Methods</h3>
          {methods && methods.length > 0 ? (
            <>
              <ResponsiveContainer width="100%" height={180}>
                <PieChart>
                  <Pie
                    data={methods}
                    cx="50%" cy="50%"
                    outerRadius={80}
                    dataKey="total"
                    labelLine={false}
                    label={({ name, percent }) =>
                      `${name} ${((percent as number) * 100).toFixed(0)}%`
                    }
                  >
                    {methods.map((_: { method: string; total: number }, i: number) => (
  <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
))}
                  </Pie>
                  <Tooltip
                    contentStyle={{ background:'#0f0f1a', border:'1px solid rgba(255,255,255,0.1)', borderRadius:8 }}
                    formatter={(v: number) => [formatCurrency(v), 'Revenue']}
                  />
                </PieChart>
              </ResponsiveContainer>
              <div className="grid grid-cols-2 gap-2 mt-3">
                {methods.map((m: { method: string; total: number }, i: number) => (
                  <div key={m.method} className="flex items-center justify-between px-3 py-2 rounded-xl bg-white/[0.02]">
                    <div className="flex items-center gap-2">
                      <div className="w-2 h-2 rounded-full" style={{ background: PIE_COLORS[i % PIE_COLORS.length] }} />
                      <span className="text-xs text-white/50">{m.method}</span>
                    </div>
                    <span className="text-xs font-medium text-white/70">{formatCurrency(m.total)}</span>
                  </div>
                ))}
              </div>
            </>
          ) : (
            <div className="h-[220px] flex flex-col items-center justify-center text-white/25">
              <CreditCard className="w-7 h-7 mb-2 opacity-30" />
              <span className="text-sm">Payment data appears after recording payments</span>
            </div>
          )}
        </motion.div>
      </div>
    </div>
  )
}