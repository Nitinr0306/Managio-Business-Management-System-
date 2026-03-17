'use client'

import { Dumbbell, LogOut, Menu } from 'lucide-react'
import Link from 'next/link'
import { useEffect, useMemo, useState } from 'react'
import { BottomTabs, type BottomTabItem } from '@/components/layout/BottomTabs'
import { MobileDrawer } from '@/components/layout/MobileDrawer'
import { useAuth } from '@/lib/hooks/useAuth'
import { BOTTOM_TABS_H } from '@/lib/ui/layout'

export default function MemberPortalLayout({ children }: { children: React.ReactNode }) {
  const { logout } = useAuth()
  const [isMobile, setIsMobile] = useState(false)
  const [drawerOpen, setDrawerOpen] = useState(false)

  useEffect(() => {
    const check = () => {
      const mobile = window.innerWidth < 768
      setIsMobile(mobile)
      if (mobile) setDrawerOpen(false)
    }
    check()
    window.addEventListener('resize', check)
    return () => window.removeEventListener('resize', check)
  }, [])

  const tabs: BottomTabItem[] = useMemo(
    () => [
      { key: 'dash', label: 'Home', href: '/member/dashboard', icon: Dumbbell },
      { key: 'sub', label: 'Plan', href: '/member/subscription', icon: Dumbbell },
      { key: 'pay', label: 'Payments', href: '/member/payments', icon: Dumbbell },
      { key: 'more', label: 'More', icon: Menu, onClick: () => setDrawerOpen(true) },
    ],
    []
  )

  return (
    <div className="min-h-screen bg-[#070710]">
      <div className="fixed inset-0 pointer-events-none">
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[600px] h-[300px] bg-emerald-600/6 rounded-full blur-[100px]" />
      </div>
      <header className="relative z-20 px-4 md:px-6 py-4 border-b border-white/5 flex items-center justify-between bg-[#070710]/70 backdrop-blur">
        <Link href="/member/dashboard" className="inline-flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-emerald-500 to-teal-600 flex items-center justify-center shadow-lg shadow-emerald-500/25">
            <Dumbbell className="w-4 h-4 text-white" />
          </div>
          <span className="text-lg font-display font-700 text-white tracking-tight">Managio</span>
          <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-500/15 text-emerald-400 font-medium">Member</span>
        </Link>
        <button
          onClick={() => setDrawerOpen(true)}
          className="md:hidden w-9 h-9 flex items-center justify-center rounded-xl border border-white/10 text-white/60 hover:text-white hover:bg-white/5 transition-all"
        >
          <Menu className="w-4 h-4" />
        </button>
      </header>

      <MobileDrawer open={isMobile && drawerOpen} title="Member menu" onClose={() => setDrawerOpen(false)}>
        <div className="p-3 bg-[#080812]">
          <div className="space-y-1">
            <Link onClick={() => setDrawerOpen(false)} href="/member/dashboard" className="block px-3 py-2.5 rounded-xl text-white/70 hover:bg-white/5">
              Dashboard
            </Link>
            <Link onClick={() => setDrawerOpen(false)} href="/member/subscription" className="block px-3 py-2.5 rounded-xl text-white/70 hover:bg-white/5">
              Subscription
            </Link>
            <Link onClick={() => setDrawerOpen(false)} href="/member/payments" className="block px-3 py-2.5 rounded-xl text-white/70 hover:bg-white/5">
              Payments
            </Link>
          </div>

          <div className="mt-3 border-t border-white/10 pt-3">
            <button
              onClick={() => logout()}
              className="w-full flex items-center justify-center gap-2 py-2.5 rounded-xl text-white/60 hover:text-red-300 hover:bg-red-500/10 transition-all"
            >
              <LogOut className="w-4 h-4" />
              Sign out
            </button>
          </div>
        </div>
      </MobileDrawer>

      <main className="relative z-10" style={{ paddingBottom: isMobile ? BOTTOM_TABS_H : undefined }}>
        {children}
      </main>

      {isMobile && <BottomTabs items={tabs} />}
    </div>
  )
}