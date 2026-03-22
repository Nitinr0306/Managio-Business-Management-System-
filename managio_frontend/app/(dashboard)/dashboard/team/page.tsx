'use client'

import Link from 'next/link'
import { useState } from 'react'
import { Users, Mail, Shield, RefreshCw } from 'lucide-react'
import { PageHeader } from '@/components/shared/PageHeader'
import { EmptyState, TableSkeleton } from '@/components/shared/EmptyState'
import { useBusinessStore } from '@/lib/store/businessStore'
import { useStaff, usePendingInvitations } from '@/lib/hooks/useStaff'
import { staffApi } from '@/lib/api/staff'
import { toast } from 'sonner'
import { cn } from '@/lib/utils/cn'
import type { Permission, Staff } from '@/lib/types/staff'

const CORE_PERMS: { label: string; perm: Permission; hint: string }[] = [
  { label: 'Members', perm: 'VIEW_MEMBERS', hint: 'Member list access' },
  { label: 'Payments', perm: 'VIEW_PAYMENTS', hint: 'Payment list access' },
  { label: 'Subscriptions', perm: 'VIEW_SUBSCRIPTIONS', hint: 'Subscription list access' },
  { label: 'Reports', perm: 'VIEW_REPORTS', hint: 'Reporting access' },
]

