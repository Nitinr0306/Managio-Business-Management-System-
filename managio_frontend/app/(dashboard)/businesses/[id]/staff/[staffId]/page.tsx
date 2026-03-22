'use client'

import { useParams } from 'next/navigation'
import Link from 'next/link'
import { ArrowLeft, Mail, Phone, UserCog, Calendar, Shield, PauseCircle, PlayCircle, UserX } from 'lucide-react'
import { motion } from 'framer-motion'
import { useStaffDetail, useSuspendStaff, useActivateStaff, useTerminateStaff } from '@/lib/hooks/useStaff'
import { LoadingSpinner } from '@/components/shared/EmptyState'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { formatDate } from '@/lib/utils/formatters'
import { cn } from '@/lib/utils/cn'
import { useState } from 'react'
import { staffApi } from '@/lib/api/staff'
import { useQueryClient } from '@tanstack/react-query'
import type { Permission } from '@/lib/types/staff'

const ROLE_COLORS: Record<string, string> = {
  MANAGER: 'bg-indigo-500/15 text-indigo-400',
  TRAINER: 'bg-emerald-500/15 text-emerald-400',
  RECEPTIONIST: 'bg-amber-500/15 text-amber-400',
  ACCOUNTANT: 'bg-cyan-500/15 text-cyan-400',
  STAFF: 'bg-white/5 text-white/50',
  OWNER: 'bg-violet-500/15 text-violet-400',
}

const STATUS_COLORS: Record<string, string> = {
  ACTIVE: 'bg-emerald-500/15 text-emerald-400',
  INACTIVE: 'bg-white/5 text-white/40',
  SUSPENDED: 'bg-amber-500/15 text-amber-400',
  TERMINATED: 'bg-red-500/15 text-red-400',
  ON_LEAVE: 'bg-blue-500/15 text-blue-400',
}

