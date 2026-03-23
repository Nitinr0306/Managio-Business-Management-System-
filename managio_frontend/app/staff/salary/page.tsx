'use client'

import { useMemo, useState } from 'react'
import Link from 'next/link'
import { CalendarDays, CheckCircle2, ChevronLeft, CircleAlert, Wallet } from 'lucide-react'
import { useAuthStore } from '@/lib/store/authStore'
import { PageHeader } from '@/components/shared/PageHeader'
import { EmptyState, TableSkeleton } from '@/components/shared/EmptyState'
import { StatusBadge } from '@/components/shared/StatusBadge'
import { useMarkSalaryPaidForBusiness, useMonthlySalaryPayments, useUnpaidSalaryPayments } from '@/lib/hooks/useStaff'
import { formatCurrency, formatDateTime } from '@/lib/utils/formatters'

export default function StaffSalaryPage() {
  const businessId = useAuthStore((s) => (s.staffContext?.businessId ? String(s.staffContext.businessId) : ''))
  const canManagePayments = useAuthStore((s) => s.staffContext?.canManagePayments ?? false)
  const canViewReports = useAuthStore((s) => s.staffContext?.canViewReports ?? false)

  const [month, setMonth] = useState(new Date().toISOString().slice(0, 10))

  const { data: monthly, isLoading: monthlyLoading } = useMonthlySalaryPayments(businessId, month)
  const { data: unpaid, isLoading: unpaidLoading } = useUnpaidSalaryPayments(businessId, month)
  const markPaid = useMarkSalaryPaidForBusiness(businessId)

  const totalPending = useMemo(
    () => (monthly || []).reduce((sum, row) => sum + Number(row.pendingAmount || 0), 0),
    [monthly]
  )

  const totalPaid = useMemo(
    () => (monthly || []).reduce((sum, row) => sum + Number(row.paidAmount || 0), 0),
    [monthly]
  )

  if (!canViewReports && !canManagePayments) {
    return (
      <div className="max-w-2xl">
        <PageHeader title="Salary Ledger" description="You don't have access to salary records." icon={Wallet} />
        <div className="p-6 rounded-2xl border border-white/6 bg-white/[0.02] text-sm text-white/50">
          Ask the owner to grant report or payment permissions.
        </div>
      </div>
    )
  }

  const onMarkPaid = (staffId: string, salaryMonth: string, pendingAmount: number) => {
    markPaid.mutate({
      staffId,
      payload: {
        salaryMonth,
        paidAmount: pendingAmount,
        paidAt: new Date().toISOString().slice(0, 10),
        notes: 'Marked paid from staff salary ledger',
      },
    })
  }

  return (
    <div>
      <PageHeader
        title="Salary Ledger"
        description="Monthly salary payouts and pending dues"
        icon={Wallet}
        actions={
          <div className="flex items-center gap-2">
            <input
              type="date"
              value={month}
              onChange={(e) => setMonth(e.target.value)}
              className="h-10 px-3 rounded-xl bg-white/[0.04] border border-white/[0.08] text-sm text-white/75 focus:outline-none focus:ring-2 focus:ring-emerald-500/30"
            />
            <Link
              href="/staff/dashboard"
              className="h-10 px-4 inline-flex items-center gap-2 rounded-xl border border-white/[0.08] text-sm text-white/60 hover:text-white/80 hover:bg-white/[0.04]"
            >
              <ChevronLeft className="w-4 h-4" />
              Back
            </Link>
          </div>
        }
      />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="rounded-2xl border border-white/[0.06] bg-white/[0.02] p-4">
          <div className="text-xs text-white/40 mb-1">Salary Month</div>
          <div className="text-lg font-semibold text-white/80 flex items-center gap-2">
            <CalendarDays className="w-4 h-4 text-emerald-400" />
            {month}
          </div>
        </div>
        <div className="rounded-2xl border border-white/[0.06] bg-white/[0.02] p-4">
          <div className="text-xs text-white/40 mb-1">Total Paid</div>
          <div className="text-lg font-semibold text-emerald-400">{formatCurrency(totalPaid)}</div>
        </div>
        <div className="rounded-2xl border border-white/[0.06] bg-white/[0.02] p-4">
          <div className="text-xs text-white/40 mb-1">Total Pending</div>
          <div className="text-lg font-semibold text-amber-400">{formatCurrency(totalPending)}</div>
        </div>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <section className="xl:col-span-2 rounded-2xl border border-white/[0.06] bg-white/[0.01] overflow-hidden">
          <div className="px-4 py-3 border-b border-white/[0.06] text-sm font-medium text-white/75">Monthly Salary Status</div>
          {monthlyLoading ? (
            <div className="p-4"><TableSkeleton rows={6} cols={6} /></div>
          ) : !monthly || monthly.length === 0 ? (
            <div className="p-6">
              <EmptyState
                icon={Wallet}
                title="No salary records for selected month"
                description="Salary rows are created when active staff have configured salary."
              />
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-white/[0.05] bg-white/[0.015]">
                    <th className="px-4 py-3 text-left text-xs text-white/35">Staff</th>
                    <th className="px-4 py-3 text-left text-xs text-white/35">Monthly Salary</th>
                    <th className="px-4 py-3 text-left text-xs text-white/35">Paid</th>
                    <th className="px-4 py-3 text-left text-xs text-white/35">Pending</th>
                    <th className="px-4 py-3 text-left text-xs text-white/35">Status</th>
                    <th className="px-4 py-3 text-left text-xs text-white/35">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {monthly.map((row) => (
                    <tr key={row.id} className="border-b border-white/[0.04] hover:bg-white/[0.02]">
                      <td className="px-4 py-3">
                        <div className="text-sm text-white/80">{row.staffName || row.staffPublicId || row.staffId}</div>
                        <div className="text-xs text-emerald-300/85 font-medium">{row.staffPublicId || row.employeeId || 'N/A'}</div>
                      </td>
                      <td className="px-4 py-3 text-sm text-white/70">{formatCurrency(row.monthlySalary)}</td>
                      <td className="px-4 py-3 text-sm text-emerald-400">{formatCurrency(row.paidAmount)}</td>
                      <td className="px-4 py-3 text-sm text-amber-400">{formatCurrency(row.pendingAmount)}</td>
                      <td className="px-4 py-3"><StatusBadge status={row.paymentStatus} /></td>
                      <td className="px-4 py-3">
                        {canManagePayments && row.paymentStatus === 'UNPAID' && row.pendingAmount > 0 ? (
                          <button
                            onClick={() => onMarkPaid(row.staffId, row.salaryMonth, Number(row.pendingAmount))}
                            disabled={markPaid.isPending}
                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 disabled:opacity-60 text-xs font-medium text-white"
                          >
                            <CheckCircle2 className="w-3.5 h-3.5" />
                            Mark Paid
                          </button>
                        ) : (
                          <span className="text-xs text-white/35">{row.paidAt ? formatDateTime(row.paidAt) : 'Read only'}</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <section className="rounded-2xl border border-white/[0.06] bg-white/[0.01] overflow-hidden">
          <div className="px-4 py-3 border-b border-white/[0.06] text-sm font-medium text-white/75">Unpaid Salaries</div>
          {unpaidLoading ? (
            <div className="p-4"><TableSkeleton rows={5} cols={1} /></div>
          ) : !unpaid || unpaid.length === 0 ? (
            <div className="p-6">
              <EmptyState
                icon={CheckCircle2}
                title="No unpaid salaries"
                description="All salary records are settled for this month."
              />
            </div>
          ) : (
            <div className="p-3 space-y-2">
              {unpaid.map((row) => (
                <div key={`${row.staffId}-${row.salaryMonth}`} className="p-3 rounded-xl bg-white/[0.03] border border-white/[0.05]">
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <div className="text-sm text-white/80">{row.staffName || row.staffPublicId || row.staffId}</div>
                      <div className="text-xs text-white/35">Pending: {formatCurrency(row.pendingAmount)}</div>
                    </div>
                    <CircleAlert className="w-4 h-4 text-amber-400" />
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  )
}
