'use client'
import { motion, AnimatePresence } from 'framer-motion'
import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { useState } from 'react'
import {
  LayoutDashboard, Building2, Users, UserCog, CreditCard,
  BarChart3, ScrollText, ChevronLeft, ChevronRight,
  LogOut, Settings, ChevronDown, Plus, Calendar, Bell, Mail,
} from 'lucide-react'
import { useAuth } from '@/lib/hooks/useAuth'
import { useBusinessStore } from '@/lib/store/businessStore'
import { useMyBusinesses } from '@/lib/hooks/useBusiness'
import { getInitials, getUserDisplayName } from '@/lib/utils/formatters'
import { cn } from '@/lib/utils/cn'

interface NavItem { label: string; href: string; icon: React.ElementType; exact?: boolean }
interface NavGroup { label?: string; items: NavItem[] }

function useNavGroups(businessId?: string): NavGroup[] {
  if (!businessId) {
    return [{
      items: [
        { label: 'Dashboard',     href: '/dashboard',       icon: LayoutDashboard, exact: true },
        { label: 'Team',          href: '/dashboard/team',  icon: UserCog },
        { label: 'My Businesses', href: '/businesses',      icon: Building2,       exact: true },
        { label: 'Notifications', href: '/notifications',   icon: Bell },
        { label: 'Profile',       href: '/profile',         icon: UserCog },
        { label: 'Settings',      href: '/settings',        icon: Settings },
      ],
    }]
  }
  const b = `/businesses/${businessId}`
  return [
    { items: [{ label: 'Overview', href: b, icon: LayoutDashboard, exact: true }] },
    {
      label: 'Management',
      items: [
        { label: 'Members',       href: `${b}/members`,       icon: Users },
        { label: 'Staff',         href: `${b}/staff`,         icon: UserCog },
        { label: 'Invitations',   href: `${b}/staff/invitations`, icon: Mail },
      ],
    },
    {
      label: 'Finance',
      items: [
        { label: 'Subscriptions', href: `${b}/subscriptions`, icon: CreditCard },
        { label: 'Plans',         href: `${b}/subscriptions/plans`, icon: Calendar },
        { label: 'Payments',      href: `${b}/payments`,      icon: BarChart3 },
      ],
    },
    {
      label: 'Analytics',
      items: [
        { label: 'Statistics',    href: `${b}/statistics`,    icon: BarChart3 },
        { label: 'Audit Logs',    href: `${b}/audit-logs`,    icon: ScrollText },
      ],
    },
  ]
}

interface SidebarProps { collapsed: boolean; onToggle: () => void; mobileOpen?: boolean }

