'use client'

import { useState, useMemo } from 'react'
import { useParams } from 'next/navigation'
import { useReactTable, getCoreRowModel, flexRender, ColumnDef } from '@tanstack/react-table'
import { UserCog, Plus, Search, Mail, ChevronLeft, ChevronRight, RefreshCw, Eye, UserX, PauseCircle } from 'lucide-react'
import Link from 'next/link'
import { useStaff, useSuspendStaff, useTerminateStaff } from '@/lib/hooks/useStaff'
import { PageHeader } from '@/components/shared/PageHeader'
import { EmptyState, TableSkeleton } from '@/components/shared/EmptyState'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { formatDate } from '@/lib/utils/formatters'
import { cn } from '@/lib/utils/cn'
import type { Staff } from '@/lib/types/staff'

const ROLE_COLORS: Record<string, string> = {
  MANAGER: 'bg-indigo-500/15 text-indigo-400',
  TRAINER: 'bg-emerald-500/15 text-emerald-400',
  RECEPTIONIST: 'bg-amber-500/15 text-amber-400',
  ACCOUNTANT: 'bg-cyan-500/15 text-cyan-400',
  STAFF: 'bg-white/5 text-white/50',
}

const STATUS_COLORS: Record<string, string> = {
  ACTIVE: 'bg-emerald-500/15 text-emerald-400',
  ON_LEAVE: 'bg-blue-500/15 text-blue-400',
  INACTIVE: 'bg-white/5 text-white/40',
  SUSPENDED: 'bg-amber-500/15 text-amber-400',
  TERMINATED: 'bg-red-500/15 text-red-400',
}

