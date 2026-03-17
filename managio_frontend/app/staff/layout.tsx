'use client'

import { useCallback, useEffect, useState } from 'react'
import { useAuthStore } from '@/lib/store/authStore'
import { StaffSidebar } from '@/components/layout/StaffSidebar'
import { Header } from '@/components/layout/Header'
import { BottomTabs, type BottomTabItem } from '@/components/layout/BottomTabs'
import { MobileDrawer } from '@/components/layout/MobileDrawer'
import { BarChart3, CreditCard, LayoutDashboard, Menu, Users } from 'lucide-react'
import { BOTTOM_TABS_H, HEADER_H, STAFF_SIDEBAR_W } from '@/lib/ui/layout'
import { usePathname } from 'next/navigation'

export default function StaffLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const businessId = useAuthStore((s) => (s.staffContext?.businessId ? String(s.staffContext.businessId) : ''))
  const canManageMembers = useAuthStore((s) => s.staffContext?.canManageMembers ?? false)
  const canManagePayments = useAuthStore((s) => s.staffContext?.canManagePayments ?? false)
  const canManageSubscriptions = useAuthStore((s) => s.staffContext?.canManageSubscriptions ?? false)

  const [drawerOpen, setDrawerOpen] = useState(false)
  const [isMobile, setIsMobile] = useState(false)

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

  const openMenu = useCallback(() => setDrawerOpen(true), [])

  const tabs: BottomTabItem[] = [
    { key: 'dash', label: 'Home', href: '/staff/dashboard', icon: LayoutDashboard },
    ...(canManageMembers ? [{ key: 'members', label: 'Members', href: '/staff/members', icon: Users }] : [{ key: 'members', label: 'Members', icon: Users, onClick: openMenu }]),
    ...(canManagePayments ? [{ key: 'payments', label: 'Payments', href: '/staff/payments', icon: BarChart3 }] : [{ key: 'payments', label: 'Payments', icon: BarChart3, onClick: openMenu }]),
    ...(canManageSubscriptions
      ? [{ key: 'subs', label: 'Subs', href: '/staff/subscriptions', icon: CreditCard }]
      : [{ key: 'subs', label: 'Subs', icon: CreditCard, onClick: openMenu }]),
    { key: 'more', label: 'More', icon: Menu, onClick: openMenu },
  ].slice(0, 5)

  // Public staff routes should not show portal chrome.
  const isPublicStaffRoute = pathname === '/staff/login' || pathname === '/staff/accept-invitation'

  if (isPublicStaffRoute) {
    return <>{children}</>
  }

  return (
    <div className="min-h-screen bg-[#070710]">
      <div className="hidden md:block">
        <StaffSidebar businessId={businessId} variant="desktop" />
      </div>

      <Header sidebarCollapsed={false} isMobile={isMobile} onMobileMenu={() => setDrawerOpen(true)} />

      <MobileDrawer open={isMobile && drawerOpen} title="Staff menu" onClose={() => setDrawerOpen(false)}>
        <StaffSidebar businessId={businessId} variant="mobile" onNavigate={() => setDrawerOpen(false)} />
      </MobileDrawer>

      <main
        className="min-h-screen px-4 md:px-6 lg:px-8"
        style={{
          paddingTop: HEADER_H,
          paddingBottom: isMobile ? BOTTOM_TABS_H : undefined,
          paddingLeft: isMobile ? undefined : STAFF_SIDEBAR_W,
        }}
      >
        <div className="py-4">{children}</div>
      </main>

      {isMobile && <BottomTabs items={tabs} />}
    </div>
  )
}

