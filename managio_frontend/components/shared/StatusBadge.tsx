'use client'
import { cn } from '@/lib/utils/cn'

type StatusType = 'ACTIVE' | 'INACTIVE' | 'PENDING' | 'EXPIRED' | 'PAID' | 'UNPAID' | 'CANCELLED' | 'COMPLETED' | string

const statusStyles: Record<string, { bg: string; text: string; dot: string }> = {
  ACTIVE:    { bg: 'bg-emerald-500/10', text: 'text-emerald-400', dot: 'bg-emerald-400' },
  PAID:      { bg: 'bg-emerald-500/10', text: 'text-emerald-400', dot: 'bg-emerald-400' },
  COMPLETED: { bg: 'bg-emerald-500/10', text: 'text-emerald-400', dot: 'bg-emerald-400' },
  INACTIVE:  { bg: 'bg-red-500/10',     text: 'text-red-400',     dot: 'bg-red-400' },
  UNPAID:    { bg: 'bg-red-500/10',     text: 'text-red-400',     dot: 'bg-red-400' },
  CANCELLED: { bg: 'bg-red-500/10',     text: 'text-red-400',     dot: 'bg-red-400' },
  EXPIRED:   { bg: 'bg-amber-500/10',   text: 'text-amber-400',   dot: 'bg-amber-400' },
  PENDING:   { bg: 'bg-amber-500/10',   text: 'text-amber-400',   dot: 'bg-amber-400' },
}

const fallback = { bg: 'bg-white/[0.06]', text: 'text-white/50', dot: 'bg-white/40' }

interface StatusBadgeProps {
  status: StatusType
  className?: string
  showDot?: boolean
  size?: 'sm' | 'md'
}

export function StatusBadge({ status, className, showDot = true, size = 'sm' }: StatusBadgeProps) {
  const style = statusStyles[status] || fallback

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 font-medium rounded-full',
        style.bg, style.text,
        size === 'sm' ? 'text-[10px] px-2 py-0.5' : 'text-xs px-2.5 py-1',
        className,
      )}
    >
      {showDot && <span className={cn('w-1.5 h-1.5 rounded-full', style.dot)} />}
      {status}
    </span>
  )
}
