'use client'

import { useState } from 'react'
import { useParams } from 'next/navigation'
import { ScrollText, ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react'
import { useAuditLogs } from '@/lib/hooks/useAuditLogs'
import { PageHeader } from '@/components/shared/PageHeader'
import { EmptyState, TableSkeleton } from '@/components/shared/EmptyState'
import { ResponsiveCardList } from '@/components/shared/ResponsiveCardList'
import { formatDateTime } from '@/lib/utils/formatters'
import { cn } from '@/lib/utils/cn'
import type { AuditEntityType } from '@/lib/types/audit'

const ACTION_COLORS: Record<string, string> = {
  // These are examples; backend actions are free-form strings.
  STAFF_ADDED: 'bg-emerald-500/15 text-emerald-400',
  STAFF_UPDATED: 'bg-indigo-500/15 text-indigo-400',
  STAFF_TERMINATED: 'bg-red-500/15 text-red-400',
  STAFF_SUSPENDED: 'bg-amber-500/15 text-amber-400',
  STAFF_ACTIVATED: 'bg-emerald-500/15 text-emerald-400',
  MANUAL_PAYMENT_RECORDED: 'bg-amber-500/15 text-amber-400',
  MEMBER_DEACTIVATED: 'bg-red-500/15 text-red-400',
}

const ENTITY_TYPES: AuditEntityType[] = [
  'MEMBER',
  'STAFF',
  'PAYMENT',
  'SUBSCRIPTION',
  'PLAN',
  'BUSINESS',
  'USER',
  'TASK',
]

export default function AuditLogsPage() {
  const { id: businessId } = useParams<{ id: string }>()
  const [page, setPage] = useState(0)
  const [entityFilter, setEntityFilter] = useState<AuditEntityType | undefined>()

  const { data, isLoading, refetch } = useAuditLogs(businessId, {
    page,
    size: 25,
    entityType: entityFilter,
  })

  const formatActor = (log: { actorPublicId?: string; actorType?: string; userId?: number }) => {
    if (log.actorPublicId) {
      return log.actorPublicId
    }
    if (log.actorType === 'SYSTEM' || log.userId === 0) {
      return 'SYSTEM'
    }
    return log.actorType || 'USER'
  }

  return (
    <div>
      <PageHeader
        title="Audit Logs"
        description="Complete history of all actions in your business"
        icon={ScrollText}
      />

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-2 mb-5">
        <button
          onClick={() => {
            setEntityFilter(undefined)
            setPage(0)
          }}
          className={cn(
            'px-3 py-1.5 rounded-xl text-xs font-medium border transition-all',
            !entityFilter
              ? 'bg-indigo-600 border-indigo-600 text-white'
              : 'border-white/8 text-white/50 hover:text-white/80'
          )}
        >
          All
        </button>
        {ENTITY_TYPES.map((et) => (
          <button
            key={et}
            onClick={() => {
              setEntityFilter(et)
              setPage(0)
            }}
            className={cn(
              'px-3 py-1.5 rounded-xl text-xs font-medium border transition-all',
              entityFilter === et
                ? 'bg-indigo-600 border-indigo-600 text-white'
                : 'border-white/8 text-white/50 hover:text-white/80'
            )}
          >
            {et}
          </button>
        ))}
        <button
          onClick={() => refetch()}
          className="w-8 h-8 flex items-center justify-center rounded-xl border border-white/8 text-white/40 hover:text-white/80 hover:bg-white/5 transition-all ml-auto"
        >
          <RefreshCw className="w-3.5 h-3.5" />
        </button>
      </div>

      <ResponsiveCardList
        mobile={
          <div className="rounded-2xl border border-white/6 overflow-hidden bg-white/[0.01]">
            {isLoading ? (
              <div className="p-4">
                <TableSkeleton rows={8} cols={1} />
              </div>
            ) : data?.content.length === 0 ? (
              <div className="p-4">
                <EmptyState icon={ScrollText} title="No audit logs" description="Actions will appear here as your team works" />
              </div>
            ) : (
              <div className="divide-y divide-white/5">
                {(data?.content ?? []).map((log) => (
                  <div key={log.id} className="p-4">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span
                            className={cn(
                              'text-[10px] px-2 py-1 rounded-full font-medium',
                              ACTION_COLORS[log.action] || 'bg-white/5 text-white/40'
                            )}
                          >
                            {log.action.replace(/_/g, ' ')}
                          </span>
                          <span className="text-[10px] px-2 py-1 rounded-full bg-white/5 text-white/50">
                            {log.entityType}
                          </span>
                          {log.entityPublicId && (
                            <span className="text-[10px] px-2 py-1 rounded-full bg-indigo-500/10 text-indigo-300/85 font-medium">
                              {log.entityPublicId}
                            </span>
                          )}
                        </div>
                        <div className="mt-2 text-xs text-white/45">
                          {log.details || '—'}
                        </div>
                      </div>
                      <div className="text-[10px] text-white/35 whitespace-nowrap">
                        {formatDateTime(log.createdAt)}
                      </div>
                    </div>
                    <div className="mt-2 flex flex-wrap items-center gap-2 text-[10px] text-white/30">
                      <span>Actor: {formatActor(log)}</span>
                      {log.logId && <span className="px-1.5 py-0.5 rounded bg-white/5 text-white/35">{log.logId}</span>}
                      {log.ipAddress && <span>IP: {log.ipAddress}</span>}
                    </div>
                  </div>
                ))}

                <div className="flex items-center justify-between px-4 py-3">
                  <span className="text-xs text-white/35">
                    Page {page + 1} / {data?.totalPages ?? 1}
                  </span>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                      disabled={page === 0}
                      className="w-9 h-9 flex items-center justify-center rounded-xl border border-white/8 text-white/40 hover:text-white/80 hover:bg-white/5 disabled:opacity-30 transition-all"
                    >
                      <ChevronLeft className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => setPage((p) => p + 1)}
                      disabled={page >= (data?.totalPages ?? 1) - 1}
                      className="w-9 h-9 flex items-center justify-center rounded-xl border border-white/8 text-white/40 hover:text-white/80 hover:bg-white/5 disabled:opacity-30 transition-all"
                    >
                      <ChevronRight className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>
        }
        desktop={
          <div className="rounded-2xl border border-white/6 overflow-hidden bg-white/[0.01]">
            {isLoading ? (
              <div className="p-4">
                <TableSkeleton rows={10} cols={5} />
              </div>
            ) : data?.content.length === 0 ? (
              <EmptyState icon={ScrollText} title="No audit logs" description="Actions will appear here as your team works" />
            ) : (
              <>
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b border-white/5 bg-white/[0.02]">
                        {['Actor', 'Action', 'Entity', 'Details', 'Time'].map((h) => (
                          <th
                            key={h}
                            className="px-4 py-3 text-left text-xs font-medium text-white/40 whitespace-nowrap"
                          >
                            {h}
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {data?.content.map((log) => (
                        <tr
                          key={log.id}
                          className="border-b border-white/[0.03] hover:bg-white/[0.02] transition-colors"
                        >
                          <td className="px-4 py-3">
                            <div className="flex items-center gap-2">
                              <span className="text-sm text-white/60">{formatActor(log)}</span>
                              {log.logId && (
                                <span className="text-[10px] px-1.5 py-0.5 rounded bg-white/5 text-white/35">{log.logId}</span>
                              )}
                            </div>
                          </td>
                          <td className="px-4 py-3">
                            <span
                              className={cn(
                                'text-xs px-2.5 py-1 rounded-full font-medium',
                                ACTION_COLORS[log.action] || 'bg-white/5 text-white/40'
                              )}
                            >
                              {log.action.replace(/_/g, ' ')}
                            </span>
                          </td>
                          <td className="px-4 py-3">
                            <div className="flex items-center gap-2 flex-wrap">
                              <span className="text-xs px-2 py-0.5 rounded bg-white/5 text-white/50">
                                {log.entityType}
                              </span>
                              {log.entityPublicId && (
                                <span className="text-[10px] px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-300/85 font-medium">
                                  {log.entityPublicId}
                                </span>
                              )}
                            </div>
                          </td>
                          <td className="px-4 py-3">
                            <div className="text-sm text-white/55">
                              <div>{log.details || '—'}</div>
                              {log.ipAddress && <div className="text-[10px] text-white/30 mt-0.5">IP: {log.ipAddress}</div>}
                            </div>
                          </td>
                          <td className="px-4 py-3 whitespace-nowrap">
                            <span className="text-xs text-white/40">
                              {formatDateTime(log.createdAt)}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                {/* Pagination */}
                <div className="flex items-center justify-between px-4 py-3 border-t border-white/5">
                  <span className="text-xs text-white/35">
                    {data?.totalElements ?? 0} total entries
                  </span>
                  <div className="flex items-center gap-1.5">
                    <button
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                      disabled={page === 0}
                      className="w-7 h-7 flex items-center justify-center rounded-lg border border-white/8 text-white/50 hover:text-white hover:bg-white/5 disabled:opacity-30 transition-all"
                    >
                      <ChevronLeft className="w-3.5 h-3.5" />
                    </button>
                    <span className="text-xs text-white/40 px-2">
                      {page + 1} / {data?.totalPages ?? 1}
                    </span>
                    <button
                      onClick={() => setPage((p) => p + 1)}
                      disabled={page >= (data?.totalPages ?? 1) - 1}
                      className="w-7 h-7 flex items-center justify-center rounded-lg border border-white/8 text-white/50 hover:text-white hover:bg-white/5 disabled:opacity-30 transition-all"
                    >
                      <ChevronRight className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </>
            )}
          </div>
        }
      />
    </div>
  )
}