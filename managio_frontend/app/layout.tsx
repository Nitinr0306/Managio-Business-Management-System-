import type { Metadata } from 'next'
import { Inter, Plus_Jakarta_Sans } from 'next/font/google'
import './globals.css'
import { Providers } from './providers'

const inter = Inter({
  subsets: ['latin'],
  variable: '--font-inter',
  weight: ['300', '400', '500', '600', '700'],
  display: 'swap',
})

const jakarta = Plus_Jakarta_Sans({
  subsets: ['latin'],
  variable: '--font-jakarta',
  weight: ['500', '600', '700', '800'],
  display: 'swap',
})

export const metadata: Metadata = {
  title: {
    template: '%s | Managio',
    default: 'Managio — Multi-Tenant Business Management Platform',
  },
  description:
    'Manage multiple businesses from one account. Members, staff, subscriptions, payments, and analytics — all with role-based access control.',
  keywords: [
    'business management',
    'multi-tenant',
    'member management',
    'staff management',
    'subscriptions',
    'payments',
    'RBAC',
    'SaaS platform',
  ],
  themeColor: '#0a0b14',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className="dark" suppressHydrationWarning>
      <body className={`${inter.variable} ${jakarta.variable} font-sans`}>
        <Providers>{children}</Providers>
      </body>
    </html>
  )
}