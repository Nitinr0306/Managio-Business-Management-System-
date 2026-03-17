'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { ArrowLeft, CreditCard, Loader2 } from 'lucide-react'
import { motion } from 'framer-motion'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { usePlans, useAssignSubscription } from '@/lib/hooks/useSubscriptions'
import { useMembers } from '@/lib/hooks/useMembers'
import { formatCurrency } from '@/lib/utils/formatters'
import { useAuthStore } from '@/lib/store/authStore'
import { useRecordPayment } from '@/lib/hooks/usePayments'
import { PaymentAfterAssignDialog } from '@/components/payments/PaymentAfterAssignDialog'
import type { PaymentMethod } from '@/lib/types/payment'
import { toast } from 'sonner'
import { useMemo, useState } from 'react'

const schema = z.object({
  memberId: z.string().min(1, 'Select a member'),
  planId: z.string().min(1, 'Select a plan'),
  startDate: z.string().min(1, 'Start date required'),
})
type FormData = z.infer<typeof schema>

export default function StaffAssignSubscriptionPage() {
  const router = useRouter()
  const businessId = useAuthStore((s) => (s.staffContext?.businessId ? String(s.staffContext.businessId) : ''))
  const canManageSubscriptions = useAuthStore((s) => s.staffContext?.canManageSubscriptions ?? false)

  const { data: plans } = usePlans(businessId)
  const { data: members } = useMembers(businessId, { size: 200, status: 'ACTIVE' })
  const assignMutation = useAssignSubscription(businessId)
  const recordPayment = useRecordPayment(businessId)

  const [confirmOpen, setConfirmOpen] = useState(false)
  const [pending, setPending] = useState<{ memberId: number; planId: number; startDate: string } | null>(null)
  const [method, setMethod] = useState<PaymentMethod>('CASH')
  const [busy, setBusy] = useState(false)

  const pendingMember = useMemo(() => {
    if (!pending) return null
    return members?.content.find((m) => String(m.id) === String(pending.memberId)) ?? null
  }, [members?.content, pending])

  const pendingPlan = useMemo(() => {
    if (!pending) return null
    return plans?.find((p) => String(p.id) === String(pending.planId)) ?? null
  }, [plans, pending])

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      startDate: new Date().toISOString().split('T')[0],
    },
  })

  const inp = 'w-full bg-white/4 border border-white/8 rounded-xl px-4 py-2.5 text-sm text-white placeholder-white/25 focus:outline-none focus:border-emerald-500/40 transition-all'
  const lbl = 'block text-xs font-medium text-white/50 mb-1.5'

  if (!canManageSubscriptions) {
    return (
      <div className="max-w-2xl">
        <div className="p-6 rounded-2xl border border-white/6 bg-white/[0.02] text-sm text-white/50">
          You don't have permission to assign subscriptions.
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-xl mx-auto">
      <div className="mb-8">
        <Link
          href="/staff/subscriptions"
          className="inline-flex items-center gap-2 text-sm text-white/50 hover:text-white/80 transition-colors mb-4"
        >
          <ArrowLeft className="w-4 h-4" /> Back to subscriptions
        </Link>
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-500/15 border border-emerald-500/20 flex items-center justify-center">
            <CreditCard className="w-5 h-5 text-emerald-400" />
          </div>
          <div>
            <h1 className="text-2xl font-display font-700">Assign Subscription</h1>
            <p className="text-sm text-white/45">Assign a plan to a member</p>
          </div>
        </div>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        className="p-6 rounded-2xl border border-white/6 bg-white/[0.02]"
      >
        <form
          onSubmit={handleSubmit(async (data) => {
            const payload = {
              memberId: Number(data.memberId),
              planId: Number(data.planId),
              startDate: data.startDate,
            }
            setPending(payload)
            setConfirmOpen(true)
          })}
          className="space-y-4"
        >
          <div>
            <label className={lbl}>Member *</label>
            <select {...register('memberId')} className={inp + ' cursor-pointer'}>
              <option value="">Select a member...</option>
              {members?.content.map(m => (
                <option key={m.id} value={m.id}>
                  {m.fullName}{m.phone ? ` — ${m.phone}` : ''}
                </option>
              ))}
            </select>
            {errors.memberId && <p className="text-red-400 text-xs mt-1">{errors.memberId.message}</p>}
          </div>

          <div>
            <label className={lbl}>Subscription Plan *</label>
            <select {...register('planId')} className={inp + ' cursor-pointer'}>
              <option value="">Select a plan...</option>
              {plans?.filter(p => p.isActive).map(p => (
                <option key={p.id} value={p.id}>
                  {p.name} — {formatCurrency(p.price)} / {p.durationDays} days
                </option>
              ))}
            </select>
            {errors.planId && <p className="text-red-400 text-xs mt-1">{errors.planId.message}</p>}
          </div>

          <div>
            <label className={lbl}>Start Date *</label>
            <input {...register('startDate')} type="date" className={inp} />
            {errors.startDate && <p className="text-red-400 text-xs mt-1">{errors.startDate.message}</p>}
          </div>

          <button
            type="submit"
            disabled={assignMutation.isPending}
            className="w-full flex items-center justify-center gap-2 py-3 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-60 text-white font-medium rounded-xl transition-all shadow-lg shadow-emerald-600/25"
          >
            {assignMutation.isPending
              ? <><Loader2 className="w-4 h-4 animate-spin" /> Assigning...</>
              : <><CreditCard className="w-4 h-4" /> Assign Subscription</>
            }
          </button>
        </form>
      </motion.div>

      <PaymentAfterAssignDialog
        open={confirmOpen}
        accent="emerald"
        memberLabel={pendingMember?.fullName || 'this member'}
        planLabel={pendingPlan?.name || 'this plan'}
        amountLabel={pendingPlan ? formatCurrency(pendingPlan.price) : '—'}
        method={method}
        onMethodChange={setMethod}
        loading={busy}
        onClose={() => {
          if (busy) return
          setConfirmOpen(false)
          setPending(null)
        }}
        onConfirmNo={async () => {
          if (!pending) return
          setBusy(true)
          try {
            await assignMutation.mutateAsync(pending)
            router.push('/staff/subscriptions')
          } finally {
            setBusy(false)
            setConfirmOpen(false)
            setPending(null)
          }
        }}
        onConfirmYes={async () => {
          if (!pending || !pendingPlan) return
          setBusy(true)
          try {
            await assignMutation.mutateAsync(pending)
            await recordPayment.mutateAsync({
              memberId: pending.memberId,
              amount: pendingPlan.price,
              paymentMethod: method,
              notes: `Payment recorded during subscription assignment (${pendingPlan.name})`,
              paidAt: pending.startDate,
            })
            router.push('/staff/subscriptions')
          } catch {
            toast.error('Assigned subscription, but failed to record payment. You can record it from Payments.')
          } finally {
            setBusy(false)
            setConfirmOpen(false)
            setPending(null)
          }
        }}
      />
    </div>
  )
}

