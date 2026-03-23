'use client'

import { useMemo, useState } from 'react'
import Link from 'next/link'
import { useReactTable, getCoreRowModel, flexRender, ColumnDef } from '@tanstack/react-table'
import { CreditCard, Plus, ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react'
import { useAuthStore } from '@/lib/store/authStore'
import { usePayments, usePaymentMethodStats, useRevenueStats } from '@/lib/hooks/usePayments'
import { paymentsApi } from '@/lib/api/payments'
import { PageHeader } from '@/components/shared/PageHeader'
import { EmptyState, TableSkeleton } from '@/components/shared/EmptyState'
import { ExportButton } from '@/components/shared/ExportButton'
import { StatsCard } from '@/components/shared/StatsCard'
import { ResponsiveCardList } from '@/components/shared/ResponsiveCardList'
import { downloadBlob } from '@/lib/utils/export'
import { formatCurrency, formatDate, formatRelative } from '@/lib/utils/formatters'
import { cn } from '@/lib/utils/cn'
import type { Payment } from '@/lib/types/payment'
import { TrendingUp, DollarSign, Activity } from 'lucide-react'
import dynamic from 'next/dynamic'

const PaymentMethodPie = dynamic(() => import('@/components/payments/PaymentMethodPie').then((m) => m.PaymentMethodPie), {
  ssr: false,
  loading: () => <div className="w-20 h-20 rounded-xl bg-white/5 border border-white/10" />,
})

const METHOD_COLORS: Record<string, string> = {
  CASH: 'bg-emerald-500/15 text-emerald-400',
  CARD: 'bg-indigo-500/15 text-indigo-400',
  UPI: 'bg-violet-500/15 text-violet-400',
  BANK_TRANSFER: 'bg-cyan-500/15 text-cyan-400',
  CHEQUE: 'bg-amber-500/15 text-amber-400',
  OTHER: 'bg-white/5 text-white/40',
}

const CHART_COLORS = ['#10b981', '#6366f1', '#8b5cf6', '#06b6d4', '#f59e0b']

export default function StaffPaymentsPage() {
  const businessId = useAuthStore((s) => (s.staffContext?.businessId ? String(s.staffContext.businessId) : ''))
  const canManagePayments = useAuthStore((s) => s.staffContext?.canManagePayments ?? false)

  const [page, setPage] = useState(0)
  const [methodFilter, setMethodFilter] = useState<string>('')

  const { data, isLoading, refetch } = usePayments(businessId, {
    page,
    size: 20,
    paymentMethod: methodFilter || undefined,
  })
  const { data: methodStats } = usePaymentMethodStats(businessId)
  const { data: revenueStats } = useRevenueStats(businessId)

  const columns = useMemo<ColumnDef<Payment>[]>(() => [
    {
      accessorKey: 'memberName',
      header: 'Member',
      cell: ({ row }) => (
        <div>
          <div className="text-sm font-medium text-white/80">{row.original.memberName}</div>
          {row.original.publicId && <div className="text-[10px] text-emerald-300/85 font-medium">{row.original.publicId}</div>}
          {row.original.memberPhone && (
            <div className="text-xs text-white/35">{row.original.memberPhone}</div>
          )}
        </div>
      ),
    },
    {
      accessorKey: 'planName',
      header: 'Plan',
      cell: ({ getValue }) => getValue()
        ? <span className="text-sm text-white/60">{getValue() as string}</span>
        : <span className="text-white/25">Manual</span>,
    },
    {
      accessorKey: 'amount',
      header: 'Amount',
      cell: ({ getValue }) => (
        <span className="text-sm font-display font-600 text-emerald-400">
          {formatCurrency(getValue() as number)}
        </span>
      ),
    },
    {
      accessorKey: 'paymentMethod',
      header: 'Method',
      cell: ({ getValue }) => (
        <span className={cn(
          'text-xs px-2.5 py-1 rounded-full font-medium',
          METHOD_COLORS[getValue() as string] || METHOD_COLORS.OTHER
        )}>
          {getValue() as string}
        </span>
      ),
    },
    {
      accessorKey: 'createdAt',
      header: 'Date',
      cell: ({ getValue }) => (
        <div>
          <div className="text-sm text-white/60">{formatDate(getValue() as string)}</div>
          <div className="text-xs text-white/30">{formatRelative(getValue() as string)}</div>
        </div>
      ),
    },
  ], [])

  const table = useReactTable({
    data: data?.content ?? [],
    columns,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
    pageCount: data?.totalPages ?? -1,
  })

  if (!canManagePayments) {
    return (
      <div className="max-w-2xl">
        <PageHeader title="Payments" description="You don't have access to manage payments." icon={CreditCard} />
        <div className="p-6 rounded-2xl border border-white/6 bg-white/[0.02] text-sm text-white/50">
          Ask the owner to grant you payment-management permissions.
        </div>
      </div>
    )
  }

  return (
    <div>
      <PageHeader
        title="Payments"
        description={`${data?.totalElements ?? 0} total payments`}
        icon={CreditCard}
        actions={
          <div className="flex items-center gap-2">
            <ExportButton
              onExport={async () => {
                const blob = await paymentsApi.exportPayments(businessId)
                downloadBlob(blob, 'payments.csv')
              }}
            />
            <Link
              href="/staff/payments/new"
              className="flex items-center gap-2 px-4 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white text-sm font-medium rounded-xl transition-all shadow-lg shadow-emerald-600/20"
            >
              <Plus className="w-4 h-4" /> Record Payment
            </Link>
          </div>
        }
      />

      {revenueStats && (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          <StatsCard title="Total Revenue" value={formatCurrency(revenueStats.totalRevenue)} icon={DollarSign} accent="amber" index={0} />
          <StatsCard title="Monthly Revenue" value={formatCurrency(revenueStats.monthlyRevenue)} icon={TrendingUp} accent="emerald" index={1} />
          <StatsCard title="Today's Revenue" value={formatCurrency(revenueStats.todayRevenue)} icon={Activity} accent="indigo" index={2} />
          <StatsCard title="Avg per Member" value={formatCurrency(revenueStats.averagePerMember)} icon={CreditCard} accent="cyan" index={3} />
        </div>
      )}

      <div className="grid lg:grid-cols-3 gap-6 mb-6">
        <div className="lg:col-span-2 flex items-center gap-3">
          <div className="flex flex-wrap gap-1.5">
            {['', 'CASH', 'UPI', 'CARD', 'BANK_TRANSFER', 'CHEQUE'].map(m => (
              <button
                key={m}
                onClick={() => { setMethodFilter(m); setPage(0) }}
                className={cn(
                  'px-3 py-1.5 rounded-xl text-xs font-medium border transition-all',
                  methodFilter === m
                    ? 'bg-emerald-600 border-emerald-600 text-white'
                    : 'border-white/8 text-white/50 hover:text-white/80 hover:border-white/15'
                )}
              >
                {m || 'All Methods'}
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

        {methodStats && methodStats.length > 0 && (
          <div className="p-4 rounded-2xl border border-white/6 bg-white/[0.02]">
            <h3 className="text-xs font-medium text-white/40 mb-3">Payment Methods</h3>
            <div className="flex items-center gap-4">
              <PaymentMethodPie stats={methodStats as any} colors={CHART_COLORS} />
              <div className="space-y-1.5 flex-1">
                {methodStats.slice(0, 4).map((m: any, i: number) => (
                  <div key={m.method} className="flex items-center justify-between">
                    <div className="flex items-center gap-1.5">
                      <div className="w-2 h-2 rounded-full flex-shrink-0" style={{ background: CHART_COLORS[i % CHART_COLORS.length] }} />
                      <span className="text-xs text-white/40">{m.method}</span>
                    </div>
                    <span className="text-xs text-white/60 font-medium">{m.percentage.toFixed(0)}%</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>

      <ResponsiveCardList
        mobile={
          <div className="rounded-2xl border border-white/6 overflow-hidden bg-white/[0.01]">
            {isLoading ? (
              <div className="p-4"><TableSkeleton rows={6} cols={1} /></div>
            ) : data?.content.length === 0 ? (
              <div className="p-4">
                <EmptyState
                  icon={CreditCard}
                  title="No payments yet"
                  description="Record your first payment to get started"
                  action={
                    <Link href="/staff/payments/new" className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-sm rounded-xl transition-all">
                      <Plus className="w-4 h-4" /> Record Payment
                    </Link>
                  }
                />
              </div>
            ) : (
              <div className="divide-y divide-white/5">
                {(data?.content ?? []).map((p) => (
                  <div key={String(p.id)} className="p-4">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <div className="text-sm font-medium text-white/85 truncate">{p.memberName || '—'}</div>
                        {p.publicId && <div className="text-[10px] text-emerald-300/85 font-medium mt-0.5 truncate">{p.publicId}</div>}
                        <div className="text-xs text-white/40 truncate">{p.planName || 'Manual'}</div>
                        <div className="mt-2 text-xs text-white/45">
                          {formatDate(p.createdAt)} • {formatRelative(p.createdAt)}
                        </div>
                      </div>
                      <div className="text-right flex-shrink-0">
                        <div className="text-sm font-display font-600 text-emerald-400">{formatCurrency(p.amount as number)}</div>
                        <div className={cn('mt-1 inline-flex text-[10px] px-2 py-1 rounded-full font-medium', METHOD_COLORS[p.paymentMethod] || METHOD_COLORS.OTHER)}>
                          {p.paymentMethod}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}

                <div className="flex items-center justify-between px-4 py-3">
                  <span className="text-xs text-white/35">
                    Page {page + 1} / {data?.totalPages ?? 1}
                  </span>
                  <div className="flex items-center gap-2">
                    <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}
                      className="w-9 h-9 flex items-center justify-center rounded-xl border border-white/8 text-white/40 hover:text-white/80 hover:bg-white/5 disabled:opacity-30 transition-all">
                      <ChevronLeft className="w-4 h-4" />
                    </button>
                    <button onClick={() => setPage((p) => p + 1)} disabled={page >= (data?.totalPages ?? 1) - 1}
                      className="w-9 h-9 flex items-center justify-center rounded-xl border border-white/8 text-white/40 hover:text-white/80 hover:bg-white/5 disabled:opacity-30 transition-all">
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
              <div className="p-4"><TableSkeleton rows={8} cols={5} /></div>
            ) : data?.content.length === 0 ? (
              <EmptyState
                icon={CreditCard}
                title="No payments yet"
                description="Record your first payment to get started"
                action={
                  <Link href="/staff/payments/new" className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-sm rounded-xl transition-all">
                    <Plus className="w-4 h-4" /> Record Payment
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
                            <th key={h.id} className="px-4 py-3 text-left text-xs font-medium text-white/40">
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
                  <span className="text-xs text-white/35">{data?.totalElements ?? 0} total payments</span>
                  <div className="flex items-center gap-1.5">
                    <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
                      className="w-7 h-7 flex items-center justify-center rounded-lg border border-white/8 text-white/50 hover:text-white hover:bg-white/5 disabled:opacity-30 transition-all">
                      <ChevronLeft className="w-3.5 h-3.5" />
                    </button>
                    <span className="text-xs text-white/40 px-2">{page + 1} / {data?.totalPages ?? 1}</span>
                    <button onClick={() => setPage(p => p + 1)} disabled={page >= (data?.totalPages ?? 1) - 1}
                      className="w-7 h-7 flex items-center justify-center rounded-lg border border-white/8 text-white/50 hover:text-white hover:bg-white/5 disabled:opacity-30 transition-all">
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

