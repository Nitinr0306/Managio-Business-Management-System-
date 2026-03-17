'use client'

import { AnimatePresence, motion } from 'framer-motion'
import { CreditCard, Loader2, X } from 'lucide-react'
import { cn } from '@/lib/utils/cn'
import type { PaymentMethod } from '@/lib/types/payment'
import { PAYMENT_METHOD_LABELS } from '@/lib/types/payment'

export function PaymentAfterAssignDialog({
  open,
  accent = 'indigo',
  memberLabel,
  planLabel,
  amountLabel,
  method,
  onMethodChange,
  onClose,
  onConfirmNo,
  onConfirmYes,
  loading,
}: {
  open: boolean
  accent?: 'indigo' | 'emerald'
  memberLabel: string
  planLabel: string
  amountLabel: string
  method: PaymentMethod
  onMethodChange: (m: PaymentMethod) => void
  onClose: () => void
  onConfirmNo: () => void | Promise<void>
  onConfirmYes: () => void | Promise<void>
  loading?: boolean
}) {
  const accentBtn =
    accent === 'emerald'
      ? 'bg-emerald-600 hover:bg-emerald-500 shadow-emerald-600/25'
      : 'bg-indigo-600 hover:bg-indigo-500 shadow-indigo-600/25'

  const methods = Object.keys(PAYMENT_METHOD_LABELS) as PaymentMethod[]

  return (
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 bg-black/70 backdrop-blur-sm"
            onClick={onClose}
          />
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 10 }}
            transition={{ duration: 0.18, ease: [0.22, 1, 0.36, 1] }}
            className="relative w-full max-w-md bg-[#0f0f1a] border border-white/10 rounded-2xl p-6 shadow-2xl"
          >
            <button onClick={onClose} className="absolute top-4 right-4 text-white/30 hover:text-white/70 transition-colors">
              <X className="w-4 h-4" />
            </button>

            <div className={cn('w-12 h-12 rounded-2xl flex items-center justify-center mb-4', accent === 'emerald' ? 'bg-emerald-500/15' : 'bg-indigo-500/15')}>
              <CreditCard className={cn('w-6 h-6', accent === 'emerald' ? 'text-emerald-400' : 'text-indigo-400')} />
            </div>

            <h3 className="text-base font-display font-700 mb-1 text-white">Payment done?</h3>
            <p className="text-sm text-white/50 mb-4 leading-relaxed">
              You’re assigning <span className="text-white/75 font-medium">{planLabel}</span> to{' '}
              <span className="text-white/75 font-medium">{memberLabel}</span>. If payment is done, we’ll record it now.
            </p>

            <div className="rounded-xl border border-white/10 bg-white/[0.02] p-4 mb-5">
              <div className="flex items-center justify-between text-sm">
                <span className="text-white/40">Amount</span>
                <span className="text-white/80 font-display font-600">{amountLabel}</span>
              </div>
              <div className="mt-3">
                <label className="block text-xs font-medium text-white/50 mb-1.5">Payment method</label>
                <select
                  value={method}
                  onChange={(e) => onMethodChange(e.target.value as PaymentMethod)}
                  className="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2.5 text-sm text-white focus:outline-none focus:border-white/20"
                >
                  {methods.map((m) => (
                    <option key={m} value={m}>
                      {PAYMENT_METHOD_LABELS[m] ?? m}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="flex gap-3">
              <button
                onClick={onConfirmNo}
                disabled={!!loading}
                className="flex-1 py-2.5 rounded-xl border border-white/10 text-sm text-white/70 hover:bg-white/5 transition-all disabled:opacity-50"
              >
                No, just assign
              </button>
              <button
                onClick={onConfirmYes}
                disabled={!!loading}
                className={cn(
                  'flex-1 flex items-center justify-center gap-2 py-2.5 rounded-xl text-sm font-medium text-white transition-all disabled:opacity-60 shadow-lg',
                  accentBtn
                )}
              >
                {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Yes, record payment'}
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  )
}

