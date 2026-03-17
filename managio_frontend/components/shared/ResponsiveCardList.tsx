'use client'

import { cn } from '@/lib/utils/cn'

export function ResponsiveCardList({
  mobile,
  desktop,
  className,
}: {
  mobile: React.ReactNode
  desktop: React.ReactNode
  className?: string
}) {
  return (
    <div className={cn('w-full', className)}>
      <div className="md:hidden">{mobile}</div>
      <div className="hidden md:block">{desktop}</div>
    </div>
  )
}