export default function TeamPage() {
  const businessId = useBusinessStore((s) => (s.currentBusiness?.id ? String(s.currentBusiness.id) : ''))
  const [page, setPage] = useState(0)
  const [busy, setBusy] = useState<string | null>(null)

  const staffQuery = useStaff(businessId, { page, size: 20 })
  const pendingInvitesQuery = usePendingInvitations(businessId)

  const staff = staffQuery.data?.content ?? []
  const pendingInvites = pendingInvitesQuery.data ?? []

  const togglePermission = async (staffId: string, perm: Permission, has: boolean) => {
    setBusy(`${staffId}:${perm}`)
    try {
      if (has) await staffApi.revokePermission(businessId, staffId, perm)
      else await staffApi.grantPermission(businessId, staffId, perm)
      toast.success('Permissions updated')
    } catch (e: any) {
      toast.error(e?.response?.data?.message || 'Failed to update permission')
    } finally {
      setBusy(null)
      staffQuery.refetch()
    }
  }

  return (
    <div>
      <PageHeader
        title="Team"
        description="Manage staff members, invitations, and permissions"
        icon={Users}
        actions={
          <div className="flex items-center gap-2">
            <Link
              href={`/businesses/${businessId}/staff/invite`}
              className="flex items-center gap-2 px-4 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-xl transition-all shadow-lg shadow-indigo-600/25"
            >
              <Mail className="w-4 h-4" /> Invite staff
            </Link>
            <button
              onClick={() => { staffQuery.refetch(); pendingInvitesQuery.refetch() }}
              className="w-9 h-9 flex items-center justify-center rounded-xl border border-white/[0.08] text-white/40 hover:text-white/80 hover:bg-white/[0.04] transition-all"
              title="Refresh"
            >
              <RefreshCw className="w-3.5 h-3.5" />
            </button>
          </div>
        }
      />

      {!businessId ? (
        <EmptyState
          icon={Users}
          title="Select a business"
          description="Choose a business from the sidebar to manage its team."
        />
      ) : staffQuery.isLoading ? (
        <div className="rounded-2xl border border-white/[0.06] overflow-hidden bg-white/[0.01] p-4">
          <TableSkeleton rows={8} cols={6} />
        </div>
      ) : (
        <div className="space-y-6">
          {/* Pending invitations */}
          <div className="p-5 rounded-2xl border border-white/[0.06] bg-white/[0.01]">
            <div className="flex items-center justify-between mb-3">
              <div className="text-sm font-display font-700 text-white/80 flex items-center gap-2">
                <Mail className="w-4 h-4 text-indigo-400" /> Pending invitations
              </div>
              <Link href={`/businesses/${businessId}/staff/invitations`} className="text-xs text-indigo-400 hover:text-indigo-300">
                Manage invitations →
              </Link>
            </div>
            {pendingInvites.length === 0 ? (
              <div className="text-sm text-white/35">No pending invitations.</div>
            ) : (
              <div className="space-y-2">
                {pendingInvites.slice(0, 5).map((inv) => (
                  <div key={inv.id} className="flex items-center justify-between px-3 py-2 rounded-xl bg-white/[0.02] border border-white/[0.06]">
                    <div>
                      <div className="text-sm text-white/75">{inv.email}</div>
                      <div className="text-xs text-white/35">{inv.role} • expires {new Date(inv.expiresAt).toLocaleString()}</div>
                    </div>
                    <button
                      onClick={async () => {
                        setBusy(`invite:${inv.id}`)
                        try {
                          await staffApi.resendInvitation(businessId, inv.id)
                          toast.success('Invitation resent')
                        } catch (e: any) {
                          toast.error(e?.response?.data?.message || 'Failed to resend')
                        } finally {
                          setBusy(null)
                          pendingInvitesQuery.refetch()
                        }
                      }}
                      disabled={busy === `invite:${inv.id}`}
                      className="text-xs px-3 py-2 rounded-xl border border-white/[0.1] text-white/60 hover:text-white hover:bg-white/[0.04] disabled:opacity-50 transition-all"
                    >
                      Resend
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Staff table */}
          <div className="rounded-2xl border border-white/[0.06] overflow-hidden bg-white/[0.01]">
            {staff.length === 0 ? (
              <EmptyState
                icon={Users}
                title="No staff members"
                description="Invite or add staff members to start delegating operations."
              />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-white/[0.05] bg-white/[0.02]">
                      {['Name', 'Email', 'Role', 'Status', 'Access', 'Actions'].map((h) => (
                        <th key={h} className="px-4 py-3 text-left text-xs font-medium text-white/40 whitespace-nowrap">
                          {h}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {staff.map((s: Staff) => (
                      <tr key={s.id} className="border-b border-white/[0.03] hover:bg-white/[0.02] transition-colors">
                        <td className="px-4 py-3">
                          <div className="text-sm font-medium text-white/80">{s.userName}</div>
                        </td>
                        <td className="px-4 py-3">
                          <div className="text-sm text-white/60">{s.userEmail}</div>
                        </td>
                        <td className="px-4 py-3">
                          <span className="text-xs px-2.5 py-1 rounded-full bg-white/5 text-white/50">{s.role}</span>
                        </td>
                        <td className="px-4 py-3">
                          <span
                            className={cn(
                              'text-xs px-2.5 py-1 rounded-full font-medium',
                              s.status === 'ACTIVE'
                                ? 'bg-emerald-500/15 text-emerald-400'
                                : s.status === 'SUSPENDED'
                                ? 'bg-amber-500/15 text-amber-400'
                                : 'bg-red-500/15 text-red-400'
                            )}
                          >
                            {s.status}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex flex-wrap gap-1.5">
                            {CORE_PERMS.map(({ label, perm }) => {
                              // We don't have effectivePermissions on list rows; use capability booleans as coarse toggles.
                              // Permission toggles are still applied via backend permission endpoints.
                              const has =
                                (perm === 'VIEW_MEMBERS' && s.canManageMembers) ||
                                (perm === 'VIEW_PAYMENTS' && s.canManagePayments) ||
                                (perm === 'VIEW_SUBSCRIPTIONS' && s.canManageSubscriptions) ||
                                (perm === 'VIEW_REPORTS' && s.canViewReports)

                              return (
                                <button
                                  key={perm}
                                  disabled={busy === `${s.id}:${perm}`}
                                  onClick={() => togglePermission(String(s.id), perm, has)}
                                  className={cn(
                                    'text-[10px] px-2 py-1 rounded-full border transition-all inline-flex items-center gap-1',
                                    has
                                      ? 'border-emerald-500/25 bg-emerald-500/10 text-emerald-300 hover:bg-emerald-500/15'
                                      : 'border-white/[0.1] bg-white/[0.01] text-white/45 hover:bg-white/[0.03]'
                                  )}
                                >
                                  <Shield className="w-3 h-3" /> {label}
                                </button>
                              )
                            })}
                          </div>
                        </td>
                        <td className="px-4 py-3">
                          <Link
                            href={`/businesses/${businessId}/staff/${s.id}`}
                            className="text-xs text-indigo-400 hover:text-indigo-300"
                          >
                            Manage →
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

