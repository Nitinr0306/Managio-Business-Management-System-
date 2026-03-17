'use client'

import { useParams } from 'next/navigation'
import { useBusiness, useBusinessStats } from '@/lib/hooks/useBusiness'
import { StatsCard } from '@/components/shared/StatsCard'
import { CardSkeleton, LoadingSpinner } from '@/components/shared/EmptyState'
import { Users, CreditCard, TrendingUp, Clock, UserCog, BarChart3, ArrowRight } from 'lucide-react'
import Link from 'next/link'
import { formatCurrency } from '@/lib/utils/formatters'
import { motion } from 'framer-motion'

export default function BusinessOverviewPage() {
  const { id } = useParams<{ id: string }>()
  const { data: business, isLoading: bizLoading } = useBusiness(id)
  const { data: stats, isLoading: statsLoading } = useBusinessStats(id)

  if (bizLoading) return <LoadingSpinner />

  const quickLinks = [
    {
      label: 'Members',
      href: `/businesses/${id}/members`,
      icon: Users,
      description: 'Manage member profiles',
      count: stats?.totalMembers,
    },
    {
      label: 'Staff',
      href: `/businesses/${id}/staff`,
      icon: UserCog,
      description: 'Manage staff access',
      // staffCount comes from BusinessResponse, not BusinessStats
      count: business?.staffCount,
    },
    {
      label: 'Subscriptions',
      href: `/businesses/${id}/subscriptions`,
      icon: CreditCard,
      description: 'Active plans & members',
      count: stats?.activeSubscriptions,
    },
    {
      label: 'Payments',
      href: `/businesses/${id}/payments`,
      icon: TrendingUp,
      description: 'Revenue & transactions',
      count: undefined,
    },
    {
      label: 'Statistics',
      href: `/businesses/${id}/statistics`,
      icon: BarChart3,
      description: 'Analytics & reports',
      count: undefined,
    },
    {
      label: 'Audit Logs',
      href: `/businesses/${id}/audit-logs`,
      icon: Clock,
      description: 'Activity history',
      count: undefined,
    },
  ]

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-display font-700 mb-1">{business?.name}</h1>
        <p className="text-white/45 text-sm">
          {business?.description || business?.type?.replace(/_/g, ' ')}
        </p>
      </div>

      {/* Stats */}
      {statsLoading ? (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          {Array.from({ length: 4 }).map((_, i) => (
            <CardSkeleton key={i} />
          ))}
        </div>
      ) : (
        stats && (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
            <StatsCard
              title="Total Members"
              value={stats.totalMembers}
              icon={Users}
              accent="indigo"
              index={0}
            />
            <StatsCard
              title="Monthly Revenue"
              value={formatCurrency(stats.monthlyRevenue)}
              icon={TrendingUp}
              accent="amber"
              index={1}
            />
            <StatsCard
              title="Active Subscriptions"
              value={stats.activeSubscriptions}
              icon={CreditCard}
              accent="emerald"
              index={2}
            />
            <StatsCard
              title="Expiring (7d)"
              value={stats.expiringIn7Days}
              icon={Clock}
              accent="pink"
              index={3}
            />
          </div>
        )
      )}

      {/* Quick nav */}
      <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
        {quickLinks.map((link, i) => (
          <motion.div
            key={link.href}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.06 }}
          >
            <Link
              href={link.href}
              className="group flex items-center justify-between p-4 rounded-2xl border border-white/6 bg-white/[0.02] hover:bg-white/[0.04] hover:border-white/10 transition-all duration-300"
            >
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-indigo-500/15 flex items-center justify-center">
                  <link.icon className="w-4 h-4 text-indigo-400" />
                </div>
                <div>
                  <div className="text-sm font-medium text-white/80 group-hover:text-white transition-colors">
                    {link.label}
                  </div>
                  <div className="text-xs text-white/35">{link.description}</div>
                </div>
              </div>
              <div className="flex items-center gap-2">
                {link.count !== undefined && (
                  <span className="text-sm font-display font-600 text-white/50">
                    {link.count}
                  </span>
                )}
                <ArrowRight className="w-3.5 h-3.5 text-white/25 group-hover:text-white/60 group-hover:translate-x-0.5 transition-all" />
              </div>
            </Link>
          </motion.div>
        ))}
      </div>
    </div>
  )
}