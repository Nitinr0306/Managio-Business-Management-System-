'use client'

import { useMemo, useState } from 'react'
import Link from 'next/link'
import {
  useReactTable,
  getCoreRowModel,
  getSortedRowModel,
  flexRender,
  ColumnDef,
  SortingState,
} from '@tanstack/react-table'
import {
  Users,
  Plus,
  Search,
  ChevronUp,
  ChevronDown,
  Eye,
  Edit,
  UserX,
  Phone,
  Mail,
  RefreshCw,
  ChevronLeft,
  ChevronRight as ChevronRightIcon,
} from 'lucide-react'
import { useAuthStore } from '@/lib/store/authStore'
import { useMembers, useDeactivateMember } from '@/lib/hooks/useMembers'
import { PageHeader } from '@/components/shared/PageHeader'
import { EmptyState, TableSkeleton } from '@/components/shared/EmptyState'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { ResponsiveCardList } from '@/components/shared/ResponsiveCardList'
import { formatDate } from '@/lib/utils/formatters'
import { cn } from '@/lib/utils/cn'
import type { Member } from '@/lib/types/member'

export default function StaffMembersPage() {
  const businessId = useAuthStore((s) => (s.staffContext?.businessId ? String(s.staffContext.businessId) : ''))
  const canManageMembers = useAuthStore((s) => s.staffContext?.canManageMembers ?? false)

  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL')
  const [page, setPage] = useState(0)
  const [sorting, setSorting] = useState<SortingState>([])
  const [deactivateTarget, setDeactivateTarget] = useState<Member | null>(null)

  const { data, isLoading, refetch } = useMembers(businessId, {
    page,
    size: 20,
    search: search || undefined,
    status: statusFilter === 'ALL' ? undefined : statusFilter,
  })
  const deactivateMutation = useDeactivateMember(businessId)

  const columns = useMemo<ColumnDef<Member>[]>(() => [
    {
      accessorKey: 'fullName',
      header: 'Member',
      cell: ({ row }) => (
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-full bg-emerald-600/20 flex items-center justify-center text-xs font-bold text-emerald-300 flex-shrink-0">
            {row.original.fullName.slice(0, 2).toUpperCase()}
          </div>
          <div>
            <div className="text-sm font-medium text-white/90">{row.original.fullName}</div>
            {row.original.email && (
              <div className="text-xs text-white/35 flex items-center gap-1">
                <Mail className="w-2.5 h-2.5" />{row.original.email}
              </div>
            )}
          </div>
        </div>
      ),
    },
    {
      accessorKey: 'phone',
      header: 'Phone',
      cell: ({ getValue }) => getValue() ? (
        <div className="flex items-center gap-1.5 text-sm text-white/60">
          <Phone className="w-3 h-3" />{getValue() as string}
        </div>
      ) : <span className="text-white/25">—</span>,
    },
    {
      accessorKey: 'status',
      header: 'Status',
      cell: ({ getValue }) => (
        <span className={cn(
          'text-xs px-2.5 py-1 rounded-full font-medium',
          getValue() === 'ACTIVE' ? 'bg-emerald-500/15 text-emerald-400' : 'bg-red-500/15 text-red-400'
        )}>
          {getValue() as string}
        </span>
      ),
    },
    {
      accessorKey: 'createdAt',
      header: 'Joined',
      cell: ({ getValue }) => <span className="text-sm text-white/50">{formatDate(getValue() as string)}</span>,
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <div className="flex items-center gap-1 justify-end">
          <Link
            href={`/staff/members/${row.original.id}`}
            className="w-7 h-7 flex items-center justify-center rounded-lg text-white/40 hover:text-white/80 hover:bg-white/6 transition-all"
            title="View"
          >
            <Eye className="w-3.5 h-3.5" />
          </Link>
          <Link
            href={`/staff/members/${row.original.id}?edit=1`}
            className="w-7 h-7 flex items-center justify-center rounded-lg text-white/40 hover:text-white/80 hover:bg-white/6 transition-all"
            title="Edit"
          >
            <Edit className="w-3.5 h-3.5" />
          </Link>
          {row.original.status === 'ACTIVE' && (
            <button
              onClick={() => setDeactivateTarget(row.original)}
              className="w-7 h-7 flex items-center justify-center rounded-lg text-white/40 hover:text-red-400 hover:bg-red-500/10 transition-all"
              title="Deactivate"
            >
              <UserX className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
      ),
    },
  ], [])

  const table = useReactTable({
    data: data?.content ?? [],
    columns,
    state: { sorting },
    onSortingChange: setSorting,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    manualPagination: true,
    pageCount: data?.totalPages ?? -1,
  })

  if (!canManageMembers) {
    return (
      <div className="max-w-2xl">
        <PageHeader title="Members" description="You don't have access to manage members." icon={Users} />
        <div className="p-6 rounded-2xl border border-white/6 bg-white/[0.02] text-sm text-white/50">
          Ask the owner to grant you member-management permissions.
        </div>
      </div>
    )
  }

  return (
    <div>
      <PageHeader
        title="Members"
        description={`${data?.totalElements ?? 0} total members`}
        icon={Users}
        actions={
          <div className="flex items-center gap-2">
            <Link
              href="/staff/members/new"
              className="flex items-center gap-2 px-4 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white text-sm font-medium rounded-xl transition-all shadow-lg shadow-emerald-600/20"
            >
              <Plus className="w-4 h-4" />
              Add Member
            </Link>
          </div>
        }
      />

      <div className="flex flex-wrap items-center gap-3 mb-5">
        <div className="flex-1 min-w-[220px] relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
          <input
            value={search}
            onChange={e => { setSearch(e.target.value); setPage(0) }}
            placeholder="Search by name, email, phone..."
            className="w-full bg-white/4 border border-white/8 rounded-xl pl-9 pr-4 py-2.5 text-sm text-white placeholder-white/25 focus:outline-none focus:border-emerald-500/40 transition-all"
          />
        </div>
        <div className="flex items-center gap-1 bg-white/4 border border-white/8 rounded-xl p-1">
          {(['ALL', 'ACTIVE', 'INACTIVE'] as const).map(s => (
            <button
              key={s}
              onClick={() => { setStatusFilter(s); setPage(0) }}
              className={cn(
                'px-3 py-1.5 rounded-lg text-xs font-medium transition-all',
                statusFilter === s ? 'bg-emerald-600 text-white' : 'text-white/50 hover:text-white/80'
              )}
            >
              {s}
            </button>
          ))}
        </div>
        <button
          onClick={() => refetch()}
          className="w-9 h-9 flex items-center justify-center rounded-xl border border-white/8 text-white/40 hover:text-white/80 hover:bg-white/5 transition-all"
          title="Refresh"
        >
          <RefreshCw className="w-3.5 h-3.5" />
        </button>
      </div>

      <ResponsiveCardList
        mobile={
          <div className="rounded-2xl border border-white/6 overflow-hidden bg-white/[0.01]">
            {isLoading ? (
              <div className="p-4"><TableSkeleton rows={6} cols={1} /></div>
            ) : data?.content.length === 0 ? (
              <div className="p-4">
                <EmptyState
                  icon={Users}
                  title="No members found"
                  description={search ? 'Try a different search term' : 'Add your first member to get started'}
                  action={
                    !search ? (
                      <Link
                        href="/staff/members/new"
                        className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-sm rounded-xl transition-all"
                      >
                        <Plus className="w-4 h-4" />Add Member
                      </Link>
                    ) : undefined
                  }
                />
              </div>
            ) : (
              <div className="divide-y divide-white/5">
                {(data?.content ?? []).map((m) => (
                  <div key={m.id} className="p-4">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <div className="text-sm font-medium text-white/90 truncate">{m.fullName}</div>
                        <div className="mt-1 space-y-1">
                          {m.email && (
                            <div className="text-xs text-white/40 flex items-center gap-1.5">
                              <Mail className="w-3 h-3" />
                              <span className="truncate">{m.email}</span>
                            </div>
                          )}
                          {m.phone && (
                            <div className="text-xs text-white/40 flex items-center gap-1.5">
                              <Phone className="w-3 h-3" />
                              <span className="truncate">{m.phone}</span>
                            </div>
                          )}
                          <div className="text-xs text-white/35">Joined {formatDate(m.createdAt)}</div>
                        </div>
                      </div>

                      <span
                        className={cn(
                          'text-[10px] px-2 py-1 rounded-full font-medium flex-shrink-0',
                          m.status === 'ACTIVE' ? 'bg-emerald-500/15 text-emerald-400' : 'bg-red-500/15 text-red-400'
                        )}
                      >
                        {m.status}
                      </span>
                    </div>

                    <div className="mt-3 flex items-center justify-end gap-2">
                      <Link
                        href={`/staff/members/${m.id}`}
                        className="px-3 py-2 rounded-xl border border-white/10 text-xs text-white/70 hover:bg-white/5"
                      >
                        View
                      </Link>
                      <Link
                        href={`/staff/members/${m.id}?edit=1`}
                        className="px-3 py-2 rounded-xl border border-white/10 text-xs text-white/70 hover:bg-white/5"
                      >
                        Edit
                      </Link>
                      {m.status === 'ACTIVE' && (
                        <button
                          onClick={() => setDeactivateTarget(m)}
                          className="px-3 py-2 rounded-xl border border-red-500/20 text-xs text-red-300 hover:bg-red-500/10"
                        >
                          Deactivate
                        </button>
                      )}
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
                      className="w-9 h-9 flex items-center justify-center rounded-xl border border-white/8 text-white/40 hover:text-white/80 hover:bg-white/5 disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-white/40 transition-all"
                    >
                      <ChevronLeft className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => setPage((p) => p + 1)}
                      disabled={page >= (data?.totalPages ?? 1) - 1}
                      className="w-9 h-9 flex items-center justify-center rounded-xl border border-white/8 text-white/40 hover:text-white/80 hover:bg-white/5 disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-white/40 transition-all"
                    >
                      <ChevronRightIcon className="w-4 h-4" />
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
              <div className="p-4"><TableSkeleton rows={8} cols={5} /></div>
            ) : data?.content.length === 0 ? (
              <EmptyState
                icon={Users}
                title="No members found"
                description={search ? 'Try a different search term' : 'Add your first member to get started'}
                action={
                  !search ? (
                    <Link
                      href="/staff/members/new"
                      className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-sm rounded-xl transition-all"
                    >
                      <Plus className="w-4 h-4" />Add Member
                    </Link>
                  ) : undefined
                }
              />
            ) : (
              <>
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      {table.getHeaderGroups().map(hg => (
                        <tr key={hg.id} className="border-b border-white/5 bg-white/[0.02]">
                          {hg.headers.map(header => (
                            <th
                              key={header.id}
                              onClick={header.column.getToggleSortingHandler()}
                              className={cn(
                                'px-4 py-3 text-left text-xs font-medium text-white/40 whitespace-nowrap',
                                header.column.getCanSort() && 'cursor-pointer hover:text-white/70 select-none'
                              )}
                            >
                              <div className="flex items-center gap-1.5">
                                {flexRender(header.column.columnDef.header, header.getContext())}
                                {header.column.getIsSorted() === 'asc' && <ChevronUp className="w-3 h-3" />}
                                {header.column.getIsSorted() === 'desc' && <ChevronDown className="w-3 h-3" />}
                              </div>
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
                  <span className="text-xs text-white/35">
                    Showing {page * 20 + 1}–{Math.min((page + 1) * 20, data?.totalElements ?? 0)} of {data?.totalElements ?? 0}
                  </span>
                  <div className="flex items-center gap-1.5">
                    <button
                      onClick={() => setPage(p => Math.max(0, p - 1))}
                      disabled={page === 0}
                      className="w-7 h-7 flex items-center justify-center rounded-lg border border-white/8 text-white/50 hover:text-white hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed transition-all"
                    >
                      <ChevronLeft className="w-3.5 h-3.5" />
                    </button>
                    <span className="text-xs text-white/40 px-2">
                      {page + 1} / {data?.totalPages ?? 1}
                    </span>
                    <button
                      onClick={() => setPage(p => p + 1)}
                      disabled={page >= (data?.totalPages ?? 1) - 1}
                      className="w-7 h-7 flex items-center justify-center rounded-lg border border-white/8 text-white/50 hover:text-white hover:bg-white/5 disabled:opacity-30 disabled:cursor-not-allowed transition-all"
                    >
                      <ChevronRightIcon className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </>
            )}
          </div>
        }
      />

      <ConfirmDialog
        open={!!deactivateTarget}
        onClose={() => setDeactivateTarget(null)}
        onConfirm={() => {
          if (deactivateTarget) {
            deactivateMutation.mutate(deactivateTarget.id)
            setDeactivateTarget(null)
          }
        }}
        title="Deactivate Member"
        description={`Are you sure you want to deactivate ${deactivateTarget?.fullName}? Their data will be preserved.`}
        confirmLabel="Deactivate"
        loading={deactivateMutation.isPending}
      />
    </div>
  )
}