export function Sidebar({ collapsed, onToggle, mobileOpen = false }: SidebarProps) {
  const pathname = usePathname()
  const router = useRouter()
  const { user, logout } = useAuth()
  const { currentBusiness, setCurrentBusiness } = useBusinessStore()
  const { data: businesses } = useMyBusinesses()
  const [bizOpen, setBizOpen] = useState(false)

  const navGroups = useNavGroups(currentBusiness?.id?.toString())
  const displayName = getUserDisplayName(user)
  const wide = !collapsed || mobileOpen

  const isActive = (href: string, exact = false) =>
    exact ? pathname === href : pathname === href || pathname.startsWith(href + '/')

  const visible = !collapsed || mobileOpen

  return (
    <>
      {mobileOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-30 md:hidden" onClick={onToggle} />
      )}
      <aside
        className={cn(
          'fixed left-0 top-0 h-screen bg-[hsl(var(--surface-1))] border-r border-white/[0.05] flex flex-col z-40 transition-[width] duration-300 ease-out-expo overflow-hidden',
          wide ? 'w-[260px]' : 'w-[72px]',
          !visible && !mobileOpen && 'hidden md:flex'
        )}
      >
        {/* Logo row */}
        <div className="flex items-center gap-3 px-4 h-16 border-b border-white/[0.05] flex-shrink-0">
          <Link
            href="/dashboard"
            className="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center flex-shrink-0 shadow-lg shadow-indigo-500/20"
          >
            <Building2 className="w-4 h-4 text-white" />
          </Link>
          {wide && (
            <motion.span
              initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
              className="text-base font-display font-700 whitespace-nowrap flex-1 tracking-tight"
            >
              Managio
            </motion.span>
          )}
          <button
            onClick={onToggle}
            className="text-white/25 hover:text-white/60 transition-colors flex-shrink-0 ml-auto w-7 h-7 flex items-center justify-center rounded-lg hover:bg-white/[0.04]"
          >
            {wide ? <ChevronLeft className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
          </button>
        </div>

        {/* Business switcher */}
        {wide && (
          <div className="px-3 py-2.5 border-b border-white/[0.05] flex-shrink-0">
            <button
              onClick={() => setBizOpen(!bizOpen)}
              className="w-full flex items-center gap-2.5 px-3 py-2.5 rounded-xl bg-white/[0.03] hover:bg-white/[0.05] border border-white/[0.04] hover:border-white/[0.08] transition-all"
            >
              <div className="w-7 h-7 rounded-lg bg-indigo-600/20 flex items-center justify-center text-xs font-display font-700 text-indigo-300 flex-shrink-0">
                {currentBusiness ? getInitials(currentBusiness.name) : 'A'}
              </div>
              <div className="flex-1 min-w-0 text-left">
                <div className="text-xs font-medium text-white/75 truncate">
                  {currentBusiness?.name || 'All Businesses'}
                </div>
                <div className="text-[10px] text-white/30 truncate">
                  {currentBusiness?.type?.replace(/_/g, ' ') || 'Select a business'}
                </div>
              </div>
              <ChevronDown className={cn('w-3.5 h-3.5 text-white/30 transition-transform flex-shrink-0', bizOpen && 'rotate-180')} />
            </button>

            <AnimatePresence>
              {bizOpen && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: 'auto', opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  transition={{ duration: 0.2, ease: [0.22, 1, 0.36, 1] }}
                  className="overflow-hidden"
                >
                  <div className="mt-1.5 space-y-0.5 max-h-44 overflow-y-auto scrollbar-thin">
                    <button
                      onClick={() => { setCurrentBusiness(null); setBizOpen(false); router.push('/dashboard') }}
                      className="w-full flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-white/[0.04] transition-all text-left"
                    >
                      <div className="w-6 h-6 rounded-md bg-white/[0.04] flex items-center justify-center text-[10px] text-white/35">A</div>
                      <span className="text-xs text-white/50">All Businesses</span>
                    </button>
                    {(businesses || []).map((biz) => (
                      <button
                        key={biz.id}
                        onClick={() => { setCurrentBusiness(biz); setBizOpen(false); router.push(`/businesses/${biz.id}`) }}
                        className={cn(
                          'w-full flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-white/[0.04] transition-all text-left',
                          currentBusiness?.id === biz.id && 'bg-indigo-500/[0.08] border border-indigo-500/15'
                        )}
                      >
                        <div className="w-6 h-6 rounded-md bg-indigo-600/15 flex items-center justify-center text-[10px] font-700 text-indigo-300">
                          {getInitials(biz.name)}
                        </div>
                        <span className="text-xs text-white/60 truncate">{biz.name}</span>
                      </button>
                    ))}
                    <Link
                      href="/businesses/new"
                      onClick={() => setBizOpen(false)}
                      className="w-full flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-white/[0.04] transition-all text-left"
                    >
                      <div className="w-6 h-6 rounded-md border border-dashed border-white/10 flex items-center justify-center">
                        <Plus className="w-3 h-3 text-white/30" />
                      </div>
                      <span className="text-xs text-white/35">Add business</span>
                    </Link>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        )}

        {/* Nav items */}
        <nav className="flex-1 overflow-y-auto scrollbar-thin py-3 px-2.5">
          {navGroups.map((group, gi) => (
            <div key={gi} className="mb-4">
              {group.label && wide && (
                <div className="px-2 py-1 text-[10px] font-600 text-white/20 uppercase tracking-wider mb-1">
                  {group.label}
                </div>
              )}
              <div className="space-y-0.5">
                {group.items.map((item) => {
                  const active = isActive(item.href, item.exact)
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      title={!wide ? item.label : undefined}
                      className={cn(
                        'relative flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200 group',
                        active
                          ? 'text-indigo-300'
                          : 'text-white/40 hover:text-white/70 hover:bg-white/[0.03]'
                      )}
                    >
                      {active && (
                        <motion.div
                          layoutId="navActive"
                          className="absolute inset-0 rounded-xl bg-indigo-500/[0.08] border border-indigo-500/15"
                          transition={{ duration: 0.2, ease: [0.22, 1, 0.36, 1] }}
                        />
                      )}
                      <item.icon className={cn('w-4 h-4 flex-shrink-0 relative z-10', active ? 'text-indigo-400' : '')} />
                      {wide && (
                        <span className="text-sm font-medium relative z-10 whitespace-nowrap">{item.label}</span>
                      )}
                    </Link>
                  )
                })}
              </div>
            </div>
          ))}
        </nav>

        {/* User footer */}
        <div className="border-t border-white/[0.05] p-3 flex-shrink-0">
          {wide ? (
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-indigo-500/25 to-violet-500/25 border border-indigo-500/15 flex items-center justify-center text-xs font-display font-700 text-indigo-300 flex-shrink-0">
                {getInitials(displayName)}
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-xs font-medium text-white/75 truncate">{displayName}</div>
                <div className="text-[10px] text-white/30 truncate">{user?.email}</div>
              </div>
              <button
                onClick={() => logout()}
                className="text-white/20 hover:text-red-400 transition-colors w-7 h-7 flex items-center justify-center rounded-lg hover:bg-red-500/[0.08]"
                title="Sign out"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <button
              onClick={() => logout()}
              className="w-full flex items-center justify-center py-2 rounded-xl text-white/20 hover:text-red-400 hover:bg-red-500/[0.08] transition-all"
              title="Sign out"
            >
              <LogOut className="w-4 h-4" />
            </button>
          )}
        </div>
      </aside>
    </>
  )
}