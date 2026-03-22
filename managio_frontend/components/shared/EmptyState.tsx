'use client'
import { motion } from 'framer-motion'
import { LucideIcon, Loader2 } from 'lucide-react'
import { cn } from '@/lib/utils/cn'

/* ── Empty State ──────────────────────────────────────────────── */
interface EmptyStateProps {
  icon: LucideIcon
  title: string
  description?: string
  action?: React.ReactNode
}

export function EmptyState({ icon: Icon, title, description, action }: EmptyStateProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
      className="flex flex-col items-center justify-center py-16 text-center px-4"
    >
      <div className="relative mb-5">
        <div className="absolute inset-0 bg-indigo-500/10 rounded-2xl blur-xl" />
        <div className="relative w-16 h-16 rounded-2xl bg-[hsl(var(--surface-1))] border border-white/[0.06] flex items-center justify-center">
          <Icon className="w-7 h-7 text-white/20" />
        </div>
      </div>
      <h3 className="text-sm font-display font-600 text-white/50 mb-1">{title}</h3>
      {description && (
        <p className="text-xs text-white/30 max-w-xs leading-relaxed">{description}</p>
      )}
      {action && <div className="mt-5">{action}</div>}
    </motion.div>
  )
}

/* ── Loading Spinner ──────────────────────────────────────────── */
export function LoadingSpinner({ size = 'md' }: { size?: 'sm' | 'md' | 'lg' }) {
  const s = { sm: 'w-4 h-4', md: 'w-7 h-7', lg: 'w-10 h-10' }
  return (
    <div className="flex items-center justify-center py-12">
      <div className="relative">
        <Loader2 className={`${s[size]} text-indigo-400 animate-spin`} />
        <div className={`absolute inset-0 ${s[size]} bg-indigo-500/20 blur-lg rounded-full`} />
      </div>
    </div>
  )
}

/* ── Skeleton Primitives ──────────────────────────────────────── */
export function Skeleton({ className = '' }: { className?: string }) {
  return <div className={cn('skeleton', className)} />
}

export function CardSkeleton() {
  return (
    <div className="p-5 rounded-2xl border border-white/[0.06] bg-[hsl(var(--card))] space-y-3">
      <div className="flex items-center justify-between">
        <Skeleton className="w-9 h-9 rounded-xl" />
        <Skeleton className="w-14 h-4 rounded-full" />
      </div>
      <Skeleton className="w-20 h-6 rounded-md" />
      <Skeleton className="w-28 h-3 rounded-md" />
    </div>
  )
}

export function TableSkeleton({ rows = 5, cols = 4 }: { rows?: number; cols?: number }) {
  return (
    <div className="space-y-1.5">
      <div className="flex gap-4 px-4 py-3 mb-1">
        {Array.from({ length: cols }).map((_, i) => (
          <Skeleton key={i} className="h-3.5 flex-1 rounded-md" />
        ))}
      </div>
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="flex gap-4 px-4 py-3.5 rounded-xl bg-white/[0.01]">
          {Array.from({ length: cols }).map((_, j) => (
            <Skeleton key={j} className="h-3.5 flex-1 rounded-md" />
          ))}
        </div>
      ))}
    </div>
  )
}

/* ── Page Loading ─────────────────────────────────────────────── */
export function PageLoading() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] gap-3">
      <LoadingSpinner size="lg" />
      <p className="text-xs text-white/25 animate-pulse">Loading...</p>
    </div>
  )
}