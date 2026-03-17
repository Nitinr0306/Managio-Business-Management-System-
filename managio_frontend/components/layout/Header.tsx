'use client'

import { Bell, Search, Moon, Sun, Menu, LogOut, Settings, User as UserIcon } from 'lucide-react'
import { useTheme } from 'next-themes'
import { useAuth } from '@/lib/hooks/useAuth'
import { getInitials, getUserDisplayName } from '@/lib/utils/formatters'
import Link from 'next/link'
import { useState, useRef, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'

interface HeaderProps {
  sidebarCollapsed: boolean
  isMobile?: boolean
  onMobileMenu: () => void
}

export function Header({ sidebarCollapsed, isMobile = false, onMobileMenu }: HeaderProps) {
  const { resolvedTheme, setTheme } = useTheme()
  const { user, logout, userType, staffContext } = useAuth()
  const [profileOpen, setProfileOpen] = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)
  const [mounted, setMounted] = useState(false)
  const profileRef = useRef<HTMLDivElement>(null)

  const displayName = getUserDisplayName(user)

  useEffect(() => {
    setMounted(true)
  }, [])

  // Close profile dropdown on outside click
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (profileRef.current && !profileRef.current.contains(e.target as Node)) {
        setProfileOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const roleLabel =
    userType === 'staff'
      ? staffContext?.staffRole || 'Staff'
      : userType === 'member'
      ? 'Member'
      : 'Owner'

  return (
    <header
      className="fixed top-0 right-0 z-20 h-16 bg-[#080811]/90 backdrop-blur-xl border-b border-white/5 flex items-center px-4 md:px-6 gap-3 transition-[left] duration-300"
      style={{ left: isMobile ? 0 : sidebarCollapsed ? 72 : 260 }}
    >
      {/* Mobile menu toggle */}
      {isMobile && (
        <button
          onClick={onMobileMenu}
          className="w-8 h-8 flex items-center justify-center rounded-lg text-white/50 hover:text-white hover:bg-white/5 transition-all"
        >
          <Menu className="w-4.5 h-4.5" />
        </button>
      )}

      {/* Spacer */}
      <div className="flex-1 min-w-0">
        {searchOpen && (
          <motion.div initial={{ opacity: 0, x: -10 }} animate={{ opacity: 1, x: 0 }} className="max-w-sm">
            <input
              autoFocus
              placeholder="Search members, payments, staff..."
              onBlur={() => setSearchOpen(false)}
              className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2 text-sm text-white placeholder-white/30 focus:outline-none focus:border-indigo-500/60 transition-all"
            />
          </motion.div>
        )}
      </div>

      <div className="flex items-center gap-1">
        {/* Search */}
        <button
          onClick={() => setSearchOpen(!searchOpen)}
          className="w-8 h-8 flex items-center justify-center rounded-lg text-white/40 hover:text-white/80 hover:bg-white/5 transition-all"
        >
          <Search className="w-4 h-4" />
        </button>

        {/* Theme */}
        <button
          onClick={() => setTheme(resolvedTheme === 'dark' ? 'light' : 'dark')}
          className="w-8 h-8 flex items-center justify-center rounded-lg text-white/40 hover:text-white/80 hover:bg-white/5 transition-all"
        >
          {mounted ? (
            resolvedTheme === 'dark' ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />
          ) : (
            <div className="w-4 h-4" />
          )}
        </button>

        {/* Notifications */}
        <Link
          href="/notifications"
          className="w-8 h-8 flex items-center justify-center rounded-lg text-white/40 hover:text-white/80 hover:bg-white/5 transition-all relative"
        >
          <Bell className="w-4 h-4" />
          <span className="absolute top-1.5 right-1.5 w-1.5 h-1.5 rounded-full bg-indigo-500 animate-pulse" />
        </Link>

        {/* Profile dropdown */}
        <div ref={profileRef} className="relative">
          <button
            onClick={() => setProfileOpen(!profileOpen)}
            className="flex items-center gap-2 pl-2 pr-3 h-8 rounded-xl bg-white/4 hover:bg-white/8 border border-white/8 hover:border-white/15 transition-all"
          >
            <div className="w-5 h-5 rounded-full bg-gradient-to-br from-indigo-500/50 to-violet-500/50 border border-indigo-500/30 flex items-center justify-center text-[10px] font-bold text-indigo-300">
              {getInitials(displayName)}
            </div>
            <span className="text-xs font-medium text-white/70 hidden sm:block max-w-[100px] truncate">
              {displayName.split(' ')[0]}
            </span>
          </button>

          <AnimatePresence>
            {profileOpen && (
              <motion.div
                initial={{ opacity: 0, y: 8, scale: 0.95 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: 8, scale: 0.95 }}
                transition={{ duration: 0.15 }}
                className="absolute right-0 top-full mt-2 w-56 bg-[#0f0f1a] border border-white/10 rounded-2xl shadow-2xl overflow-hidden z-50"
              >
                {/* User info */}
                <div className="px-4 py-3 border-b border-white/5">
                  <div className="text-sm font-medium text-white/90 truncate">{displayName}</div>
                  <div className="text-xs text-white/40 truncate">{user?.email}</div>
                  <div className="flex items-center gap-1.5 mt-1.5">
                    <span className="text-[10px] px-1.5 py-0.5 rounded-md bg-indigo-500/15 text-indigo-400 font-medium">
                      {roleLabel}
                    </span>
                    {user?.emailVerified && (
                      <span className="text-[10px] px-1.5 py-0.5 rounded-md bg-emerald-500/15 text-emerald-400">
                        Verified
                      </span>
                    )}
                  </div>
                </div>
                {/* Actions */}
                <div className="p-1.5">
                  {userType === 'owner' && (
                    <>
                      <Link
                        href="/profile"
                        onClick={() => setProfileOpen(false)}
                        className="flex items-center gap-2.5 px-3 py-2 rounded-xl text-sm text-white/70 hover:text-white hover:bg-white/5 transition-all"
                      >
                        <UserIcon className="w-3.5 h-3.5" />
                        Profile
                      </Link>
                      <Link
                        href="/settings"
                        onClick={() => setProfileOpen(false)}
                        className="flex items-center gap-2.5 px-3 py-2 rounded-xl text-sm text-white/70 hover:text-white hover:bg-white/5 transition-all"
                      >
                        <Settings className="w-3.5 h-3.5" />
                        Settings
                      </Link>
                    </>
                  )}
                  <button
                    onClick={() => { setProfileOpen(false); logout() }}
                    className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-sm text-red-400/80 hover:text-red-400 hover:bg-red-500/10 transition-all"
                  >
                    <LogOut className="w-3.5 h-3.5" />
                    Sign Out
                  </button>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </header>
  )
}