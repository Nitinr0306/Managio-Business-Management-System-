'use client'

import { useState, useEffect, useCallback } from 'react'
import { Sidebar } from '@/components/layout/Sidebar'
import { Header } from '@/components/layout/Header'
import { motion } from 'framer-motion'
import { ErrorBoundary } from '@/components/shared/ErrorBoundary'
import { BottomTabs, type BottomTabItem } from '@/components/layout/BottomTabs'
import { useBusinessStore } from '@/lib/store/businessStore'
import { Bell, Building2, CheckSquare, CreditCard, LayoutDashboard, Menu, Users, UserCog } from 'lucide-react'
import { BOTTOM_TABS_H, HEADER_H, OWNER_SIDEBAR_FULL, OWNER_SIDEBAR_MINI } from '@/lib/ui/layout'

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [isMobile, setIsMobile] = useState(false)
  const currentBusiness = useBusinessStore((s) => s.currentBusiness)

  useEffect(() => {
    const check = () => {
      const mobile = window.innerWidth < 768
      setIsMobile(mobile)
      if (mobile) {
        setCollapsed(true)
        setMobileOpen(false)
      }
    }
    check()
    window.addEventListener('resize', check)
    return () => window.removeEventListener('resize', check)
  }, [])

  const toggleSidebar = useCallback(() => {
    if (isMobile) {
      setMobileOpen((v) => !v)
    } else {
      setCollapsed((v) => !v)
    }
  }, [isMobile])

  const mainMarginLeft = isMobile ? 0 : collapsed ? OWNER_SIDEBAR_MINI : OWNER_SIDEBAR_FULL

  const tabs: BottomTabItem[] = currentBusiness?.id
    ? [
        { key: 'overview', label: 'Home', href: `/businesses/${currentBusiness.id}`, icon: LayoutDashboard },
        { key: 'members', label: 'Members', href: `/businesses/${currentBusiness.id}/members`, icon: Users },
        { key: 'tasks', label: 'Tasks', href: `/businesses/${currentBusiness.id}/tasks`, icon: CheckSquare },
        { key: 'payments', label: 'Payments', href: `/businesses/${currentBusiness.id}/payments`, icon: CreditCard },
        { key: 'more', label: 'More', icon: Menu, onClick: () => setMobileOpen(true) },
      ]
    : [
        { key: 'dashboard', label: 'Home', href: `/dashboard`, icon: LayoutDashboard },
        { key: 'businesses', label: 'Biz', href: `/businesses`, icon: Building2 },
        { key: 'team', label: 'Team', href: `/dashboard/team`, icon: UserCog },
        { key: 'notifs', label: 'Alerts', href: `/notifications`, icon: Bell },
        { key: 'more', label: 'More', icon: Menu, onClick: () => setMobileOpen(true) },
      ]

  return (
    <div className="min-h-screen bg-[hsl(var(--background))]">
      {/* Mobile overlay */}
      {isMobile && mobileOpen && (
        <div
          className="fixed inset-0 bg-black/60 backdrop-blur-sm z-30 md:hidden"
          onClick={() => setMobileOpen(false)}
        />
      )}

      <Sidebar
        collapsed={isMobile ? true : collapsed}
        onToggle={toggleSidebar}
        mobileOpen={!isMobile ? false : mobileOpen}
      />

      <Header
        sidebarCollapsed={collapsed}
        isMobile={isMobile}
        onMobileMenu={() => setMobileOpen(true)}
      />

      <main
        className="transition-[margin-left] duration-300 ease-out-expo min-h-screen"
        style={{
          marginLeft: mainMarginLeft,
          paddingTop: HEADER_H,
          paddingBottom: isMobile ? BOTTOM_TABS_H : undefined,
        }}
      >
        <ErrorBoundary>
          <motion.div
            key="page"
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.35, ease: [0.22, 1, 0.36, 1] }}
            className="p-4 md:p-6 lg:p-8"
          >
            {children}
          </motion.div>
        </ErrorBoundary>
      </main>

      {isMobile && <BottomTabs items={tabs} />}
    </div>
  )
}