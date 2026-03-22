'use client'

import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { ArrowLeft, CreditCard, Loader2 } from 'lucide-react'
import { motion } from 'framer-motion'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useRecordPayment } from '@/lib/hooks/usePayments'
import { useMembers } from '@/lib/hooks/useMembers'
import { useMemberSubscriptions } from '@/lib/hooks/useMembers'
import { useState } from 'react'

const schema = z.object({
  memberId: z.string().min(1, 'Select a member'),
  subscriptionId: z.string().optional(),
  amount: z.number({ coerce: true }).min(1, 'Amount required'),
  paymentMethod: z.enum(['CASH', 'CARD', 'UPI', 'BANK_TRANSFER', 'CHEQUE', 'OTHER']),
  referenceNumber: z.string().optional(),
  notes: z.string().optional(),
  paidAt: z.string().optional(),
})
type FormData = z.infer<typeof schema>

export default function RecordPaymentPage() {
  const { id: businessId } = useParams<{ id: string }>()
  const router = useRouter()
  const recordMutation = useRecordPayment(businessId)
  const { data: members } = useMembers(businessId, { size: 200, status: 'ACTIVE' })
  const [selectedMember, setSelectedMember] = useState('')
  const { data: memberSubs } = useMemberSubscriptions(businessId, selectedMember)

  const { register, handleSubmit, setValue, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      paymentMethod: 'CASH',
      paidAt: new Date().toISOString().split('T')[0],
    },
  })

  const inp = 'w-full bg-white/4 border border-white/8 rounded-xl px-4 py-2.5 text-sm text-white placeholder-white/25 focus:outline-none focus:border-indigo-500/60 transition-all'
  const lbl = 'block text-xs font-medium text-white/50 mb-1.5'

  return (
    <div className="max-w-xl mx-auto">
      <div className="mb-8">
        <Link href={`/businesses/${businessId}/payments`} className="inline-flex items-center gap-2 text-sm text-white/50 hover:text-white/80 transition-colors mb-4">
          <ArrowLeft className="w-4 h-4" /> Back to payments
        </Link>
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-500/15 border border-emerald-500/20 flex items-center justify-center">
            <CreditCard className="w-5 h-5 text-emerald-400" />
          </div>
          <div>
            <h1 className="text-2xl font-display font-700">Record Payment</h1>
            <p className="text-sm text-white/45">Log a manual payment for a member</p>
          </div>
        </div>
      </div>

      <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} className="p-6 rounded-2xl border border-white/6 bg-white/[0.02]">
        <form
          onSubmit={handleSubmit(async (data) => {
            try {
              await recordMutation.mutateAsync({
                memberId: Number(data.memberId),
                subscriptionId: data.subscriptionId ? Number(data.subscriptionId) : undefined,
                amount: data.amount,
                paymentMethod: data.paymentMethod,
                referenceNumber: data.referenceNumber,
                notes: data.notes,
                paidAt: data.paidAt,
              })
              router.push(`/businesses/${businessId}/payments`)
            } catch {
              // Error toast is handled by useRecordPayment hook — stay on form for retry
            }
          })}
          className="space-y-4"
        >
          <div>
            <label className={lbl}>Member *</label>
            <select
              {...register('memberId')}
              onChange={e => { setValue('memberId', e.target.value); setSelectedMember(e.target.value) }}
              className={inp + ' cursor-pointer'}
            >
              <option value="">Select a member...</option>
              {members?.content.map(m => (
                <option key={m.id} value={m.id}>
                  {m.fullName}{m.phone ? ` — ${m.phone}` : ''}
                </option>
              ))}
            </select>
            {errors.memberId && <p className="text-red-400 text-xs mt-1">{errors.memberId.message}</p>}
          </div>

          {selectedMember && memberSubs && memberSubs.length > 0 && (
            <div>
              <label className={lbl}>Link to Subscription (optional)</label>
              <select {...register('subscriptionId')} className={inp + ' cursor-pointer'}>
                <option value="">No linked subscription</option>
                {memberSubs.filter(s => s.status === 'ACTIVE').map(s => (
                  <option key={s.id} value={s.id}>{s.planName}</option>
                ))}
              </select>
            </div>
          )}

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={lbl}>Amount (₹) *</label>
              <input {...register('amount')} type="number" placeholder="0" className={inp} />
              {errors.amount && <p className="text-red-400 text-xs mt-1">{errors.amount.message}</p>}
            </div>
            <div>
              <label className={lbl}>Payment Method *</label>
              <select {...register('paymentMethod')} className={inp + ' cursor-pointer'}>
                <option value="CASH">Cash</option>
                <option value="UPI">UPI</option>
                <option value="CARD">Card</option>
                <option value="BANK_TRANSFER">Bank Transfer</option>
                <option value="CHEQUE">Cheque</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={lbl}>Payment Date</label>
              <input {...register('paidAt')} type="date" className={inp} />
            </div>
            <div>
              <label className={lbl}>Reference Number</label>
              <input {...register('referenceNumber')} placeholder="e.g. UPI Ref #" className={inp} />
            </div>
          </div>

          <div>
            <label className={lbl}>Notes</label>
            <textarea {...register('notes')} placeholder="Optional notes..." rows={2} className={inp + ' resize-none'} />
          </div>

          <button
            type="submit"
            disabled={recordMutation.isPending}
            className="w-full flex items-center justify-center gap-2 py-3 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-60 text-white font-medium rounded-xl transition-all shadow-lg shadow-emerald-600/25"
          >
            {recordMutation.isPending
              ? <><Loader2 className="w-4 h-4 animate-spin" /> Recording...</>
              : <><CreditCard className="w-4 h-4" /> Record Payment</>
            }
          </button>
        </form>
      </motion.div>
    </div>
  )
}