'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { cn } from '@/lib/utils/cn'

export type BottomTabItem = {
  key: string
  label: string
  href?: string
  icon: React.ElementType
  onClick?: () => void
}

export function BottomTabs({ items }: { items: BottomTabItem[] }) {
  const pathname = usePathname()
  const cols = Math.min(5, Math.max(3, items.length))

  const isActive = (href?: string) => {
    if (!href) return false
    return pathname === href || pathname.startsWith(href + '/')
  }

  return (
    <nav
      className={cn(
        'fixed bottom-0 left-0 right-0 z-30 md:hidden',
        'bg-[hsl(var(--surface-1))]/90 backdrop-blur-xl border-t border-white/[0.06]'
      )}
      aria-label="Primary navigation"
    >
      <div className="mx-auto max-w-screen-xl px-2 pb-[env(safe-area-inset-bottom)]">
        <div className="h-16 grid gap-1" style={{ gridTemplateColumns: `repeat(${cols}, minmax(0, 1fr))` }}>
          {items.slice(0, cols).map((item) => {
            const active = isActive(item.href)
            const Icon = item.icon

            const content = (
              <div
                className={cn(
                  'h-full w-full rounded-xl flex flex-col items-center justify-center gap-1 transition-all duration-200',
                  active
                    ? 'bg-indigo-500/[0.08] text-indigo-200'
                    : 'text-white/45 hover:text-white/70 active:bg-white/[0.04]'
                )}
              >
                <Icon className={cn('w-[18px] h-[18px]', active && 'text-indigo-300')} />
                <div className="text-[10px] font-600 leading-none">{item.label}</div>
              </div>
            )

            if (item.href) {
              return (
                <Link key={item.key} href={item.href} className="h-full">
                  {content}
                </Link>
              )
            }

            return (
              <button key={item.key} onClick={item.onClick} className="h-full w-full">
                {content}
              </button>
            )
          })}
        </div>
      </div>
    </nav>
  )
}
