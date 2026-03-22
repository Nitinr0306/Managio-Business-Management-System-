'use client'

import { useParams } from 'next/navigation'
import { useState } from 'react'
import { Mail, RefreshCw, Trash2, Clock, CheckCircle } from 'lucide-react'
import { PageHeader } from '@/components/shared/PageHeader'
import { EmptyState, TableSkeleton } from '@/components/shared/EmptyState'
import { useStaffInvitations, usePendingInvitations } from '@/lib/hooks/useStaff'
import { staffApi } from '@/lib/api/staff'
import { toast } from 'sonner'
import { formatDateTime, formatRelative } from '@/lib/utils/formatters'
import { cn } from '@/lib/utils/cn'
import { getErrorMessage } from '@/lib/utils/errors'

export default function StaffInvitationsPage() {
  const { id: businessId } = useParams<{ id: string }>()
  const [page, setPage] = useState(0)
  const [mode, setMode] = useState<'pending' | 'all'>('pending')
  const [busyId, setBusyId] = useState<string | null>(null)

  const pendingQuery = usePendingInvitations(businessId)
  const allQuery = useStaffInvitations(businessId, { page, size: 20 })

  const items =
    mode === 'pending'
      ? (pendingQuery.data ?? [])
      : (allQuery.data?.content ?? [])

  const isLoading = mode === 'pending' ? pendingQuery.isLoading : allQuery.isLoading

  const resend = async (invId: string) => {
    setBusyId(invId)
    try {
      await staffApi.resendInvitation(businessId, invId)
      toast.success('Invitation resent')
      pendingQuery.refetch()
      allQuery.refetch()
    } catch (e: unknown) {
      toast.error(getErrorMessage(e, 'Failed to resend invitation'))
    } finally {
      setBusyId(null)
    }
  }

  const cancel = async (invId: string) => {
    setBusyId(invId)
    try {
      await staffApi.cancelInvitation(businessId, invId)
      toast.success('Invitation cancelled')
      pendingQuery.refetch()
      allQuery.refetch()
    } catch (e: unknown) {
      toast.error(getErrorMessage(e, 'Failed to cancel invitation'))
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div>
      <PageHeader
        title="Staff Invitations"
        description={mode === 'pending' ? 'Pending invitations' : 'All invitations'}
        icon={Mail}
        actions={
          <button
            onClick={() => {
              if (mode === 'pending') pendingQuery.refetch()
              else allQuery.refetch()
            }}
            className="w-9 h-9 flex items-center justify-center rounded-xl border border-white/8 text-white/40 hover:text-white/80 hover:bg-white/5 transition-all"
            title="Refresh"
          >
            <RefreshCw className="w-3.5 h-3.5" />
          </button>
        }
      />

      <div className="flex items-center gap-1 bg-white/4 border border-white/8 rounded-xl p-1 mb-5 w-fit">
        {(['pending', 'all'] as const).map((m) => (
          <button
            key={m}
            onClick={() => {
              setMode(m)
              setPage(0)
            }}
            className={cn(
              'px-4 py-2 rounded-lg text-sm font-medium capitalize transition-all',
              mode === m ? 'bg-indigo-600 text-white' : 'text-white/50 hover:text-white/80'
            )}
          >
            {m}
          </button>
        ))}
      </div>

      <div className="rounded-2xl border border-white/6 overflow-hidden bg-white/[0.01]">
        {isLoading ? (
          <div className="p-4">
            <TableSkeleton rows={8} cols={6} />
          </div>
        ) : items.length === 0 ? (
          <EmptyState
            icon={Mail}
            title="No invitations"
            description={
              mode === 'pending'
                ? 'No pending invitations right now'
                : 'No invitations have been sent yet'
            }
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-white/5 bg-white/[0.02]">
                  {['Email', 'Role', 'Status', 'Expires', 'Created', ''].map((h) => (
                    <th key={h} className="px-4 py-3 text-left text-xs font-medium text-white/40 whitespace-nowrap">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {items.map((inv: any) => (
                  <tr key={inv.id} className="border-b border-white/[0.03] hover:bg-white/[0.02] transition-colors">
                    <td className="px-4 py-3">
                      <div className="text-sm font-medium text-white/80">{inv.email}</div>
                      {inv.invitedByUserEmail && (
                        <div className="text-xs text-white/35">Invited by {inv.invitedByUserEmail}</div>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-white/60">{inv.role}</span>
                    </td>
                    <td className="px-4 py-3">
                      {inv.used ? (
                        <span className="inline-flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-full bg-emerald-500/15 text-emerald-400">
                          <CheckCircle className="w-3 h-3" /> Accepted
                        </span>
                      ) : inv.expired ? (
                        <span className="inline-flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-full bg-red-500/15 text-red-400">
                          <Clock className="w-3 h-3" /> Expired
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-full bg-amber-500/15 text-amber-400">
                          <Clock className="w-3 h-3" /> Pending
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <div className="text-xs text-white/50">{inv.expiresAt ? formatDateTime(inv.expiresAt) : '—'}</div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="text-xs text-white/50">{formatRelative(inv.createdAt)}</div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2 justify-end">
                        {!inv.used && (
                          <button
                            disabled={!!busyId}
                            onClick={() => resend(inv.id)}
                            className="text-xs text-indigo-400 hover:text-indigo-300 disabled:opacity-50 transition-colors"
                          >
                            Resend
                          </button>
                        )}
                        {!inv.used && (
                          <button
                            disabled={!!busyId}
                            onClick={() => cancel(inv.id)}
                            className="inline-flex items-center gap-1 text-xs text-white/30 hover:text-red-400 disabled:opacity-50 transition-colors"
                          >
                            <Trash2 className="w-3 h-3" /> Cancel
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}

