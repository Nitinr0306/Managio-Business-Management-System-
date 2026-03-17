'use client'

import { useParams } from 'next/navigation'
import Link from 'next/link'
import { CreditCard, Plus, Calendar, ChevronLeft, ChevronRight } from 'lucide-react'
import { useState } from 'react'
import { useSubscriptions } from '@/lib/hooks/useSubscriptions'
import { subscriptionsApi } from '@/lib/api/subscriptions'
import { PageHeader } from '@/components/shared/PageHeader'
import { EmptyState, TableSkeleton } from '@/components/shared/EmptyState'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { ResponsiveCardList } from '@/components/shared/ResponsiveCardList'
import { formatCurrency, formatDate, getDaysRemaining } from '@/lib/utils/formatters'
import { cn } from '@/lib/utils/cn'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { MemberSubscription } from '@/lib/types/subscription'

export default function SubscriptionsPage() {
  const { id: businessId } = useParams<{ id: string }>()
  const [page, setPage] = useState(0)
  const [cancelTarget, setCancelTarget] = useState<MemberSubscription | null>(null)
  const [cancelling, setCancelling] = useState(false)
  const qc = useQueryClient()

  const { data, isLoading } = useSubscriptions(businessId, { page, size: 20 })

  return (
    <div>
      <PageHeader
        title="Subscriptions"
        description={`${data?.totalElements ?? 0} subscriptions`}
        icon={CreditCard}
        actions={
          <div className="flex items-center gap-2">
            <Link
              href={`/businesses/${businessId}/subscriptions/plans`}
              className="flex items-center gap-2 px-4 py-2.5 border border-white/10 text-white/70 hover:text-white hover:bg-white/5 text-sm rounded-xl transition-all"
            >
              <Calendar className="w-4 h-4" /> Manage Plans
            </Link>
            <Link
              href={`/businesses/${businessId}/subscriptions/assign`}
              className="flex items-center gap-2 px-4 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-xl transition-all shadow-lg shadow-indigo-600/25"
            >
              <Plus className="w-4 h-4" /> Assign Subscription
            </Link>
          </div>
        }
      />

      <ResponsiveCardList
        mobile={
          <div className="rounded-2xl border border-white/6 overflow-hidden bg-white/[0.01]">
            {isLoading ? (
              <div className="p-4"><TableSkeleton rows={6} cols={1} /></div>
            ) : data?.content.length === 0 ? (
              <div className="p-4">
                <EmptyState
                  icon={CreditCard}
                  title="No subscriptions yet"
                  description="Assign a subscription plan to your members"
                  action={
                    <Link
                      href={`/businesses/${businessId}/subscriptions/assign`}
                      className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm rounded-xl transition-all"
                    >
                      <Plus className="w-4 h-4" /> Assign Subscription
                    </Link>
                  }
                />
              </div>
            ) : (
              <div className="divide-y divide-white/5">
                {(data?.content ?? []).map((sub) => {
                  const daysLeft = getDaysRemaining(sub.endDate)
                  return (
                    <div key={String(sub.id)} className="p-4">
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <div className="text-sm font-medium text-white/85 truncate">{sub.memberName || '—'}</div>
                          <div className="text-xs text-white/40 truncate">{sub.memberEmail || '—'}</div>
                          <div className="mt-2 text-xs text-white/50">
                            {sub.planName} • {formatDate(sub.startDate)} → {formatDate(sub.endDate)}
                          </div>
                          {sub.amount != null && (
                            <div className="mt-2 text-sm font-display font-600 text-amber-400">
                              {formatCurrency(sub.amount)}
                            </div>
                          )}
                        </div>

                        <div className="flex flex-col items-end gap-2 flex-shrink-0">
                          <span
                            className={cn(
                              'text-[10px] px-2 py-1 rounded-full font-medium',
                              sub.status === 'ACTIVE' ? 'bg-emerald-500/15 text-emerald-400' :
                              sub.status === 'EXPIRED' ? 'bg-red-500/15 text-red-400' :
                              sub.status === 'CANCELLED' ? 'bg-white/5 text-white/40' :
                              'bg-amber-500/15 text-amber-400'
                            )}
                          >
                            {sub.status}
                          </span>
                          <span
                            className={cn(
                              'text-xs font-medium',
                              daysLeft < 0 ? 'text-red-400' : daysLeft <= 7 ? 'text-amber-400' : 'text-white/50'
                            )}
                          >
                            {daysLeft < 0 ? 'Expired' : `${daysLeft}d left`}
                          </span>
                        </div>
                      </div>

                      {sub.status === 'ACTIVE' && (
                        <div className="mt-3 flex justify-end">
                          <button
                            onClick={() => setCancelTarget(sub)}
                            className="px-3 py-2 rounded-xl border border-red-500/20 text-xs text-red-300 hover:bg-red-500/10"
                          >
                            Cancel
                          </button>
                        </div>
                      )}
                    </div>
                  )
                })}

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
              <div className="p-4"><TableSkeleton rows={6} cols={6} /></div>
            ) : data?.content.length === 0 ? (
              <EmptyState
                icon={CreditCard}
                title="No subscriptions yet"
                description="Assign a subscription plan to your members"
                action={
                  <Link
                    href={`/businesses/${businessId}/subscriptions/assign`}
                    className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm rounded-xl transition-all"
                  >
                    <Plus className="w-4 h-4" /> Assign Subscription
                  </Link>
                }
              />
            ) : (
              <>
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b border-white/5 bg-white/[0.02]">
                        {['Member', 'Plan', 'Period', 'Amount', 'Status', 'Days Left', ''].map(h => (
                          <th key={h} className="px-4 py-3 text-left text-xs font-medium text-white/40">{h}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {data?.content.map(sub => {
                        const daysLeft = getDaysRemaining(sub.endDate)
                        return (
                          <tr key={sub.id} className="border-b border-white/[0.03] hover:bg-white/[0.02] transition-colors">
                            <td className="px-4 py-3">
                              <div className="flex items-center gap-2.5">
                                <div className="w-7 h-7 rounded-full bg-indigo-600/20 flex items-center justify-center text-xs font-bold text-indigo-300">
                                  {(sub.memberName || '?').slice(0, 2).toUpperCase()}
                                </div>
                                <div>
                                  <div className="text-sm font-medium text-white/80">{sub.memberName}</div>
                                  {sub.memberEmail && (
                                    <div className="text-xs text-white/35">{sub.memberEmail}</div>
                                  )}
                                </div>
                              </div>
                            </td>
                            <td className="px-4 py-3">
                              <span className="text-sm text-white/70">{sub.planName}</span>
                            </td>
                            <td className="px-4 py-3">
                              <div className="text-xs text-white/50">
                                {formatDate(sub.startDate)} → {formatDate(sub.endDate)}
                              </div>
                            </td>
                            <td className="px-4 py-3">
                              {sub.amount != null && (
                                <span className="text-sm font-display font-600 text-amber-400">
                                  {formatCurrency(sub.amount)}
                                </span>
                              )}
                            </td>
                            <td className="px-4 py-3">
                              <span className={cn(
                                'text-xs px-2.5 py-1 rounded-full font-medium',
                                sub.status === 'ACTIVE' ? 'bg-emerald-500/15 text-emerald-400' :
                                sub.status === 'EXPIRED' ? 'bg-red-500/15 text-red-400' :
                                sub.status === 'CANCELLED' ? 'bg-white/5 text-white/40' :
                                'bg-amber-500/15 text-amber-400'
                              )}>
                                {sub.status}
                              </span>
                            </td>
                            <td className="px-4 py-3">
                              <span className={cn(
                                'text-xs font-medium',
                                daysLeft < 0 ? 'text-red-400' :
                                daysLeft <= 7 ? 'text-amber-400' : 'text-white/50'
                              )}>
                                {daysLeft < 0 ? 'Expired' : `${daysLeft}d`}
                              </span>
                            </td>
                            <td className="px-4 py-3">
                              {sub.status === 'ACTIVE' && (
                                <button
                                  onClick={() => setCancelTarget(sub)}
                                  className="text-xs text-white/30 hover:text-red-400 transition-colors"
                                >
                                  Cancel
                                </button>
                              )}
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>

                {/* Pagination */}
                <div className="flex items-center justify-between px-4 py-3 border-t border-white/5">
                  <span className="text-xs text-white/35">{data?.totalElements ?? 0} total</span>
                  <div className="flex items-center gap-1.5">
                    <button
                      onClick={() => setPage(p => Math.max(0, p - 1))}
                      disabled={page === 0}
                      className="w-7 h-7 flex items-center justify-center rounded-lg border border-white/8 text-white/50 hover:text-white hover:bg-white/5 disabled:opacity-30 transition-all"
                    >
                      <ChevronLeft className="w-3.5 h-3.5" />
                    </button>
                    <span className="text-xs text-white/40 px-2">{page + 1} / {data?.totalPages ?? 1}</span>
                    <button
                      onClick={() => setPage(p => p + 1)}
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

      <ConfirmDialog
        open={!!cancelTarget}
        onClose={() => setCancelTarget(null)}
        onConfirm={async () => {
          if (!cancelTarget) return
          setCancelling(true)
          try {
            await subscriptionsApi.cancelSubscription(businessId, String(cancelTarget.id))
            toast.success('Subscription cancelled')
            qc.invalidateQueries({ queryKey: ['subscriptions', businessId] })
          } catch {
            toast.error('Failed to cancel')
          } finally {
            setCancelling(false)
            setCancelTarget(null)
          }
        }}
        title="Cancel Subscription"
        description={`Cancel ${cancelTarget?.planName} for ${cancelTarget?.memberName}?`}
        confirmLabel="Cancel Subscription"
        loading={cancelling}
      />
    </div>
  )
}