export default function StaffPage() {
  const { id: businessId } = useParams<{ id: string }>()
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [suspendTarget, setSuspendTarget] = useState<Staff | null>(null)
  const [terminateTarget, setTerminateTarget] = useState<Staff | null>(null)

  const { data, isLoading, refetch } = useStaff(businessId, { page, size: 20, search: search || undefined })
  const suspendMutation = useSuspendStaff(businessId)
  const terminateMutation = useTerminateStaff(businessId)

  const columns = useMemo<ColumnDef<Staff>[]>(() => [
    {
      accessorKey: 'userName',
      header: 'Staff Member',
      cell: ({ row }) => (
        <div className="flex items-center gap-2.5">
          {/* FIX 6: was row.original.name — backend sends userName */}
          <div className="w-8 h-8 rounded-full bg-violet-600/20 flex items-center justify-center text-xs font-bold text-violet-300 flex-shrink-0">
            {(row.original.userName || '??').slice(0, 2).toUpperCase()}
          </div>
          <div>
            <div className="text-sm font-medium text-white/90">{row.original.userName}</div>
            {/* FIX 6: was row.original.email — backend sends userEmail */}
            <div className="text-xs text-white/35 flex items-center gap-1">
              <Mail className="w-2.5 h-2.5" />{row.original.userEmail}
            </div>
          </div>
        </div>
      ),
    },
    {
      accessorKey: 'role',
      header: 'Role',
      cell: ({ getValue }) => (
        <span className={cn('text-xs px-2.5 py-1 rounded-full font-medium', ROLE_COLORS[getValue() as string] || ROLE_COLORS.STAFF)}>
          {getValue() as string}
        </span>
      ),
    },
    {
      accessorKey: 'department',
      header: 'Department',
      cell: ({ getValue }) => getValue()
        ? <span className="text-sm text-white/60">{getValue() as string}</span>
        : <span className="text-white/25">—</span>,
    },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ getValue }) => (
        <span className={cn('text-xs px-2.5 py-1 rounded-full font-medium', STATUS_COLORS[getValue() as string] || STATUS_COLORS.INACTIVE)}>
          {getValue() as string}
        </span>
      ),
    },
    {
      // FIX 6: was "permissions" — StaffResponse has no permissions field.
      // Show canManageMembers/canManagePayments flags instead.
      id: 'capabilities',
      header: 'Capabilities',
      cell: ({ row }) => {
        const caps = [
          row.original.canManageMembers && 'Members',
          row.original.canManagePayments && 'Payments',
          row.original.canViewReports && 'Reports',
        ].filter(Boolean)
        return (
          <span className="text-xs text-white/50">
            {caps.length > 0 ? caps.join(', ') : 'View only'}
          </span>
        )
      },
    },
    {
      // FIX 6: was "joinDate" — backend sends "hireDate"
      accessorKey: 'hireDate',
      header: 'Hired',
      cell: ({ getValue }) => (
        <span className="text-sm text-white/50">
          {getValue() ? formatDate(getValue() as string) : '—'}
        </span>
      ),
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => {
        const staffIdentifier = row.original.publicId || row.original.id
        return (
        <div className="flex items-center gap-1 justify-end">
          <Link
            href={`/businesses/${businessId}/staff/${staffIdentifier}`}
            className="w-7 h-7 flex items-center justify-center rounded-lg text-white/40 hover:text-white/80 hover:bg-white/6 transition-all"
          >
            <Eye className="w-3.5 h-3.5" />
          </Link>
          {row.original.status === 'ACTIVE' && (
            <button
              onClick={() => setSuspendTarget(row.original)}
              className="w-7 h-7 flex items-center justify-center rounded-lg text-white/40 hover:text-amber-400 hover:bg-amber-500/10 transition-all"
              title="Suspend"
            >
              <PauseCircle className="w-3.5 h-3.5" />
            </button>
          )}
          {row.original.status !== 'TERMINATED' && (
            <button
              onClick={() => setTerminateTarget(row.original)}
              className="w-7 h-7 flex items-center justify-center rounded-lg text-white/40 hover:text-red-400 hover:bg-red-500/10 transition-all"
              title="Terminate"
            >
              <UserX className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
        )
      },
    },
  ], [businessId])

  const table = useReactTable({
    data: data?.content ?? [],
    columns,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
    pageCount: data?.totalPages ?? -1,
  })

  return (
    <div>
      <PageHeader
        title="Staff"
        description={`${data?.totalElements ?? 0} staff members`}
        icon={UserCog}
        actions={
          <div className="flex items-center gap-2">
            <Link
              href={`/businesses/${businessId}/staff/invite`}
              className="flex items-center gap-2 px-4 py-2.5 border border-white/10 text-white/70 hover:text-white hover:bg-white/5 text-sm font-medium rounded-xl transition-all"
            >
              <Mail className="w-4 h-4" /> Invite via Email
            </Link>
            <Link
              href={`/businesses/${businessId}/staff/new`}
              className="flex items-center gap-2 px-4 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-xl transition-all shadow-lg shadow-indigo-600/25"
            >
              <Plus className="w-4 h-4" /> Add Staff
            </Link>
          </div>
        }
      />

      <div className="flex items-center gap-3 mb-5">
        <div className="flex-1 max-w-md relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
          <input
            value={search}
            onChange={e => { setSearch(e.target.value); setPage(0) }}
            placeholder="Search staff by name or email..."
            className="w-full bg-white/4 border border-white/8 rounded-xl pl-9 pr-4 py-2.5 text-sm text-white placeholder-white/25 focus:outline-none focus:border-indigo-500/50 transition-all"
          />
        </div>
        <button onClick={() => refetch()} className="w-9 h-9 flex items-center justify-center rounded-xl border border-white/8 text-white/40 hover:text-white/80 hover:bg-white/5 transition-all">
          <RefreshCw className="w-3.5 h-3.5" />
        </button>
      </div>

      <div className="rounded-2xl border border-white/6 overflow-hidden bg-white/[0.01]">
        {isLoading ? (
          <div className="p-4"><TableSkeleton rows={6} cols={7} /></div>
        ) : data?.content.length === 0 ? (
          <EmptyState
            icon={UserCog}
            title="No staff members yet"
            description="Add staff directly or invite them via email"
            action={
              <Link href={`/businesses/${businessId}/staff/new`} className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm rounded-xl transition-all">
                <Plus className="w-4 h-4" /> Add Staff
              </Link>
            }
          />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  {table.getHeaderGroups().map(hg => (
                    <tr key={hg.id} className="border-b border-white/5 bg-white/[0.02]">
                      {hg.headers.map(h => (
                        <th key={h.id} className="px-4 py-3 text-left text-xs font-medium text-white/40 whitespace-nowrap">
                          {flexRender(h.column.columnDef.header, h.getContext())}
                        </th>
                      ))}
                    </tr>
                  ))}
                </thead>
                <tbody>
                  {table.getRowModel().rows.map(row => (
                    <tr key={row.id} className="border-b border-white/[0.03] hover:bg-white/[0.02] transition-colors">
                      {row.getVisibleCells().map(cell => (
                        <td key={cell.id} className="px-4 py-3">
                          {flexRender(cell.column.columnDef.cell, cell.getContext())}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="flex items-center justify-between px-4 py-3 border-t border-white/5">
              <span className="text-xs text-white/35">{data?.totalElements ?? 0} total staff</span>
              <div className="flex items-center gap-1.5">
                <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
                  className="w-7 h-7 flex items-center justify-center rounded-lg border border-white/8 text-white/50 hover:text-white hover:bg-white/5 disabled:opacity-30 transition-all">
                  <ChevronLeft className="w-3.5 h-3.5" />
                </button>
                <button onClick={() => setPage(p => p + 1)} disabled={page >= (data?.totalPages ?? 1) - 1}
                  className="w-7 h-7 flex items-center justify-center rounded-lg border border-white/8 text-white/50 hover:text-white hover:bg-white/5 disabled:opacity-30 transition-all">
                  <ChevronRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          </>
        )}
      </div>

      <ConfirmDialog
        open={!!suspendTarget}
        onClose={() => setSuspendTarget(null)}
        onConfirm={async () => {
          if (!suspendTarget) return
          try { await suspendMutation.mutateAsync(suspendTarget.publicId || suspendTarget.id) }
          finally { setSuspendTarget(null) }
        }}
        title="Suspend Staff Member"
        description={`Are you sure you want to suspend ${suspendTarget?.userName}?`}
        confirmLabel="Suspend"
        variant="warning"
        loading={suspendMutation.isPending}
      />
      <ConfirmDialog
        open={!!terminateTarget}
        onClose={() => setTerminateTarget(null)}
        onConfirm={async () => {
          if (!terminateTarget) return
          try {
            await terminateMutation.mutateAsync({
              staffId: terminateTarget.publicId || terminateTarget.id,
              terminationDate: new Date().toISOString(),
            })
          } finally {
            setTerminateTarget(null)
          }
        }}
        title="Terminate Staff Member"
        description={`Are you sure you want to terminate ${terminateTarget?.userName}? This action is permanent.`}
        confirmLabel="Terminate"
        loading={terminateMutation.isPending}
      />
    </div>
  )
}