export default function StaffDetailPage() {
  const { id: businessId, staffId } = useParams<{ id: string; staffId: string }>()
  const { data: staff, isLoading } = useStaffDetail(businessId, staffId)
  const suspendMutation = useSuspendStaff(businessId)
  const activateMutation = useActivateStaff(businessId)
  const terminateMutation = useTerminateStaff(businessId)
  const qc = useQueryClient()

  const [confirmSuspend, setConfirmSuspend] = useState(false)
  const [confirmActivate, setConfirmActivate] = useState(false)
  const [confirmTerminate, setConfirmTerminate] = useState(false)
  const [busyPerm, setBusyPerm] = useState<string | null>(null)

  if (isLoading) return <LoadingSpinner />
  if (!staff) return <div className="text-white/50 text-sm">Staff member not found</div>

  const ALL_PERMISSIONS: Permission[] = [
    'VIEW_MEMBERS','ADD_MEMBERS','EDIT_MEMBERS','DELETE_MEMBERS','EDIT_MEMBER_NOTES','IMPORT_MEMBERS','EXPORT_MEMBERS',
    'VIEW_SUBSCRIPTIONS','ASSIGN_SUBSCRIPTIONS','CANCEL_SUBSCRIPTIONS','EXTEND_SUBSCRIPTIONS','VIEW_SUBSCRIPTION_HISTORY',
    'VIEW_PAYMENTS','RECORD_PAYMENTS','REFUND_PAYMENTS','VIEW_PAYMENT_HISTORY','EXPORT_PAYMENTS',
    'VIEW_REPORTS','VIEW_DASHBOARD','EXPORT_DATA','VIEW_BUSINESS_STATS',
    'VIEW_STAFF','ADD_STAFF','EDIT_STAFF','REMOVE_STAFF','VIEW_AUDIT_LOGS',
    'RECORD_ATTENDANCE','VIEW_ATTENDANCE',
    'MANAGE_BUSINESS_SETTINGS','MANAGE_SUBSCRIPTION_PLANS','ACCESS_API',
  ]

  const effective = new Set<string>((staff.effectivePermissions || []) as string[])

  const capabilities = [
    { label: 'Manage Members', value: staff.canManageMembers },
    { label: 'Manage Payments', value: staff.canManagePayments },
    { label: 'Manage Subscriptions', value: staff.canManageSubscriptions },
    { label: 'View Reports', value: staff.canViewReports },
    { label: 'Can Login', value: staff.canLogin },
  ]

  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-6">
        <Link
          href={`/businesses/${businessId}/staff`}
          className="inline-flex items-center gap-2 text-sm text-white/50 hover:text-white/80 transition-colors mb-4"
        >
          <ArrowLeft className="w-4 h-4" /> Back to staff
        </Link>
      </div>

      {/* Header card */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        className="p-6 rounded-2xl border border-white/6 bg-white/[0.02] mb-6"
      >
        <div className="flex items-start justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 rounded-2xl bg-violet-600/20 border border-violet-500/20 flex items-center justify-center text-xl font-display font-700 text-violet-300">
              {(staff.userName || 'ST').slice(0, 2).toUpperCase()}
            </div>
            <div>
              <h1 className="text-2xl font-display font-700 mb-1">{staff.userName}</h1>
              <div className="flex flex-wrap gap-3 text-sm text-white/45">
                {staff.userEmail && (
                  <span className="flex items-center gap-1">
                    <Mail className="w-3 h-3" />{staff.userEmail}
                  </span>
                )}
                {staff.phone && (
                  <span className="flex items-center gap-1">
                    <Phone className="w-3 h-3" />{staff.phone}
                  </span>
                )}
              </div>
              <div className="flex items-center gap-2 mt-2">
                <span className={cn('text-xs px-2.5 py-1 rounded-full font-medium', ROLE_COLORS[staff.role] || ROLE_COLORS.STAFF)}>
                  {staff.role}
                </span>
                <span className={cn('text-xs px-2.5 py-1 rounded-full font-medium', STATUS_COLORS[staff.status] || STATUS_COLORS.INACTIVE)}>
                  {staff.status}
                </span>
              </div>
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex items-center gap-2">
            {staff.status === 'ACTIVE' && (
              <button
                onClick={() => setConfirmSuspend(true)}
                className="flex items-center gap-1.5 px-3 py-2 rounded-xl border border-amber-500/20 text-amber-400 text-xs hover:bg-amber-500/10 transition-all"
              >
                <PauseCircle className="w-3.5 h-3.5" /> Suspend
              </button>
            )}
            {staff.status === 'SUSPENDED' && (
              <button
                onClick={() => setConfirmActivate(true)}
                className="flex items-center gap-1.5 px-3 py-2 rounded-xl border border-emerald-500/20 text-emerald-400 text-xs hover:bg-emerald-500/10 transition-all"
              >
                <PlayCircle className="w-3.5 h-3.5" /> Activate
              </button>
            )}
            {staff.status !== 'TERMINATED' && (
              <button
                onClick={() => setConfirmTerminate(true)}
                className="flex items-center gap-1.5 px-3 py-2 rounded-xl border border-red-500/20 text-red-400 text-xs hover:bg-red-500/10 transition-all"
              >
                <UserX className="w-3.5 h-3.5" /> Terminate
              </button>
            )}
          </div>
        </div>
      </motion.div>

      <div className="grid md:grid-cols-2 gap-4">
        {/* Details */}
        <div className="p-5 rounded-2xl border border-white/6 bg-white/[0.02] space-y-3">
          <h3 className="text-sm font-display font-600 text-white/70 flex items-center gap-2">
            <UserCog className="w-4 h-4 text-indigo-400" /> Staff Details
          </h3>
          {[
            { label: 'Department', value: staff.department || '—' },
            { label: 'Designation', value: staff.designation || '—' },
            { label: 'Employee ID', value: staff.employeeId || '—' },
            { label: 'Hire Date', value: staff.hireDate ? formatDate(staff.hireDate) : '—' },
            { label: 'Termination Date', value: staff.terminationDate ? formatDate(staff.terminationDate) : '—' },
          ].map(d => (
            <div key={d.label} className="flex justify-between text-sm">
              <span className="text-white/40">{d.label}</span>
              <span className="text-white/70">{d.value}</span>
            </div>
          ))}
        </div>

        {/* Capabilities */}
        <div className="p-5 rounded-2xl border border-white/6 bg-white/[0.02] space-y-3">
          <h3 className="text-sm font-display font-600 text-white/70 flex items-center gap-2">
            <Shield className="w-4 h-4 text-indigo-400" /> Capabilities
          </h3>
          {capabilities.map(c => (
            <div key={c.label} className="flex justify-between items-center text-sm">
              <span className="text-white/40">{c.label}</span>
              <span className={cn(
                'text-xs px-2 py-0.5 rounded-full',
                c.value ? 'bg-emerald-500/15 text-emerald-400' : 'bg-white/5 text-white/30'
              )}>
                {c.value ? 'Yes' : 'No'}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Permissions */}
      <div className="p-5 rounded-2xl border border-white/6 bg-white/[0.02] mt-4">
        <h3 className="text-sm font-display font-600 text-white/70 flex items-center gap-2 mb-3">
          <Shield className="w-4 h-4 text-indigo-400" /> Permissions
        </h3>
        <div className="grid md:grid-cols-2 gap-2">
          {ALL_PERMISSIONS.map((perm) => {
            const on = effective.has(perm)
            return (
              <button
                key={perm}
                disabled={!!busyPerm}
                onClick={async () => {
                  setBusyPerm(perm)
                  try {
                    if (on) await staffApi.revokePermission(businessId, staffId, perm)
                    else await staffApi.grantPermission(businessId, staffId, perm)
                    await qc.invalidateQueries({ queryKey: ['staff-detail', businessId, staffId] })
                    await qc.invalidateQueries({ queryKey: ['staff-member', businessId, staffId] })
                    await qc.invalidateQueries({ queryKey: ['staff', businessId] })
                  } finally {
                    setBusyPerm(null)
                  }
                }}
                className={cn(
                  'flex items-center justify-between px-3 py-2 rounded-xl border transition-all text-left',
                  on
                    ? 'border-emerald-500/20 bg-emerald-500/5 hover:bg-emerald-500/10'
                    : 'border-white/8 bg-white/[0.01] hover:bg-white/[0.03]'
                )}
              >
                <span className="text-xs text-white/65">{perm}</span>
                <span
                  className={cn(
                    'text-[10px] px-2 py-0.5 rounded-full',
                    on ? 'bg-emerald-500/15 text-emerald-400' : 'bg-white/5 text-white/35'
                  )}
                >
                  {on ? 'ON' : 'OFF'}
                </span>
              </button>
            )
          })}
        </div>
      </div>

      {staff.notes && (
        <div className="p-5 rounded-2xl border border-white/6 bg-white/[0.02] mt-4">
          <h3 className="text-sm font-display font-600 text-white/70 mb-2">Notes</h3>
          <p className="text-sm text-white/50">{staff.notes}</p>
        </div>
      )}

      {/* Dialogs */}
      <ConfirmDialog
        open={confirmSuspend}
        onClose={() => setConfirmSuspend(false)}
        onConfirm={async () => {
          try { await suspendMutation.mutateAsync(staffId) }
          finally { setConfirmSuspend(false) }
        }}
        title="Suspend Staff Member"
        description={`Suspend ${staff.userName}? They will lose system access.`}
        confirmLabel="Suspend"
        variant="warning"
        loading={suspendMutation.isPending}
      />
      <ConfirmDialog
        open={confirmActivate}
        onClose={() => setConfirmActivate(false)}
        onConfirm={async () => {
          try { await activateMutation.mutateAsync(staffId) }
          finally { setConfirmActivate(false) }
        }}
        title="Activate Staff Member"
        description={`Reactivate ${staff.userName}? They will regain access.`}
        confirmLabel="Activate"
        variant="warning"
        loading={activateMutation.isPending}
      />
      <ConfirmDialog
        open={confirmTerminate}
        onClose={() => setConfirmTerminate(false)}
        onConfirm={async () => {
          try {
            await terminateMutation.mutateAsync({ staffId, terminationDate: new Date().toISOString().slice(0, 10) })
          } finally {
            setConfirmTerminate(false)
          }
        }}
        title="Terminate Staff Member"
        description={`Permanently terminate ${staff.userName}? This cannot be undone.`}
        confirmLabel="Terminate"
        loading={terminateMutation.isPending}
      />
    </div>
  )
}