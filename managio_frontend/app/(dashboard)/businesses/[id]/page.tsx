'use client'

import { useParams } from 'next/navigation'
import { useBusiness, useBusinessStats } from '@/lib/hooks/useBusiness'
import { StatsCard } from '@/components/shared/StatsCard'
import { CardSkeleton, LoadingSpinner } from '@/components/shared/EmptyState'
import { PageHeader } from '@/components/shared/PageHeader'
import { StatusBadge } from '@/components/shared/StatusBadge'
import { Users, CreditCard, TrendingUp, Clock, UserCog, BarChart3, ArrowRight, ScrollText, Building2 } from 'lucide-react'
import Link from 'next/link'
import { formatCurrency } from '@/lib/utils/formatters'
import { motion } from 'framer-motion'

export default function BusinessOverviewPage() {
  const { id } = useParams<{ id: string }>()
  const { data: business, isLoading: bizLoading } = useBusiness(id)
  const { data: stats, isLoading: statsLoading } = useBusinessStats(id)

  if (bizLoading) return <LoadingSpinner />

  const quickLinks = [
    { label: 'Members', href: `/businesses/${id}/members`, icon: Users, description: 'Manage member profiles', count: stats?.totalMembers },
    { label: 'Staff', href: `/businesses/${id}/staff`, icon: UserCog, description: 'Manage staff access', count: business?.staffCount },
    { label: 'Subscriptions', href: `/businesses/${id}/subscriptions`, icon: CreditCard, description: 'Active plans & members', count: stats?.activeSubscriptions },
    { label: 'Payments', href: `/businesses/${id}/payments`, icon: TrendingUp, description: 'Revenue & transactions', count: undefined },
    { label: 'Statistics', href: `/businesses/${id}/statistics`, icon: BarChart3, description: 'Analytics & reports', count: undefined },
    { label: 'Audit Logs', href: `/businesses/${id}/audit-logs`, icon: ScrollText, description: 'Activity history', count: undefined },
  ]

  return (
    <div>
      <PageHeader
        title={business?.name || 'Business'}
        description={business?.description || business?.type?.replace(/_/g, ' ')}
        icon={Building2}
        breadcrumbs={[
          { label: 'Businesses', href: '/businesses' },
          { label: business?.name || 'Business' },
        ]}
        actions={
          <div className="flex items-center gap-2">
            {business?.publicId && (
              <span className="text-xs px-2.5 py-1 rounded-lg bg-indigo-500/10 text-indigo-300 border border-indigo-500/20">
                {business.publicId}
              </span>
            )}
            <StatusBadge status={business?.status || 'UNKNOWN'} size="md" />
          </div>
        }
      />

      {/* Stats */}
      {statsLoading ? (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          {Array.from({ length: 4 }).map((_, i) => <CardSkeleton key={i} />)}
        </div>
      ) : stats && (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <StatsCard title="Total Members" value={stats.totalMembers} icon={Users} accent="indigo" index={0} />
          <StatsCard title="Monthly Revenue" value={formatCurrency(stats.monthlyRevenue)} icon={TrendingUp} accent="amber" index={1} />
          <StatsCard title="Active Subscriptions" value={stats.activeSubscriptions} icon={CreditCard} accent="emerald" index={2} />
          <StatsCard title="Expiring (7d)" value={stats.expiringIn7Days} icon={Clock} accent="pink" index={3} />
        </div>
      )}

      {/* Quick nav */}
      <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
        {quickLinks.map((link, i) => (
          <motion.div
            key={link.href}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.06, ease: [0.22, 1, 0.36, 1] }}
          >
            <Link
              href={link.href}
              className="group flex items-center justify-between p-4 rounded-2xl border border-white/[0.06] bg-[hsl(var(--card))] hover:bg-white/[0.04] hover:border-white/[0.1] transition-all duration-300"
            >
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-indigo-500/10 border border-indigo-500/15 flex items-center justify-center">
                  <link.icon className="w-4 h-4 text-indigo-400" />
                </div>
                <div>
                  <div className="text-sm font-medium text-white/80 group-hover:text-white transition-colors">{link.label}</div>
                  <div className="text-xs text-white/30">{link.description}</div>
                </div>
              </div>
              <div className="flex items-center gap-2">
                {link.count !== undefined && (
                  <span className="text-sm font-display font-600 text-white/45">{link.count}</span>
                )}
                <ArrowRight className="w-3.5 h-3.5 text-white/20 group-hover:text-white/50 group-hover:translate-x-0.5 transition-all" />
              </div>
            </Link>
          </motion.div>
        ))}
      </div>
    </div>
  )
}