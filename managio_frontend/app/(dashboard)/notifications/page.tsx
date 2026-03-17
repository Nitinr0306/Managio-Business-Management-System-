'use client'

import { motion } from 'framer-motion'
import { Bell, ScrollText } from 'lucide-react'
import { PageHeader } from '@/components/shared/PageHeader'
import { formatRelative } from '@/lib/utils/formatters'
import { useBusinessStore } from '@/lib/store/businessStore'
import { useRecentAuditLogs } from '@/lib/hooks/useAuditLogs'
import { LoadingSpinner, EmptyState } from '@/components/shared/EmptyState'

export default function NotificationsPage() {
  const businessId = useBusinessStore((s) => (s.currentBusiness?.id ? String(s.currentBusiness.id) : ''))
  const { data, isLoading } = useRecentAuditLogs(businessId, 7)
  const logs = data ?? []

  return (
    <div className="max-w-2xl mx-auto">
      <PageHeader
        title="Notifications"
        description="Recent activity (last 7 days)"
        icon={Bell}
      />

      {isLoading ? (
        <LoadingSpinner />
      ) : logs.length === 0 ? (
        <EmptyState
          icon={Bell}
          title="No recent activity"
          description="Actions in your business will appear here."
        />
      ) : (
        <div className="space-y-2">
          {logs.map((log, i) => {
            return (
              <motion.div
                key={log.id}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.04 }}
                className="group relative p-4 rounded-2xl border border-white/5 bg-white/[0.01] hover:bg-white/[0.03] transition-all duration-200"
              >
                <div className="flex items-start gap-3">
                  <div className="w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0 mt-0.5 bg-indigo-500/15">
                    <ScrollText className="w-4 h-4 text-indigo-400" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2">
                      <p className="text-sm font-medium text-white/85">
                        {log.action.replace(/_/g, ' ')}
                      </p>
                    </div>
                    <p className="text-xs text-white/45 mt-0.5 leading-relaxed">
                      {log.entityType}
                      {log.entityId != null ? ` #${log.entityId}` : ''} • User #{log.userId}
                      {log.details ? ` — ${log.details}` : ''}
                    </p>
                    <p className="text-xs text-white/25 mt-1.5">{formatRelative(log.createdAt)}</p>
                  </div>
                </div>
              </motion.div>
            )
          })}
        </div>
      )}
    </div>
  )
}