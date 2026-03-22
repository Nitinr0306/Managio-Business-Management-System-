'use client'
import { motion } from 'framer-motion'
import { LucideIcon, ChevronRight } from 'lucide-react'
import Link from 'next/link'
import { cn } from '@/lib/utils/cn'

/* ── Breadcrumb ───────────────────────────────────────────────── */
interface BreadcrumbItem {
  label: string
  href?: string
}

function Breadcrumbs({ items }: { items: BreadcrumbItem[] }) {
  return (
    <nav className="flex items-center gap-1 text-xs text-white/30 mb-2">
      {items.map((item, i) => (
        <span key={i} className="flex items-center gap-1">
          {i > 0 && <ChevronRight className="w-3 h-3 text-white/15" />}
          {item.href ? (
            <Link href={item.href} className="hover:text-white/50 transition-colors">
              {item.label}
            </Link>
          ) : (
            <span className="text-white/40">{item.label}</span>
          )}
        </span>
      ))}
    </nav>
  )
}

/* ── Page Header ──────────────────────────────────────────────── */
interface PageHeaderProps {
  title: string
  description?: string
  icon?: LucideIcon
  actions?: React.ReactNode
  breadcrumbs?: BreadcrumbItem[]
}

export function PageHeader({ title, description, icon: Icon, actions, breadcrumbs }: PageHeaderProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: -8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, ease: [0.22, 1, 0.36, 1] }}
      className="mb-6 md:mb-8"
    >
      {breadcrumbs && <Breadcrumbs items={breadcrumbs} />}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex items-center gap-3 min-w-0">
          {Icon && (
            <div className="w-10 h-10 rounded-xl bg-indigo-500/10 border border-indigo-500/15 flex items-center justify-center flex-shrink-0">
              <Icon className="w-5 h-5 text-indigo-400" />
            </div>
          )}
          <div className="min-w-0">
            <h1 className="text-xl md:text-2xl font-display font-700 text-white truncate">{title}</h1>
            {description && (
              <p className="text-xs md:text-sm text-white/40 mt-0.5 truncate">{description}</p>
            )}
          </div>
        </div>
        {actions && (
          <div className="flex flex-wrap items-center gap-2 sm:ml-4 flex-shrink-0">{actions}</div>
        )}
      </div>
    </motion.div>
  )
}