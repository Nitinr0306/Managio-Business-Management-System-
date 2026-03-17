'use client'
import { motion } from 'framer-motion'
import { LucideIcon } from 'lucide-react'

interface PageHeaderProps {
  title: string
  description?: string
  icon?: LucideIcon
  actions?: React.ReactNode
}

export function PageHeader({ title, description, icon: Icon, actions }: PageHeaderProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: -8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, ease: [0.22, 1, 0.36, 1] }}
      className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between mb-6 md:mb-8"
    >
      <div className="flex items-center gap-3 min-w-0">
        {Icon && (
          <div className="w-10 h-10 rounded-xl bg-indigo-500/15 border border-indigo-500/20 flex items-center justify-center flex-shrink-0">
            <Icon className="w-5 h-5 text-indigo-400" />
          </div>
        )}
        <div className="min-w-0">
          <h1 className="text-xl md:text-2xl font-display font-700 text-white truncate">{title}</h1>
          {description && <p className="text-xs md:text-sm text-white/45 mt-0.5 truncate">{description}</p>}
        </div>
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2 sm:ml-4 flex-shrink-0">{actions}</div>}
    </motion.div>
  )
}