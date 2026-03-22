'use client'
import { cn } from '@/lib/utils/cn'

interface FilterOption {
  label: string
  value: string
}

interface FilterGroupProps {
  options: FilterOption[]
  value: string
  onChange: (value: string) => void
  className?: string
}

export function FilterGroup({ options, value, onChange, className }: FilterGroupProps) {
  return (
    <div className={cn('flex items-center gap-0.5 bg-[hsl(var(--surface-1))] border border-white/[0.08] rounded-xl p-1', className)}>
      {options.map((option) => (
        <button
          key={option.value}
          onClick={() => onChange(option.value)}
          className={cn(
            'px-3 py-1.5 rounded-lg text-xs font-medium transition-all duration-200',
            value === option.value
              ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-600/20'
              : 'text-white/45 hover:text-white/70 hover:bg-white/[0.04]'
          )}
        >
          {option.label}
        </button>
      ))}
    </div>
  )
}
