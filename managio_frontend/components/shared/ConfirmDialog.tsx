'use client'
import { motion, AnimatePresence } from 'framer-motion'
import { AlertTriangle, Loader2, X } from 'lucide-react'
import { cn } from '@/lib/utils/cn'

interface ConfirmDialogProps {
  open: boolean
  onClose: () => void
  onConfirm: () => void | Promise<void>
  title: string
  description: string
  confirmLabel?: string
  cancelLabel?: string
  variant?: 'danger' | 'warning'
  loading?: boolean
}

export function ConfirmDialog({
  open, onClose, onConfirm, title, description,
  confirmLabel = 'Confirm', cancelLabel = 'Cancel',
  variant = 'danger', loading = false,
}: ConfirmDialogProps) {
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
            initial={{ opacity: 0, scale: 0.92, y: 12 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.92, y: 12 }}
            transition={{ duration: 0.2, ease: [0.22, 1, 0.36, 1] }}
            className="relative w-full max-w-sm bg-[hsl(var(--card))] border border-white/[0.08] rounded-2xl p-6 shadow-2xl shadow-black/40"
          >
            <button
              onClick={onClose}
              className="absolute top-4 right-4 text-white/25 hover:text-white/60 transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
            <div className={cn(
              'w-12 h-12 rounded-2xl flex items-center justify-center mb-4',
              variant === 'danger' ? 'bg-red-500/10' : 'bg-amber-500/10'
            )}>
              <AlertTriangle className={cn('w-6 h-6', variant === 'danger' ? 'text-red-400' : 'text-amber-400')} />
            </div>
            <h3 className="text-base font-display font-700 mb-2 text-white">{title}</h3>
            <p className="text-sm text-white/45 mb-6 leading-relaxed">{description}</p>
            <div className="flex gap-3">
              <button
                onClick={onClose}
                disabled={loading}
                className="flex-1 py-2.5 rounded-xl border border-white/[0.08] text-sm text-white/60 hover:text-white/80 hover:bg-white/[0.04] transition-all disabled:opacity-50"
              >
                {cancelLabel}
              </button>
              <button
                onClick={onConfirm}
                disabled={loading}
                className={cn(
                  'flex-1 flex items-center justify-center gap-2 py-2.5 rounded-xl text-sm font-medium text-white transition-all disabled:opacity-60',
                  variant === 'danger'
                    ? 'bg-red-600 hover:bg-red-500 shadow-lg shadow-red-600/20'
                    : 'bg-amber-600 hover:bg-amber-500 shadow-lg shadow-amber-600/20'
                )}
              >
                {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : confirmLabel}
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  )
}