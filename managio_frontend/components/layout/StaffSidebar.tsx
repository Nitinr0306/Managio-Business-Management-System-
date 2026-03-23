'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import {
  LayoutDashboard,
  Users,
  CreditCard,
  BarChart3,
  ScrollText,
  LogOut,
  Dumbbell,
  Wallet,
  CheckSquare,
} from 'lucide-react'
import { useAuth } from '@/lib/hooks/useAuth'
import { cn } from '@/lib/utils/cn'

interface StaffSidebarProps {
  businessId: string
  variant?: 'desktop' | 'mobile'
  onNavigate?: () => void
  className?: string
}

export function StaffSidebar({
  businessId,
  variant = 'desktop',
  onNavigate,
  className,
}: StaffSidebarProps) {
  const pathname = usePathname()
  const { logout, staffContext } = useAuth()

  const nav = [
    { label: 'Dashboard', href: '/staff/dashboard', icon: LayoutDashboard, enabled: true },
    {
      label: 'Members',
      href: '/staff/members',
      icon: Users,
      enabled: !!staffContext?.canManageMembers,
    },
    {
      label: 'Payments',
      href: '/staff/payments',
      icon: BarChart3,
      enabled: !!staffContext?.canManagePayments,
    },
    {
      label: 'Tasks',
      href: '/staff/tasks',
      icon: CheckSquare,
      enabled: true,
    },
    {
      label: 'Salary Ledger',
      href: '/staff/salary',
      icon: Wallet,
      enabled: !!staffContext?.canViewReports || !!staffContext?.canManagePayments,
    },
    {
      label: 'Subscriptions',
      href: '/staff/subscriptions',
      icon: CreditCard,
      enabled: !!staffContext?.canManageSubscriptions,
    },
    { label: 'Audit Logs', href: `/businesses/${businessId}/audit-logs`, icon: ScrollText, enabled: true },
  ]

  const isActive = (href: string) => pathname === href || pathname.startsWith(href + '/')

  return (
    <aside
      className={cn(
        variant === 'desktop'
          ? 'w-[260px] fixed left-0 top-0 h-screen bg-[#080812] border-r border-white/5 hidden md:flex flex-col'
          : 'w-full bg-transparent flex flex-col',
        className
      )}
    >
      <div className={cn('flex items-center gap-3 px-4 h-16 border-b border-white/5', variant === 'mobile' && 'bg-[#080812]')}>
        <Link
          href="/staff/dashboard"
          onClick={() => onNavigate?.()}
          className="w-8 h-8 rounded-lg bg-gradient-to-br from-emerald-500 to-teal-600 flex items-center justify-center flex-shrink-0 shadow-lg shadow-emerald-500/25"
        >
          <Dumbbell className="w-4 h-4 text-white" />
        </Link>
        <div className="flex-1 min-w-0">
          <div className="text-sm font-display font-700 truncate">Staff Portal</div>
          <div className="text-[10px] text-white/35 truncate">Business: {staffContext?.businessPublicId || businessId}</div>
        </div>
      </div>

      <nav className={cn('flex-1 overflow-y-auto py-3 px-2.5 space-y-0.5', variant === 'mobile' && 'bg-[#080812]')}>
        {nav.map((item) => {
          const active = isActive(item.href)
          const disabled = !item.enabled
          return (
            <Link
              key={item.href}
              href={disabled ? '#' : item.href}
              aria-disabled={disabled}
              onClick={(e) => {
                if (disabled) e.preventDefault()
                else onNavigate?.()
              }}
              className={cn(
                'relative flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-150',
                active ? 'bg-emerald-500/10 text-emerald-300 border border-emerald-500/20' : 'text-white/45 hover:text-white/80 hover:bg-white/4',
                disabled && 'opacity-40 cursor-not-allowed hover:bg-transparent hover:text-white/45 border border-transparent'
              )}
            >
              <item.icon className={cn('w-4 h-4 flex-shrink-0', active ? 'text-emerald-400' : '')} />
              <span className="text-sm font-medium whitespace-nowrap">{item.label}</span>
            </Link>
          )
        })}
      </nav>

      <div className={cn('border-t border-white/5 p-3', variant === 'mobile' && 'bg-[#080812]')}>
        <button
          onClick={() => logout()}
          className="w-full flex items-center justify-center gap-2 py-2 rounded-xl text-white/40 hover:text-red-400 hover:bg-red-500/10 transition-all"
        >
          <LogOut className="w-4 h-4" />
          Sign out
        </button>
      </div>
    </aside>
  )
}

