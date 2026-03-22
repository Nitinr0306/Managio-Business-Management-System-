'use client'
import { motion } from 'framer-motion'
import { LucideIcon, TrendingUp, TrendingDown, Minus } from 'lucide-react'
import { cn } from '@/lib/utils/cn'

interface StatsCardProps {
  title: string
  value: string | number
  icon: LucideIcon
  change?: string
  changeType?: 'up' | 'down' | 'neutral'
  description?: string
  accent?: 'indigo' | 'emerald' | 'amber' | 'pink' | 'cyan' | 'violet'
  index?: number
  onClick?: () => void
}

const accentMap = {
  indigo: {
    icon: 'text-indigo-400',
    bg: 'bg-indigo-500/10',
    border: 'border-indigo-500/15',
    glow: '99,102,241',
    gradient: 'from-indigo-500/20 to-indigo-600/5',
  },
  emerald: {
    icon: 'text-emerald-400',
    bg: 'bg-emerald-500/10',
    border: 'border-emerald-500/15',
    glow: '16,185,129',
    gradient: 'from-emerald-500/20 to-emerald-600/5',
  },
  amber: {
    icon: 'text-amber-400',
    bg: 'bg-amber-500/10',
    border: 'border-amber-500/15',
    glow: '245,158,11',
    gradient: 'from-amber-500/20 to-amber-600/5',
  },
  pink: {
    icon: 'text-pink-400',
    bg: 'bg-pink-500/10',
    border: 'border-pink-500/15',
    glow: '236,72,153',
    gradient: 'from-pink-500/20 to-pink-600/5',
  },
  cyan: {
    icon: 'text-cyan-400',
    bg: 'bg-cyan-500/10',
    border: 'border-cyan-500/15',
    glow: '6,182,212',
    gradient: 'from-cyan-500/20 to-cyan-600/5',
  },
  violet: {
    icon: 'text-violet-400',
    bg: 'bg-violet-500/10',
    border: 'border-violet-500/15',
    glow: '139,92,246',
    gradient: 'from-violet-500/20 to-violet-600/5',
  },
}

export function StatsCard({
  title, value, icon: Icon,
  change, changeType = 'neutral', description,
  accent = 'indigo', index = 0, onClick,
}: StatsCardProps) {
  const a = accentMap[accent]
  const ChangeIcon = changeType === 'up' ? TrendingUp : changeType === 'down' ? TrendingDown : Minus

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.45, delay: index * 0.06, ease: [0.22, 1, 0.36, 1] }}
      onClick={onClick}
      className={cn(
        'relative p-4 md:p-5 rounded-2xl border transition-all duration-300 group overflow-hidden',
        'bg-[hsl(var(--card))] hover:bg-[hsl(var(--card))]',
        a.border,
        onClick && 'cursor-pointer'
      )}
    >
      {/* Hover glow */}
      <div
        className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-500 rounded-2xl pointer-events-none"
        style={{ background: `radial-gradient(circle at 0% 0%, rgba(${a.glow},0.08), transparent 70%)` }}
      />

      {/* Top gradient line */}
      <div className={cn('absolute top-0 left-4 right-4 h-px bg-gradient-to-r opacity-0 group-hover:opacity-100 transition-opacity duration-300', a.gradient)} />

      <div className="flex items-start justify-between mb-3 relative z-10">
        <div className={cn('w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0', a.bg)}>
          <Icon className={cn('w-4 h-4', a.icon)} />
        </div>
        {change && (
          <div className={cn(
            'flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded-full',
            changeType === 'up'   ? 'text-emerald-400 bg-emerald-500/10' :
            changeType === 'down' ? 'text-red-400 bg-red-500/10' :
            'text-white/40 bg-white/[0.04]'
          )}>
            <ChangeIcon className="w-3 h-3" />
            {change}
          </div>
        )}
      </div>
      <div className="relative z-10">
        <div className="text-xl md:text-2xl font-display font-700 text-white leading-tight mb-0.5">
          {value}
        </div>
        <div className="text-xs text-white/40 font-medium">{title}</div>
        {description && <div className="text-[11px] text-white/25 mt-1">{description}</div>}
      </div>
    </motion.div>
  )
}