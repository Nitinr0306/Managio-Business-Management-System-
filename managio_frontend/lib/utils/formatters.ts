import { format, formatDistanceToNow, parseISO, differenceInDays, isValid } from 'date-fns'
import type { User } from '@/lib/types/auth'

export const formatCurrency = (amount: number | string | null | undefined, currency = 'INR') => {
  const n = Number(amount)
  if (isNaN(n)) return '—'
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(n)
}

export const formatDate = (date: string | Date | null | undefined, fmt = 'dd MMM yyyy') => {
  if (!date) return '—'
  try {
    const d = typeof date === 'string' ? parseISO(date) : date
    if (!isValid(d)) return '—'
    return format(d, fmt)
  } catch {
    return '—'
  }
}

export const formatDateTime = (date: string | Date | null | undefined) => {
  if (!date) return '—'
  try {
    const d = typeof date === 'string' ? parseISO(date) : date
    if (!isValid(d)) return '—'
    return format(d, 'dd MMM yyyy, h:mm a')
  } catch {
    return '—'
  }
}

export const formatRelative = (date: string | Date | null | undefined) => {
  if (!date) return '—'
  try {
    const d = typeof date === 'string' ? parseISO(date) : date
    if (!isValid(d)) return '—'
    return formatDistanceToNow(d, { addSuffix: true })
  } catch {
    return '—'
  }
}

export const getDaysRemaining = (endDate: string | null | undefined): number => {
  if (!endDate) return 0
  try {
    const d = parseISO(endDate)
    if (!isValid(d)) return 0
    return differenceInDays(d, new Date())
  } catch {
    return 0
  }
}

export const formatNumber = (n: number | null | undefined) => {
  if (n == null) return '0'
  return new Intl.NumberFormat('en-IN').format(n)
}

export const getInitials = (name?: string | null): string => {
  if (!name || name.trim() === '') return '?'
  return name
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
}

export const getUserDisplayName = (user: User | null | undefined): string => {
  if (!user) return ''
  return (
    user.fullName?.trim() ||
    `${user.firstName || ''} ${user.lastName || ''}`.trim() ||
    user.email ||
    ''
  )
}

export const truncate = (str: string | null | undefined, length = 30): string => {
  if (!str) return ''
  return str.length > length ? str.slice(0, length) + '…' : str
}

export const formatBytes = (bytes: number): string => {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export const slugify = (text: string) =>
  text
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '')